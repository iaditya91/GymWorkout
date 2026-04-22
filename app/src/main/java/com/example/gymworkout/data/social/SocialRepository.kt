package com.example.gymworkout.data.social

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SocialRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    private val usersCol get() = db.collection("users")
    private val friendshipsCol get() = db.collection("friendships")
    private val battlesCol get() = db.collection("streak_battles")
    private val challengesCol get() = db.collection("challenges")
    private val timelineCol get() = db.collection("timeline_events")

    // ═══════════════════════════════════════════
    // USER PROFILE
    // ═══════════════════════════════════════════

    suspend fun createOrUpdateUser(user: SocialUser) {
        try {
            usersCol.document(user.uid).set(user.toMap()).await()
        } catch (_: Exception) { /* offline — will sync when back online */ }
    }

    suspend fun getUser(uid: String): SocialUser? {
        val doc = try {
            usersCol.document(uid).get(Source.SERVER).await()
        } catch (_: Exception) {
            try { usersCol.document(uid).get(Source.CACHE).await() } catch (_: Exception) { null }
        }
        return doc?.toObject(SocialUser::class.java)?.copy(uid = doc.id)
    }

    suspend fun updateUserStreaks(uid: String, streaks: Map<String, Int>, dmgs: Float) {
        try {
            usersCol.document(uid).update(
                mapOf("streaks" to streaks, "dmgs" to dmgs, "lastSeen" to Timestamp.now())
            ).await()
        } catch (_: Exception) { }
    }

    suspend fun updateDailyProgress(uid: String, progress: DailyProgress) {
        try {
            usersCol.document(uid).update(
                mapOf("dailyProgress" to progress.toMap(), "lastSeen" to Timestamp.now())
            ).await()
        } catch (_: Exception) { }
    }

    suspend fun updateOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            usersCol.document(uid).update(
                mapOf("isOnline" to isOnline, "lastSeen" to Timestamp.now())
            ).await()
        } catch (_: Exception) { /* offline — skip */ }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        try {
            usersCol.document(uid).update("fcmToken", token).await()
        } catch (_: Exception) { /* offline — skip */ }
    }

    suspend fun findUserByFriendCode(code: String): SocialUser? {
        return try {
            val snapshot = usersCol.whereEqualTo("friendCode", code).limit(1).get().await()
            snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(SocialUser::class.java)?.copy(uid = doc.id)
            }
        } catch (_: Exception) { null }
    }

    // ═══════════════════════════════════════════
    // FRIENDS
    // ═══════════════════════════════════════════

    suspend fun sendFriendRequest(fromUid: String, toUid: String) {
        try {
            val friendship = Friendship(
                user1Id = fromUid,
                user2Id = toUid,
                status = "pending",
                requestedBy = fromUid,
                createdAt = Timestamp.now()
            )
            friendshipsCol.add(friendship.toMap()).await()
        } catch (_: Exception) { }
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

    suspend fun getFriendsList(uid: String): List<FriendInfo> {
        val friendInfos = mutableListOf<FriendInfo>()
        val snap1 = friendshipsCol.whereEqualTo("user1Id", uid).get().await()
        val snap2 = friendshipsCol.whereEqualTo("user2Id", uid).get().await()
        val combined = (snap1.documents + snap2.documents).mapNotNull { doc ->
            doc.toObject(Friendship::class.java)?.copy(id = doc.id)
        }
        for (f in combined) {
            val friendUid = if (f.user1Id == uid) f.user2Id else f.user1Id
            val userDoc = usersCol.document(friendUid).get().await()
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
        }
        return friendInfos
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
        return try {
            val ids = mutableListOf<String>()
            val snap1 = friendshipsCol.whereEqualTo("user1Id", uid).get().await()
            snap1.documents.forEach { doc ->
                doc.toObject(Friendship::class.java)?.let {
                    if (it.status == "accepted") ids.add(it.user2Id)
                }
            }
            val snap2 = friendshipsCol.whereEqualTo("user2Id", uid).get().await()
            snap2.documents.forEach { doc ->
                doc.toObject(Friendship::class.java)?.let {
                    if (it.status == "accepted") ids.add(it.user1Id)
                }
            }
            ids
        } catch (_: Exception) { emptyList() }
    }

    suspend fun friendshipExists(uid1: String, uid2: String): Boolean {
        return try {
            val s1 = friendshipsCol.whereEqualTo("user1Id", uid1)
                .whereEqualTo("user2Id", uid2).get().await()
            if (s1.documents.isNotEmpty()) return true
            val s2 = friendshipsCol.whereEqualTo("user1Id", uid2)
                .whereEqualTo("user2Id", uid1).get().await()
            s2.documents.isNotEmpty()
        } catch (_: Exception) { false }
    }

    // ═══════════════════════════════════════════
    // STREAK BATTLES
    // ═══════════════════════════════════════════

    suspend fun createStreakBattle(battle: StreakBattle): String {
        return try {
            val docRef = battlesCol.add(battle.toMap()).await()
            docRef.id
        } catch (_: Exception) { "" }
    }

    suspend fun acceptStreakBattle(battleId: String) {
        battlesCol.document(battleId).update("status", "active").await()
    }

    suspend fun declineStreakBattle(battleId: String) {
        battlesCol.document(battleId).delete().await()
    }

    suspend fun getBattlesList(uid: String): List<StreakBattle> {
        val snap1 = battlesCol.whereEqualTo("creatorId", uid).get().await()
        val snap2 = battlesCol.whereEqualTo("opponentId", uid).get().await()
        val list1 = snap1.documents.mapNotNull { doc ->
            doc.toObject(StreakBattle::class.java)?.copy(id = doc.id)
        }
        val list2 = snap2.documents.mapNotNull { doc ->
            doc.toObject(StreakBattle::class.java)?.copy(id = doc.id)
        }
        return (list1 + list2).sortedByDescending { it.createdAt }
    }

    suspend fun updateBattleStreak(battleId: String, isCreator: Boolean, streak: Int) {
        try {
            val field = if (isCreator) "creatorStreak" else "opponentStreak"
            battlesCol.document(battleId).update(field, streak).await()
        } catch (_: Exception) { }
    }

    suspend fun completeBattle(battleId: String, winnerId: String) {
        try {
            battlesCol.document(battleId).update(
                mapOf("status" to "completed", "winnerId" to winnerId)
            ).await()
        } catch (_: Exception) { }
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
        return try {
            val docRef = challengesCol.add(challenge.toMap()).await()
            docRef.id
        } catch (_: Exception) { "" }
    }

    suspend fun joinChallenge(challengeId: String, participant: ChallengeParticipant) {
        challengesCol.document(challengeId).update(
            "participants", FieldValue.arrayUnion(participant.toMap())
        ).await()
    }

    suspend fun updateChallengeProgress(challengeId: String, userId: String, newProgress: Float) {
        try {
            val doc = challengesCol.document(challengeId).get().await()
            val challenge = doc.toObject(WeeklyChallenge::class.java) ?: return
            val updatedParticipants = challenge.participants.map {
                if (it.userId == userId) it.copy(progress = newProgress) else it
            }
            challengesCol.document(challengeId).update(
                "participants", updatedParticipants.map { it.toMap() }
            ).await()
        } catch (_: Exception) { }
    }

    fun observeActiveChallenges(uid: String): Flow<List<WeeklyChallenge>> = callbackFlow {
        val reg = challengesCol.whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val challenges = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(WeeklyChallenge::class.java)?.copy(id = doc.id)
                }.filter { challenge ->
                    // Show challenges created by user or that user participates in
                    challenge.creatorId == uid ||
                            challenge.participants.any { it.userId == uid }
                }.sortedByDescending { it.createdAt }
                trySend(challenges)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getAvailableChallenges(friendIds: List<String>): List<WeeklyChallenge> {
        if (friendIds.isEmpty()) return emptyList()
        return try {
            val challenges = mutableListOf<WeeklyChallenge>()
            for (chunk in friendIds.chunked(30)) {
                val snap = challengesCol.whereIn("creatorId", chunk).get().await()
                snap.documents.mapNotNullTo(challenges) { doc ->
                    doc.toObject(WeeklyChallenge::class.java)?.copy(id = doc.id)
                }
            }
            challenges.filter { it.status == "active" }.sortedByDescending { it.createdAt }
        } catch (_: Exception) { emptyList() }
    }

    // ═══════════════════════════════════════════
    // JOURNEY TIMELINE
    // ═══════════════════════════════════════════

    suspend fun postTimelineEvent(event: TimelineEvent) {
        try { timelineCol.add(event.toMap()).await() } catch (_: Exception) { }
    }

    // ═══════════════════════════════════════════
    // ACCOUNTABILITY PARTNERS
    // ═══════════════════════════════════════════

    private val partnershipsCol get() = db.collection("accountability_partners")

    suspend fun createPartnership(partnership: AccountabilityPartnership): String {
        return try {
            val docRef = partnershipsCol.add(partnership.toMap()).await()
            docRef.id
        } catch (_: Exception) { "" }
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

    suspend fun getPartnershipsList(uid: String): List<AccountabilityPartnership> {
        val snap1 = partnershipsCol.whereEqualTo("user1Id", uid).get().await()
        val snap2 = partnershipsCol.whereEqualTo("user2Id", uid).get().await()
        val list1 = snap1.documents.mapNotNull { doc ->
            doc.toObject(AccountabilityPartnership::class.java)?.copy(id = doc.id)
        }
        val list2 = snap2.documents.mapNotNull { doc ->
            doc.toObject(AccountabilityPartnership::class.java)?.copy(id = doc.id)
        }
        return (list1 + list2).sortedByDescending { it.createdAt }
    }

    suspend fun updatePartnershipNotify(partnershipId: String, field: String, value: Boolean) {
        partnershipsCol.document(partnershipId).update(field, value).await()
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
        return try {
            val docRef = teamGoalsCol.add(goal.toMap()).await()
            docRef.id
        } catch (_: Exception) { "" }
    }

    suspend fun joinTeamGoal(goalId: String, member: TeamMember) {
        teamGoalsCol.document(goalId).update(
            "members", FieldValue.arrayUnion(member.toMap())
        ).await()
    }

    suspend fun getTeamGoalsList(uid: String): List<TeamGoal> {
        val snapshot = teamGoalsCol.whereEqualTo("status", "active").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(TeamGoal::class.java)?.copy(id = doc.id)
        }.filter { goal ->
            goal.creatorId == uid || goal.members.any { it.userId == uid }
        }.sortedByDescending { it.createdAt }
    }

    suspend fun updateTeamGoalProgress(goalId: String, userId: String, contribution: Float, newTotal: Float) {
        try {
            val doc = teamGoalsCol.document(goalId).get().await()
            val goal = doc.toObject(TeamGoal::class.java) ?: return
            val updatedMembers = goal.members.map {
                if (it.userId == userId) it.copy(contribution = contribution) else it
            }
            teamGoalsCol.document(goalId).update(
                mapOf("members" to updatedMembers.map { it.toMap() }, "currentTotal" to newTotal)
            ).await()
        } catch (_: Exception) { }
    }

    fun observeTeamGoals(uid: String): Flow<List<TeamGoal>> = callbackFlow {
        val reg = teamGoalsCol.whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val goals = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TeamGoal::class.java)?.copy(id = doc.id)
                }.filter { goal ->
                    goal.creatorId == uid || goal.members.any { it.userId == uid }
                }.sortedByDescending { it.createdAt }
                trySend(goals)
            }
        awaitClose { reg.remove() }
    }

    // ═══════════════════════════════════════════
    // NUTRITION DUELS
    // ═══════════════════════════════════════════

    private val duelsCol get() = db.collection("nutrition_duels")

    suspend fun createDuel(duel: NutritionDuel): String {
        return try {
            val docRef = duelsCol.add(duel.toMap()).await()
            docRef.id
        } catch (_: Exception) { "" }
    }

    suspend fun acceptDuel(duelId: String) {
        duelsCol.document(duelId).update("status", "active").await()
    }

    suspend fun declineDuel(duelId: String) {
        duelsCol.document(duelId).delete().await()
    }

    suspend fun getDuelsList(uid: String): List<NutritionDuel> {
        val snap1 = duelsCol.whereEqualTo("challengerId", uid).get().await()
        val snap2 = duelsCol.whereEqualTo("opponentId", uid).get().await()
        val list1 = snap1.documents.mapNotNull { doc ->
            doc.toObject(NutritionDuel::class.java)?.copy(id = doc.id)
        }
        val list2 = snap2.documents.mapNotNull { doc ->
            doc.toObject(NutritionDuel::class.java)?.copy(id = doc.id)
        }
        return (list1 + list2).sortedByDescending { it.createdAt }
    }

    suspend fun updateDuelProgress(duelId: String, isChallenger: Boolean, progress: Float) {
        try {
            val field = if (isChallenger) "challengerProgress" else "opponentProgress"
            duelsCol.document(duelId).update(field, progress).await()
        } catch (_: Exception) { }
    }

    suspend fun completeDuel(duelId: String, winnerId: String) {
        try {
            duelsCol.document(duelId).update(
                mapOf("status" to "completed", "winnerId" to winnerId)
            ).await()
        } catch (_: Exception) { }
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
        return try {
            val snapshot = usersCol.whereEqualTo("isPublic", true).get().await()
            snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(SocialUser::class.java)?.copy(uid = doc.id) ?: return@mapNotNull null
                LeaderboardEntry(
                    uid = user.uid, displayName = user.displayName,
                    photoUrl = user.photoUrl, fitnessLevel = user.fitnessLevel,
                    dmgs = user.dmgs, streaks = user.streaks, isPublic = true
                )
            }.let { entries ->
                if (fitnessLevel != null) entries.filter { it.fitnessLevel == fitnessLevel } else entries
            }.sortedByDescending { it.dmgs }.take(limit.toInt())
        } catch (_: Exception) { emptyList() }
    }

    suspend fun setProfilePublic(uid: String, isPublic: Boolean) {
        try { usersCol.document(uid).update("isPublic", isPublic).await() } catch (_: Exception) { }
    }

    // ═══════════════════════════════════════════
    // WORKOUT TEMPLATES
    // ═══════════════════════════════════════════

    private val templatesCol get() = db.collection("workout_templates")
    private val reviewsCol get() = db.collection("template_reviews")

    suspend fun publishTemplate(template: WorkoutTemplate): String {
        return try {
            val docRef = templatesCol.add(template.toMap()).await()
            android.util.Log.d("SocialRepo", "publishTemplate SUCCESS: id=${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            android.util.Log.e("SocialRepo", "publishTemplate FAILED: ${e.message}", e)
            throw e
        }
    }

    suspend fun getTemplates(fitnessLevel: String? = null): List<WorkoutTemplate> {
        return try {
            val snapshot = templatesCol.get().await()
            val result = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkoutTemplate::class.java)?.copy(id = doc.id)
            }.let { templates ->
                if (fitnessLevel != null) templates.filter { it.fitnessLevel == fitnessLevel } else templates
            }.sortedByDescending { it.downloads }.take(50)
            android.util.Log.d("SocialRepo", "getTemplates: found ${result.size}")
            result
        } catch (e: Exception) {
            android.util.Log.e("SocialRepo", "getTemplates FAILED: ${e.message}", e)
            throw e
        }
    }

    suspend fun getMyTemplates(uid: String): List<WorkoutTemplate> {
        return try {
            val snapshot = templatesCol.whereEqualTo("creatorId", uid).get().await()
            val result = snapshot.documents.mapNotNull { doc ->
                doc.toObject(WorkoutTemplate::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.createdAt }
            android.util.Log.d("SocialRepo", "getMyTemplates(uid=$uid): found ${result.size}")
            result
        } catch (e: Exception) {
            android.util.Log.e("SocialRepo", "getMyTemplates FAILED: ${e.message}", e)
            throw e
        }
    }

    suspend fun incrementTemplateDownloads(templateId: String) {
        try { templatesCol.document(templateId).update("downloads", FieldValue.increment(1)).await() } catch (_: Exception) { }
    }

    suspend fun updateTemplate(templateId: String, title: String, description: String, fitnessLevel: String) {
        try {
            templatesCol.document(templateId).update(
                mapOf(
                    "title" to title,
                    "description" to description,
                    "fitnessLevel" to fitnessLevel
                )
            ).await()
            android.util.Log.d("SocialRepo", "updateTemplate SUCCESS: id=$templateId")
        } catch (e: Exception) {
            android.util.Log.e("SocialRepo", "updateTemplate FAILED: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteTemplate(templateId: String) {
        try {
            // Delete associated reviews first
            val reviewSnap = reviewsCol.whereEqualTo("templateId", templateId).get().await()
            for (doc in reviewSnap.documents) {
                try { doc.reference.delete().await() } catch (_: Exception) { }
            }
            templatesCol.document(templateId).delete().await()
            android.util.Log.d("SocialRepo", "deleteTemplate SUCCESS: id=$templateId")
        } catch (e: Exception) {
            android.util.Log.e("SocialRepo", "deleteTemplate FAILED: ${e.message}", e)
            throw e
        }
    }

    suspend fun addTemplateReview(review: TemplateReview) {
        try {
            reviewsCol.add(review.toMap()).await()
            val reviews = reviewsCol.whereEqualTo("templateId", review.templateId).get().await()
            val allReviews = reviews.documents.mapNotNull { it.toObject(TemplateReview::class.java) }
            val avgRating = if (allReviews.isNotEmpty()) allReviews.map { it.rating }.average().toFloat() else 0f
            templatesCol.document(review.templateId).update(
                mapOf("rating" to avgRating, "ratingCount" to allReviews.size)
            ).await()
        } catch (_: Exception) { }
    }

    suspend fun getTemplateReviews(templateId: String): List<TemplateReview> {
        return try {
            val snapshot = reviewsCol.whereEqualTo("templateId", templateId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(TemplateReview::class.java)?.copy(id = doc.id)
            }
        } catch (_: Exception) { emptyList() }
    }

    // ═══════════════════════════════════════════
    // ACHIEVEMENT BADGES
    // ═══════════════════════════════════════════

    suspend fun awardBadge(uid: String, badge: AchievementBadge) {
        try {
            usersCol.document(uid).collection("badges").document(badge.key)
                .set(badge.toMap()).await()
        } catch (_: Exception) { }
    }

    suspend fun getUserBadges(uid: String): List<AchievementBadge> {
        return try {
            val snapshot = usersCol.document(uid).collection("badges").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AchievementBadge::class.java)?.copy(id = doc.id)
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun hasBadge(uid: String, key: String): Boolean {
        return try {
            val doc = usersCol.document(uid).collection("badges").document(key).get().await()
            doc.exists()
        } catch (_: Exception) { false }
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
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val events = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TimelineEvent::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.createdAt }.take(50)
                    trySend(events)
                }
        }

        awaitClose { registrations.forEach { it.remove() } }
    }
}
