package com.example.gymworkout.data.social

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SocialRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCol get() = db.collection("users")
    private val friendshipsCol get() = db.collection("friendships")
    private val battlesCol get() = db.collection("streak_battles")
    private val challengesCol get() = db.collection("challenges")
    private val timelineCol get() = db.collection("timeline_events")

    // ═══════════════════════════════════════════
    // USER PROFILE
    // ═══════════════════════════════════════════

    suspend fun createOrUpdateUser(user: SocialUser) {
        usersCol.document(user.uid).set(user.toMap()).await()
    }

    suspend fun getUser(uid: String): SocialUser? {
        val doc = usersCol.document(uid).get().await()
        return doc.toObject(SocialUser::class.java)?.copy(uid = doc.id)
    }

    suspend fun updateUserStreaks(uid: String, streaks: Map<String, Int>, dmgs: Float) {
        usersCol.document(uid).update(
            mapOf("streaks" to streaks, "dmgs" to dmgs, "lastSeen" to Timestamp.now())
        ).await()
    }

    suspend fun updateOnlineStatus(uid: String, isOnline: Boolean) {
        usersCol.document(uid).update(
            mapOf("isOnline" to isOnline, "lastSeen" to Timestamp.now())
        ).await()
    }

    suspend fun findUserByFriendCode(code: String): SocialUser? {
        val snapshot = usersCol.whereEqualTo("friendCode", code).limit(1).get().await()
        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.toObject(SocialUser::class.java)?.copy(uid = doc.id)
        }
    }

    // ═══════════════════════════════════════════
    // FRIENDS
    // ═══════════════════════════════════════════

    suspend fun sendFriendRequest(fromUid: String, toUid: String) {
        val friendship = Friendship(
            user1Id = fromUid,
            user2Id = toUid,
            status = "pending",
            requestedBy = fromUid,
            createdAt = Timestamp.now()
        )
        friendshipsCol.add(friendship.toMap()).await()
    }

    suspend fun acceptFriendRequest(friendshipId: String) {
        friendshipsCol.document(friendshipId).update("status", "accepted").await()
    }

    suspend fun declineFriendRequest(friendshipId: String) {
        friendshipsCol.document(friendshipId).delete().await()
    }

    suspend fun removeFriend(friendshipId: String) {
        friendshipsCol.document(friendshipId).delete().await()
    }

    fun observeFriends(uid: String): Flow<List<FriendInfo>> = callbackFlow {
        // Listen to friendships where user is either user1 or user2
        val listener1 = friendshipsCol.whereEqualTo("user1Id", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // We'll combine both queries in the second listener
            }

        // Use a combined approach: query both sides
        val allFriendships = mutableListOf<Friendship>()

        val reg1 = friendshipsCol.whereEqualTo("user1Id", uid)
            .addSnapshotListener { snap1, _ ->
                if (snap1 == null) return@addSnapshotListener
                val list1 = snap1.documents.mapNotNull { doc ->
                    doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                }

                friendshipsCol.whereEqualTo("user2Id", uid)
                    .get()
                    .addOnSuccessListener { snap2 ->
                        val list2 = snap2.documents.mapNotNull { doc ->
                            doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                        }
                        val combined = list1 + list2
                        // Resolve friend user info
                        val friendInfos = mutableListOf<FriendInfo>()
                        if (combined.isEmpty()) {
                            trySend(emptyList())
                            return@addOnSuccessListener
                        }
                        var resolved = 0
                        for (f in combined) {
                            val friendUid = if (f.user1Id == uid) f.user2Id else f.user1Id
                            usersCol.document(friendUid).get()
                                .addOnSuccessListener { userDoc ->
                                    val friendUser = userDoc.toObject(SocialUser::class.java)
                                        ?.copy(uid = userDoc.id) ?: SocialUser(uid = friendUid)
                                    friendInfos.add(
                                        FriendInfo(
                                            user = friendUser,
                                            friendshipId = f.id,
                                            isPending = f.status == "pending",
                                            isIncoming = f.requestedBy != uid && f.status == "pending"
                                        )
                                    )
                                    resolved++
                                    if (resolved == combined.size) {
                                        trySend(friendInfos.toList())
                                    }
                                }
                                .addOnFailureListener {
                                    resolved++
                                    if (resolved == combined.size) {
                                        trySend(friendInfos.toList())
                                    }
                                }
                        }
                    }
            }

        val reg2 = friendshipsCol.whereEqualTo("user2Id", uid)
            .addSnapshotListener { _, _ ->
                // Trigger refresh via reg1's logic by re-fetching
            }

        awaitClose {
            listener1.remove()
            reg1.remove()
            reg2.remove()
        }
    }

    suspend fun getAcceptedFriendIds(uid: String): List<String> {
        val ids = mutableListOf<String>()
        val snap1 = friendshipsCol.whereEqualTo("user1Id", uid)
            .whereEqualTo("status", "accepted").get().await()
        snap1.documents.forEach { doc ->
            doc.toObject(Friendship::class.java)?.let { ids.add(it.user2Id) }
        }
        val snap2 = friendshipsCol.whereEqualTo("user2Id", uid)
            .whereEqualTo("status", "accepted").get().await()
        snap2.documents.forEach { doc ->
            doc.toObject(Friendship::class.java)?.let { ids.add(it.user1Id) }
        }
        return ids
    }

    suspend fun friendshipExists(uid1: String, uid2: String): Boolean {
        val s1 = friendshipsCol.whereEqualTo("user1Id", uid1)
            .whereEqualTo("user2Id", uid2).get().await()
        if (s1.documents.isNotEmpty()) return true
        val s2 = friendshipsCol.whereEqualTo("user1Id", uid2)
            .whereEqualTo("user2Id", uid1).get().await()
        return s2.documents.isNotEmpty()
    }

    // ═══════════════════════════════════════════
    // STREAK BATTLES
    // ═══════════════════════════════════════════

    suspend fun createStreakBattle(battle: StreakBattle): String {
        val docRef = battlesCol.add(battle.toMap()).await()
        return docRef.id
    }

    suspend fun acceptStreakBattle(battleId: String) {
        battlesCol.document(battleId).update("status", "active").await()
    }

    suspend fun declineStreakBattle(battleId: String) {
        battlesCol.document(battleId).delete().await()
    }

    suspend fun updateBattleStreak(battleId: String, isCreator: Boolean, streak: Int) {
        val field = if (isCreator) "creatorStreak" else "opponentStreak"
        battlesCol.document(battleId).update(field, streak).await()
    }

    suspend fun completeBattle(battleId: String, winnerId: String) {
        battlesCol.document(battleId).update(
            mapOf("status" to "completed", "winnerId" to winnerId)
        ).await()
    }

    fun observeMyBattles(uid: String): Flow<List<StreakBattle>> = callbackFlow {
        val reg1 = battlesCol.whereEqualTo("creatorId", uid)
            .addSnapshotListener { snap1, _ ->
                if (snap1 == null) return@addSnapshotListener
                val list1 = snap1.documents.mapNotNull { doc ->
                    doc.toObject(StreakBattle::class.java)?.copy(id = doc.id)
                }
                battlesCol.whereEqualTo("opponentId", uid).get()
                    .addOnSuccessListener { snap2 ->
                        val list2 = snap2.documents.mapNotNull { doc ->
                            doc.toObject(StreakBattle::class.java)?.copy(id = doc.id)
                        }
                        trySend((list1 + list2).sortedByDescending { it.createdAt })
                    }
            }

        val reg2 = battlesCol.whereEqualTo("opponentId", uid)
            .addSnapshotListener { _, _ -> }

        awaitClose { reg1.remove(); reg2.remove() }
    }

    // ═══════════════════════════════════════════
    // WEEKLY CHALLENGES
    // ═══════════════════════════════════════════

    suspend fun createChallenge(challenge: WeeklyChallenge): String {
        val docRef = challengesCol.add(challenge.toMap()).await()
        return docRef.id
    }

    suspend fun joinChallenge(challengeId: String, participant: ChallengeParticipant) {
        challengesCol.document(challengeId).update(
            "participants", FieldValue.arrayUnion(participant.toMap())
        ).await()
    }

    suspend fun updateChallengeProgress(challengeId: String, userId: String, newProgress: Float) {
        val doc = challengesCol.document(challengeId).get().await()
        val challenge = doc.toObject(WeeklyChallenge::class.java) ?: return
        val updatedParticipants = challenge.participants.map {
            if (it.userId == userId) it.copy(progress = newProgress) else it
        }
        challengesCol.document(challengeId).update(
            "participants", updatedParticipants.map { it.toMap() }
        ).await()
    }

    fun observeActiveChallenges(uid: String): Flow<List<WeeklyChallenge>> = callbackFlow {
        val reg = challengesCol.whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val challenges = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(WeeklyChallenge::class.java)?.copy(id = doc.id)
                }.filter { challenge ->
                    // Show challenges created by user or that user participates in
                    challenge.creatorId == uid ||
                            challenge.participants.any { it.userId == uid }
                }
                trySend(challenges)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getAvailableChallenges(friendIds: List<String>): List<WeeklyChallenge> {
        if (friendIds.isEmpty()) return emptyList()
        // Get active challenges created by friends
        val challenges = mutableListOf<WeeklyChallenge>()
        // Firestore whereIn limited to 30 items
        for (chunk in friendIds.chunked(30)) {
            val snap = challengesCol.whereIn("creatorId", chunk)
                .whereEqualTo("status", "active").get().await()
            snap.documents.mapNotNullTo(challenges) { doc ->
                doc.toObject(WeeklyChallenge::class.java)?.copy(id = doc.id)
            }
        }
        return challenges.sortedByDescending { it.createdAt }
    }

    // ═══════════════════════════════════════════
    // JOURNEY TIMELINE
    // ═══════════════════════════════════════════

    suspend fun postTimelineEvent(event: TimelineEvent) {
        timelineCol.add(event.toMap()).await()
    }

    // ═══════════════════════════════════════════
    // ACCOUNTABILITY PARTNERS
    // ═══════════════════════════════════════════

    private val partnershipsCol get() = db.collection("accountability_partners")

    suspend fun createPartnership(partnership: AccountabilityPartnership): String {
        val docRef = partnershipsCol.add(partnership.toMap()).await()
        return docRef.id
    }

    suspend fun acceptPartnership(partnershipId: String) {
        partnershipsCol.document(partnershipId).update("status", "active").await()
    }

    suspend fun declinePartnership(partnershipId: String) {
        partnershipsCol.document(partnershipId).delete().await()
    }

    suspend fun removePartnership(partnershipId: String) {
        partnershipsCol.document(partnershipId).delete().await()
    }

    fun observePartnerships(uid: String): Flow<List<AccountabilityPartnership>> = callbackFlow {
        val reg1 = partnershipsCol.whereEqualTo("user1Id", uid)
            .addSnapshotListener { snap1, _ ->
                if (snap1 == null) return@addSnapshotListener
                val list1 = snap1.documents.mapNotNull { doc ->
                    doc.toObject(AccountabilityPartnership::class.java)?.copy(id = doc.id)
                }
                partnershipsCol.whereEqualTo("user2Id", uid).get()
                    .addOnSuccessListener { snap2 ->
                        val list2 = snap2.documents.mapNotNull { doc ->
                            doc.toObject(AccountabilityPartnership::class.java)?.copy(id = doc.id)
                        }
                        trySend((list1 + list2).sortedByDescending { it.createdAt })
                    }
            }
        val reg2 = partnershipsCol.whereEqualTo("user2Id", uid)
            .addSnapshotListener { _, _ -> }
        awaitClose { reg1.remove(); reg2.remove() }
    }

    // ═══════════════════════════════════════════
    // TEAM GOALS
    // ═══════════════════════════════════════════

    private val teamGoalsCol get() = db.collection("team_goals")

    suspend fun createTeamGoal(goal: TeamGoal): String {
        val docRef = teamGoalsCol.add(goal.toMap()).await()
        return docRef.id
    }

    suspend fun joinTeamGoal(goalId: String, member: TeamMember) {
        teamGoalsCol.document(goalId).update(
            "members", FieldValue.arrayUnion(member.toMap())
        ).await()
    }

    suspend fun updateTeamGoalProgress(goalId: String, userId: String, contribution: Float, newTotal: Float) {
        val doc = teamGoalsCol.document(goalId).get().await()
        val goal = doc.toObject(TeamGoal::class.java) ?: return
        val updatedMembers = goal.members.map {
            if (it.userId == userId) it.copy(contribution = contribution) else it
        }
        teamGoalsCol.document(goalId).update(
            mapOf("members" to updatedMembers.map { it.toMap() }, "currentTotal" to newTotal)
        ).await()
    }

    fun observeTeamGoals(uid: String): Flow<List<TeamGoal>> = callbackFlow {
        val reg = teamGoalsCol.whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val goals = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TeamGoal::class.java)?.copy(id = doc.id)
                }.filter { goal ->
                    goal.creatorId == uid || goal.members.any { it.userId == uid }
                }
                trySend(goals)
            }
        awaitClose { reg.remove() }
    }

    // ═══════════════════════════════════════════
    // NUTRITION DUELS
    // ═══════════════════════════════════════════

    private val duelsCol get() = db.collection("nutrition_duels")

    suspend fun createDuel(duel: NutritionDuel): String {
        val docRef = duelsCol.add(duel.toMap()).await()
        return docRef.id
    }

    suspend fun acceptDuel(duelId: String) {
        duelsCol.document(duelId).update("status", "active").await()
    }

    suspend fun declineDuel(duelId: String) {
        duelsCol.document(duelId).delete().await()
    }

    suspend fun updateDuelProgress(duelId: String, isChallenger: Boolean, progress: Float) {
        val field = if (isChallenger) "challengerProgress" else "opponentProgress"
        duelsCol.document(duelId).update(field, progress).await()
    }

    suspend fun completeDuel(duelId: String, winnerId: String) {
        duelsCol.document(duelId).update(
            mapOf("status" to "completed", "winnerId" to winnerId)
        ).await()
    }

    fun observeDuels(uid: String): Flow<List<NutritionDuel>> = callbackFlow {
        val reg1 = duelsCol.whereEqualTo("challengerId", uid)
            .addSnapshotListener { snap1, _ ->
                if (snap1 == null) return@addSnapshotListener
                val list1 = snap1.documents.mapNotNull { doc ->
                    doc.toObject(NutritionDuel::class.java)?.copy(id = doc.id)
                }
                duelsCol.whereEqualTo("opponentId", uid).get()
                    .addOnSuccessListener { snap2 ->
                        val list2 = snap2.documents.mapNotNull { doc ->
                            doc.toObject(NutritionDuel::class.java)?.copy(id = doc.id)
                        }
                        trySend((list1 + list2).sortedByDescending { it.createdAt })
                    }
            }
        val reg2 = duelsCol.whereEqualTo("opponentId", uid)
            .addSnapshotListener { _, _ -> }
        awaitClose { reg1.remove(); reg2.remove() }
    }

    // ═══════════════════════════════════════════
    // LEADERBOARDS
    // ═══════════════════════════════════════════

    suspend fun getLeaderboard(fitnessLevel: String? = null, limit: Long = 50): List<LeaderboardEntry> {
        var query = usersCol.whereEqualTo("isPublic", true)
            .orderBy("dmgs", Query.Direction.DESCENDING)
            .limit(limit)
        if (fitnessLevel != null) {
            query = usersCol.whereEqualTo("isPublic", true)
                .whereEqualTo("fitnessLevel", fitnessLevel)
                .orderBy("dmgs", Query.Direction.DESCENDING)
                .limit(limit)
        }
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val user = doc.toObject(SocialUser::class.java)?.copy(uid = doc.id) ?: return@mapNotNull null
            LeaderboardEntry(
                uid = user.uid, displayName = user.displayName,
                photoUrl = user.photoUrl, fitnessLevel = user.fitnessLevel,
                dmgs = user.dmgs, streaks = user.streaks, isPublic = true
            )
        }
    }

    suspend fun setProfilePublic(uid: String, isPublic: Boolean) {
        usersCol.document(uid).update("isPublic", isPublic).await()
    }

    // ═══════════════════════════════════════════
    // WORKOUT TEMPLATES
    // ═══════════════════════════════════════════

    private val templatesCol get() = db.collection("workout_templates")
    private val reviewsCol get() = db.collection("template_reviews")

    suspend fun publishTemplate(template: WorkoutTemplate): String {
        val docRef = templatesCol.add(template.toMap()).await()
        return docRef.id
    }

    suspend fun getTemplates(fitnessLevel: String? = null): List<WorkoutTemplate> {
        var query = templatesCol.orderBy("downloads", Query.Direction.DESCENDING).limit(50)
        if (fitnessLevel != null) {
            query = templatesCol.whereEqualTo("fitnessLevel", fitnessLevel)
                .orderBy("downloads", Query.Direction.DESCENDING).limit(50)
        }
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(WorkoutTemplate::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun getMyTemplates(uid: String): List<WorkoutTemplate> {
        val snapshot = templatesCol.whereEqualTo("creatorId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(WorkoutTemplate::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun incrementTemplateDownloads(templateId: String) {
        templatesCol.document(templateId).update("downloads", FieldValue.increment(1)).await()
    }

    suspend fun addTemplateReview(review: TemplateReview) {
        reviewsCol.add(review.toMap()).await()
        // Update average rating
        val reviews = reviewsCol.whereEqualTo("templateId", review.templateId).get().await()
        val allReviews = reviews.documents.mapNotNull { it.toObject(TemplateReview::class.java) }
        val avgRating = if (allReviews.isNotEmpty()) allReviews.map { it.rating }.average().toFloat() else 0f
        templatesCol.document(review.templateId).update(
            mapOf("rating" to avgRating, "ratingCount" to allReviews.size)
        ).await()
    }

    suspend fun getTemplateReviews(templateId: String): List<TemplateReview> {
        val snapshot = reviewsCol.whereEqualTo("templateId", templateId)
            .orderBy("createdAt", Query.Direction.DESCENDING).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(TemplateReview::class.java)?.copy(id = doc.id)
        }
    }

    // ═══════════════════════════════════════════
    // ACHIEVEMENT BADGES
    // ═══════════════════════════════════════════

    suspend fun awardBadge(uid: String, badge: AchievementBadge) {
        usersCol.document(uid).collection("badges").document(badge.key)
            .set(badge.toMap()).await()
    }

    suspend fun getUserBadges(uid: String): List<AchievementBadge> {
        val snapshot = usersCol.document(uid).collection("badges").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(AchievementBadge::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun hasBadge(uid: String, key: String): Boolean {
        val doc = usersCol.document(uid).collection("badges").document(key).get().await()
        return doc.exists()
    }

    // ═══════════════════════════════════════════
    // JOURNEY TIMELINE
    // ═══════════════════════════════════════════

    fun observeTimeline(friendIds: List<String>, myUid: String): Flow<List<TimelineEvent>> = callbackFlow {
        val allIds = (friendIds + myUid).distinct()
        if (allIds.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // Firestore whereIn limited to 30
        val registrations = allIds.chunked(30).map { chunk ->
            timelineCol.whereIn("userId", chunk)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) return@addSnapshotListener
                    val events = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TimelineEvent::class.java)?.copy(id = doc.id)
                    }
                    trySend(events)
                }
        }

        awaitClose { registrations.forEach { it.remove() } }
    }
}
