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
