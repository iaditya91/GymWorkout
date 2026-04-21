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
    val isPublic: Boolean = false,
    val lastSeen: Timestamp = Timestamp.now(),
    val dailyProgress: DailyProgress = DailyProgress()
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
        "isPublic" to isPublic,
        "lastSeen" to lastSeen,
        "dailyProgress" to dailyProgress.toMap()
    )
}

data class DailyProgress(
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
    val daysOnJourney: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "date" to date,
        "workoutDone" to workoutDone,
        "workoutName" to workoutName,
        "proteinProgress" to proteinProgress,
        "proteinTarget" to proteinTarget,
        "caloriesProgress" to caloriesProgress,
        "caloriesTarget" to caloriesTarget,
        "waterProgress" to waterProgress,
        "waterTarget" to waterTarget,
        "sleepProgress" to sleepProgress,
        "sleepTarget" to sleepTarget,
        "daysOnJourney" to daysOnJourney
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

// ── Accountability Partner ──

data class AccountabilityPartnership(
    val id: String = "",
    val user1Id: String = "",
    val user1Name: String = "",
    val user2Id: String = "",
    val user2Name: String = "",
    val status: String = "pending", // pending, active
    val notifyWorkout: Boolean = true,
    val notifyHabits: Boolean = true,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "user1Id" to user1Id, "user1Name" to user1Name,
        "user2Id" to user2Id, "user2Name" to user2Name,
        "status" to status,
        "notifyWorkout" to notifyWorkout, "notifyHabits" to notifyHabits,
        "createdAt" to createdAt
    )
}

// ── Team Goal ──

data class TeamGoal(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val category: String = "", // WATER, PROTEIN, CALORIES, WORKOUT
    val targetValue: Float = 0f,
    val targetUnit: String = "",
    val currentTotal: Float = 0f,
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "active",
    val members: List<TeamMember> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "creatorId" to creatorId, "creatorName" to creatorName,
        "title" to title, "category" to category,
        "targetValue" to targetValue, "targetUnit" to targetUnit,
        "currentTotal" to currentTotal,
        "startDate" to startDate, "endDate" to endDate,
        "status" to status,
        "members" to members.map { it.toMap() },
        "createdAt" to createdAt
    )
}

data class TeamMember(
    val userId: String = "",
    val displayName: String = "",
    val contribution: Float = 0f,
    val joinedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId, "displayName" to displayName,
        "contribution" to contribution, "joinedAt" to joinedAt
    )
}

// ── Nutrition Duel ──

data class NutritionDuel(
    val id: String = "",
    val challengerId: String = "",
    val challengerName: String = "",
    val opponentId: String = "",
    val opponentName: String = "",
    val category: String = "", // PROTEIN, WATER, CALORIES, SLEEP
    val duration: String = "day", // day, week
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "pending", // pending, active, completed
    val challengerProgress: Float = 0f,
    val opponentProgress: Float = 0f,
    val winnerId: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "challengerId" to challengerId, "challengerName" to challengerName,
        "opponentId" to opponentId, "opponentName" to opponentName,
        "category" to category, "duration" to duration,
        "startDate" to startDate, "endDate" to endDate,
        "status" to status,
        "challengerProgress" to challengerProgress,
        "opponentProgress" to opponentProgress,
        "winnerId" to winnerId, "createdAt" to createdAt
    )
}

// ── Leaderboard Entry ──

data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val fitnessLevel: String = "",
    val dmgs: Float = 0f,
    val streaks: Map<String, Int> = emptyMap(),
    val totalWorkouts: Int = 0,
    val isPublic: Boolean = false
)

// ── Workout Template ──

data class WorkoutTemplate(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val fitnessLevel: String = "", // beginner, intermediate, advanced
    val daysPerWeek: Int = 0,
    val exercises: List<TemplateExercise> = emptyList(),
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val downloads: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "creatorId" to creatorId, "creatorName" to creatorName,
        "title" to title, "description" to description,
        "fitnessLevel" to fitnessLevel, "daysPerWeek" to daysPerWeek,
        "exercises" to exercises.map { it.toMap() },
        "rating" to rating, "ratingCount" to ratingCount,
        "downloads" to downloads, "createdAt" to createdAt
    )
}

data class TemplateExercise(
    val dayOfWeek: Int = 0,
    val name: String = "",
    val sets: Int = 3,
    val reps: String = "10-12",
    val restTimeSeconds: Int = 0,
    val orderIndex: Int = 0,
    val supersetGroupId: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "dayOfWeek" to dayOfWeek, "name" to name,
        "sets" to sets, "reps" to reps,
        "restTimeSeconds" to restTimeSeconds, "orderIndex" to orderIndex,
        "supersetGroupId" to supersetGroupId
    )
}

data class TemplateReview(
    val id: String = "",
    val templateId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0, // 1-5
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "templateId" to templateId, "userId" to userId,
        "userName" to userName, "rating" to rating,
        "comment" to comment, "createdAt" to createdAt
    )
}

// ── Achievement Badge ──

data class AchievementBadge(
    val id: String = "",
    val key: String = "", // unique identifier e.g. "streak_7", "workouts_100"
    val title: String = "",
    val description: String = "",
    val icon: String = "", // emoji
    val earnedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "key" to key, "title" to title,
        "description" to description, "icon" to icon,
        "earnedAt" to earnedAt
    )
}

// ── Friend with details (for UI) ──

data class FriendInfo(
    val user: SocialUser = SocialUser(),
    val friendshipId: String = "",
    val isPending: Boolean = false,
    val isIncoming: Boolean = false
)
