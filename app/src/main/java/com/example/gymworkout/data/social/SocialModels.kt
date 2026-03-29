package com.example.gymworkout.data.social

import com.google.firebase.Timestamp

// ── Firestore User Profile (synced to cloud) ──

data class SocialUser(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val fitnessLevel: String = "beginner",
    val friendCode: String = "",
    val joinedAt: Timestamp = Timestamp.now(),
    val streaks: Map<String, Int> = emptyMap(),
    val dmgs: Float = 0f,
    val isOnline: Boolean = false,
    val lastSeen: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "fitnessLevel" to fitnessLevel,
        "friendCode" to friendCode,
        "joinedAt" to joinedAt,
        "streaks" to streaks,
        "dmgs" to dmgs,
        "isOnline" to isOnline,
        "lastSeen" to lastSeen
    )
}

// ── Friendship ──

data class Friendship(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val status: String = "pending", // pending, accepted, declined
    val requestedBy: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "user1Id" to user1Id,
        "user2Id" to user2Id,
        "status" to status,
        "requestedBy" to requestedBy,
        "createdAt" to createdAt
    )
}

// ── Streak Battle ──

data class StreakBattle(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val opponentId: String = "",
    val opponentName: String = "",
    val category: String = "", // WATER, PROTEIN, CALORIES, SLEEP, etc.
    val startDate: String = "", // yyyy-MM-dd
    val endDate: String = "", // yyyy-MM-dd
    val status: String = "pending", // pending, active, completed
    val creatorStreak: Int = 0,
    val opponentStreak: Int = 0,
    val winnerId: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "creatorId" to creatorId,
        "creatorName" to creatorName,
        "opponentId" to opponentId,
        "opponentName" to opponentName,
        "category" to category,
        "startDate" to startDate,
        "endDate" to endDate,
        "status" to status,
        "creatorStreak" to creatorStreak,
        "opponentStreak" to opponentStreak,
        "winnerId" to winnerId,
        "createdAt" to createdAt
    )
}

// ── Weekly Challenge ──

data class WeeklyChallenge(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "", // PROTEIN, CALORIES, WORKOUT, WATER, CUSTOM
    val targetValue: Float = 0f,
    val targetUnit: String = "", // g, kcal, days, L, etc.
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "active", // active, completed
    val participants: List<ChallengeParticipant> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "creatorId" to creatorId,
        "creatorName" to creatorName,
        "title" to title,
        "description" to description,
        "category" to category,
        "targetValue" to targetValue,
        "targetUnit" to targetUnit,
        "startDate" to startDate,
        "endDate" to endDate,
        "status" to status,
        "participants" to participants.map { it.toMap() },
        "createdAt" to createdAt
    )
}

data class ChallengeParticipant(
    val userId: String = "",
    val displayName: String = "",
    val progress: Float = 0f,
    val joinedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "progress" to progress,
        "joinedAt" to joinedAt
    )
}

// ── Timeline Event ──

data class TimelineEvent(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val type: String = "", // streak_milestone, challenge_won, workout_complete, goal_reached, battle_won
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val value: Float = 0f,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "userName" to userName,
        "userPhotoUrl" to userPhotoUrl,
        "type" to type,
        "title" to title,
        "description" to description,
        "category" to category,
        "value" to value,
        "createdAt" to createdAt
    )
}

// ── Progress Share Card Data ──

data class ProgressShareData(
    val userName: String = "",
    val date: String = "",
    val workoutDone: Boolean = false,
    val workoutName: String = "",
    val proteinProgress: Float = 0f,
    val proteinTarget: Float = 0f,
    val caloriesProgress: Float = 0f,
    val caloriesTarget: Float = 0f,
    val waterProgress: Float = 0f,
    val waterTarget: Float = 0f,
    val sleepProgress: Float = 0f,
    val sleepTarget: Float = 0f,
    val currentStreaks: Map<String, Int> = emptyMap(),
    val dmgs: Float = 0f,
    val daysOnJourney: Int = 0
)

// ── Friend with details (for UI) ──

data class FriendInfo(
    val user: SocialUser = SocialUser(),
    val friendshipId: String = "",
    val isPending: Boolean = false,
    val isIncoming: Boolean = false
)
