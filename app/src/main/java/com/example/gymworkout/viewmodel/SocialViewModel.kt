package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.WorkoutDatabase
import com.example.gymworkout.data.social.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = FirebaseAuthManager()
    private val repo = SocialRepository()
    private val nutritionDao = WorkoutDatabase.getDatabase(application).nutritionDao()
    private val checkInDao = WorkoutDatabase.getDatabase(application).dailyCheckInDao()
    private val exerciseDao = WorkoutDatabase.getDatabase(application).exerciseDao()
    private val userDao = WorkoutDatabase.getDatabase(application).userDao()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── Auth State ──
    // Check both Firebase Auth and Google Sign-In
    private val _isSignedIn = MutableStateFlow(
        authManager.isSignedIn ||
        com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(application) != null
    )
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _currentSocialUser = MutableStateFlow<SocialUser?>(null)
    val currentSocialUser: StateFlow<SocialUser?> = _currentSocialUser

    // ── Friends ──
    private val _friends = MutableStateFlow<List<FriendInfo>>(emptyList())
    val friends: StateFlow<List<FriendInfo>> = _friends

    private val _friendSearchResult = MutableStateFlow<SocialUser?>(null)
    val friendSearchResult: StateFlow<SocialUser?> = _friendSearchResult

    private val _friendSearchError = MutableStateFlow<String?>(null)
    val friendSearchError: StateFlow<String?> = _friendSearchError

    // ── Streak Battles ──
    private val _battles = MutableStateFlow<List<StreakBattle>>(emptyList())
    val battles: StateFlow<List<StreakBattle>> = _battles

    // ── Weekly Challenges ──
    private val _myChallenges = MutableStateFlow<List<WeeklyChallenge>>(emptyList())
    val myChallenges: StateFlow<List<WeeklyChallenge>> = _myChallenges

    private val _availableChallenges = MutableStateFlow<List<WeeklyChallenge>>(emptyList())
    val availableChallenges: StateFlow<List<WeeklyChallenge>> = _availableChallenges

    // ── Timeline ──
    private val _timeline = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val timeline: StateFlow<List<TimelineEvent>> = _timeline

    // ── Progress Share ──
    private val _shareData = MutableStateFlow<ProgressShareData?>(null)
    val shareData: StateFlow<ProgressShareData?> = _shareData

    // ── Loading / Error ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        if (_isSignedIn.value) {
            loadSocialData()
        }
    }

    // ═══════════════════════════════════════════
    // AUTH
    // ═══════════════════════════════════════════

    fun refreshSignInState() {
        val googleSignedIn = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getLastSignedInAccount(getApplication()) != null
        val wasSignedIn = _isSignedIn.value
        _isSignedIn.value = authManager.isSignedIn || googleSignedIn
        if (_isSignedIn.value && !wasSignedIn) {
            // Build a basic social user from Google account if Firebase isn't available
            if (!authManager.isSignedIn && googleSignedIn) {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(getApplication())
                account?.let {
                    _currentSocialUser.value = SocialUser(
                        uid = it.id ?: "",
                        displayName = it.displayName ?: "",
                        photoUrl = it.photoUrl?.toString() ?: "",
                        friendCode = (it.id ?: "").takeLast(8).uppercase()
                    )
                }
            }
            loadSocialData()
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authManager.signInWithGoogle(account)
            result.onSuccess { firebaseUser ->
                _isSignedIn.value = true
                // Create or update social profile
                val localProfile = withContext(Dispatchers.IO) {
                    userDao.getProfileSync()
                }
                val socialUser = SocialUser(
                    uid = firebaseUser.uid,
                    displayName = localProfile?.name ?: firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    fitnessLevel = localProfile?.fitnessLevel ?: "beginner",
                    friendCode = authManager.generateFriendCode(),
                    joinedAt = Timestamp.now()
                )
                withContext(Dispatchers.IO) {
                    repo.createOrUpdateUser(socialUser)
                }
                _currentSocialUser.value = socialUser
                loadSocialData()
            }.onFailure {
                _error.value = "Sign-in failed: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.currentUserId?.let { uid ->
                withContext(Dispatchers.IO) {
                    repo.updateOnlineStatus(uid, false)
                }
            }
            authManager.signOut()
            _isSignedIn.value = false
            _currentSocialUser.value = null
            _friends.value = emptyList()
            _battles.value = emptyList()
            _myChallenges.value = emptyList()
            _timeline.value = emptyList()
        }
    }

    private fun loadSocialData() {
        val uid = authManager.currentUserId ?: return
        viewModelScope.launch {
            // Load user profile
            withContext(Dispatchers.IO) {
                _currentSocialUser.value = repo.getUser(uid)
                repo.updateOnlineStatus(uid, true)
            }
            // Sync local streaks to cloud
            syncStreaksToCloud()
        }
        // Observe friends
        viewModelScope.launch {
            repo.observeFriends(uid).collect { _friends.value = it }
        }
        // Observe battles
        viewModelScope.launch {
            repo.observeMyBattles(uid).collect { _battles.value = it }
        }
        // Observe challenges
        viewModelScope.launch {
            repo.observeActiveChallenges(uid).collect { _myChallenges.value = it }
        }
        // Observe timeline
        viewModelScope.launch {
            val friendIds = withContext(Dispatchers.IO) { repo.getAcceptedFriendIds(uid) }
            repo.observeTimeline(friendIds, uid).collect { _timeline.value = it }
        }
        // Load available challenges from friends
        viewModelScope.launch {
            val friendIds = withContext(Dispatchers.IO) { repo.getAcceptedFriendIds(uid) }
            _availableChallenges.value = withContext(Dispatchers.IO) {
                repo.getAvailableChallenges(friendIds)
            }
        }
    }

    // ═══════════════════════════════════════════
    // FRIENDS
    // ═══════════════════════════════════════════

    fun searchFriendByCode(code: String) {
        viewModelScope.launch {
            _friendSearchError.value = null
            _friendSearchResult.value = null
            val trimmed = code.trim().uppercase()
            if (trimmed.isEmpty()) {
                _friendSearchError.value = "Enter a friend code"
                return@launch
            }
            _isLoading.value = true
            val user = withContext(Dispatchers.IO) { repo.findUserByFriendCode(trimmed) }
            if (user == null) {
                _friendSearchError.value = "No user found with code: $trimmed"
            } else if (user.uid == authManager.currentUserId) {
                _friendSearchError.value = "That's your own code!"
            } else {
                _friendSearchResult.value = user
            }
            _isLoading.value = false
        }
    }

    fun sendFriendRequest(toUid: String) {
        val myUid = authManager.currentUserId ?: return
        viewModelScope.launch {
            val exists = withContext(Dispatchers.IO) { repo.friendshipExists(myUid, toUid) }
            if (exists) {
                _error.value = "Friend request already exists"
                return@launch
            }
            withContext(Dispatchers.IO) { repo.sendFriendRequest(myUid, toUid) }
            _friendSearchResult.value = null
        }
    }

    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.acceptFriendRequest(friendshipId) }
        }
    }

    fun declineFriendRequest(friendshipId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.declineFriendRequest(friendshipId) }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.removeFriend(friendshipId) }
        }
    }

    fun clearFriendSearch() {
        _friendSearchResult.value = null
        _friendSearchError.value = null
    }

    // ═══════════════════════════════════════════
    // STREAK BATTLES
    // ═══════════════════════════════════════════

    fun createStreakBattle(opponentId: String, opponentName: String, category: String, durationDays: Int = 7) {
        val myUid = authManager.currentUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        val startDate = LocalDate.now().format(formatter)
        val endDate = LocalDate.now().plusDays(durationDays.toLong()).format(formatter)

        viewModelScope.launch {
            val battle = StreakBattle(
                creatorId = myUid,
                creatorName = myName,
                opponentId = opponentId,
                opponentName = opponentName,
                category = category,
                startDate = startDate,
                endDate = endDate,
                status = "pending",
                createdAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.createStreakBattle(battle) }
        }
    }

    fun acceptBattle(battleId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.acceptStreakBattle(battleId) }
        }
    }

    fun declineBattle(battleId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.declineStreakBattle(battleId) }
        }
    }

    fun syncBattleStreaks() {
        val uid = authManager.currentUserId ?: return
        viewModelScope.launch {
            val activeBattles = _battles.value.filter { it.status == "active" }
            for (battle in activeBattles) {
                val isCreator = battle.creatorId == uid
                val streak = withContext(Dispatchers.IO) {
                    computeStreakForCategory(battle.category, battle.startDate)
                }
                withContext(Dispatchers.IO) {
                    repo.updateBattleStreak(battle.id, isCreator, streak)
                }
                // Check if battle is over
                val today = LocalDate.now()
                val endDate = LocalDate.parse(battle.endDate, formatter)
                if (today.isAfter(endDate) || today.isEqual(endDate)) {
                    val updatedBattle = if (isCreator) {
                        battle.copy(creatorStreak = streak)
                    } else {
                        battle.copy(opponentStreak = streak)
                    }
                    val winnerId = when {
                        updatedBattle.creatorStreak > updatedBattle.opponentStreak -> updatedBattle.creatorId
                        updatedBattle.opponentStreak > updatedBattle.creatorStreak -> updatedBattle.opponentId
                        else -> "tie"
                    }
                    withContext(Dispatchers.IO) {
                        repo.completeBattle(battle.id, winnerId)
                    }
                    // Post timeline event for winner
                    if (winnerId == uid) {
                        postEvent(
                            type = "battle_won",
                            title = "Won a Streak Battle!",
                            description = "Beat ${if (isCreator) battle.opponentName else battle.creatorName} in ${battle.category} streak",
                            category = battle.category,
                            value = streak.toFloat()
                        )
                    }
                }
            }
        }
    }

    private suspend fun computeStreakForCategory(category: String, sinceDate: String): Int {
        val start = LocalDate.parse(sinceDate, formatter)
        val today = LocalDate.now()
        var streak = 0
        var d = today
        while (!d.isBefore(start)) {
            val dateStr = d.format(formatter)
            val total = nutritionDao.getTotalForDateAndCategorySync(dateStr, category)
            val target = nutritionDao.getTargetSync(category)?.targetValue ?: 0f
            if (target > 0f && total >= target) {
                streak++
            } else {
                break
            }
            d = d.minusDays(1)
        }
        return streak
    }

    // ═══════════════════════════════════════════
    // WEEKLY CHALLENGES
    // ═══════════════════════════════════════════

    fun createChallenge(
        title: String,
        description: String,
        category: String,
        targetValue: Float,
        targetUnit: String,
        durationDays: Int = 7
    ) {
        val myUid = authManager.currentUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        val startDate = LocalDate.now().format(formatter)
        val endDate = LocalDate.now().plusDays(durationDays.toLong()).format(formatter)

        viewModelScope.launch {
            val challenge = WeeklyChallenge(
                creatorId = myUid,
                creatorName = myName,
                title = title,
                description = description,
                category = category,
                targetValue = targetValue,
                targetUnit = targetUnit,
                startDate = startDate,
                endDate = endDate,
                status = "active",
                participants = listOf(
                    ChallengeParticipant(
                        userId = myUid,
                        displayName = myName,
                        progress = 0f,
                        joinedAt = Timestamp.now()
                    )
                ),
                createdAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.createChallenge(challenge) }

            postEvent(
                type = "challenge_created",
                title = "Created a Challenge!",
                description = "$title — $description",
                category = category,
                value = targetValue
            )
        }
    }

    fun joinChallenge(challengeId: String) {
        val myUid = authManager.currentUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        viewModelScope.launch {
            val participant = ChallengeParticipant(
                userId = myUid,
                displayName = myName,
                progress = 0f,
                joinedAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.joinChallenge(challengeId, participant) }
        }
    }

    fun syncChallengeProgress() {
        val uid = authManager.currentUserId ?: return
        viewModelScope.launch {
            for (challenge in _myChallenges.value) {
                val isParticipant = challenge.participants.any { it.userId == uid }
                if (!isParticipant) continue

                val progress = withContext(Dispatchers.IO) {
                    computeChallengeProgress(challenge)
                }
                withContext(Dispatchers.IO) {
                    repo.updateChallengeProgress(challenge.id, uid, progress)
                }

                // Check if challenge goal met
                if (progress >= challenge.targetValue) {
                    postEvent(
                        type = "challenge_won",
                        title = "Challenge Completed!",
                        description = "Reached ${challenge.targetValue}${challenge.targetUnit} in \"${challenge.title}\"",
                        category = challenge.category,
                        value = progress
                    )
                }
            }
        }
    }

    private suspend fun computeChallengeProgress(challenge: WeeklyChallenge): Float {
        val start = LocalDate.parse(challenge.startDate, formatter)
        val today = LocalDate.now()
        var total = 0f

        when (challenge.category) {
            "WORKOUT" -> {
                // Count workout days completed
                var d = start
                while (!d.isAfter(today)) {
                    val dateStr = d.format(formatter)
                    val checkIn = checkInDao.getCheckInSync(dateStr)
                    if (checkIn?.workoutDone == true) total += 1f
                    d = d.plusDays(1)
                }
            }
            else -> {
                // Sum nutrition values across the challenge period
                var d = start
                while (!d.isAfter(today)) {
                    val dateStr = d.format(formatter)
                    total += nutritionDao.getTotalForDateAndCategorySync(dateStr, challenge.category)
                    d = d.plusDays(1)
                }
            }
        }
        return total
    }

    // ═══════════════════════════════════════════
    // JOURNEY TIMELINE
    // ═══════════════════════════════════════════

    private fun postEvent(type: String, title: String, description: String, category: String = "", value: Float = 0f) {
        val uid = authManager.currentUserId ?: return
        val user = _currentSocialUser.value ?: return
        viewModelScope.launch {
            val event = TimelineEvent(
                userId = uid,
                userName = user.displayName,
                userPhotoUrl = user.photoUrl,
                type = type,
                title = title,
                description = description,
                category = category,
                value = value,
                createdAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.postTimelineEvent(event) }
        }
    }

    fun checkAndPostMilestones() {
        val uid = authManager.currentUserId ?: return
        viewModelScope.launch {
            // Check streak milestones for major categories
            val categories = listOf("WATER", "PROTEIN", "CALORIES", "SLEEP")
            for (cat in categories) {
                val streak = withContext(Dispatchers.IO) {
                    computeStreakForCategory(cat, LocalDate.now().minusDays(365).format(formatter))
                }
                // Post milestone events at 7, 14, 30, 60, 100 day marks
                val milestones = listOf(7, 14, 30, 60, 100)
                if (streak in milestones) {
                    postEvent(
                        type = "streak_milestone",
                        title = "$streak-Day ${cat.lowercase().replaceFirstChar { it.uppercase() }} Streak!",
                        description = "Maintained a $streak-day streak for $cat",
                        category = cat,
                        value = streak.toFloat()
                    )
                }
            }

            // Check journey progress milestones
            val profile = withContext(Dispatchers.IO) { userDao.getProfileSync() }
            if (profile != null && profile.journeyStartDate.isNotEmpty()) {
                val startDate = LocalDate.parse(profile.journeyStartDate, formatter)
                val daysOnJourney = ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt()
                val journeyMilestones = listOf(7, 30, 60, 90, 180, 365)
                if (daysOnJourney in journeyMilestones) {
                    postEvent(
                        type = "goal_reached",
                        title = "$daysOnJourney Days on Journey!",
                        description = "Has been on their fitness journey for $daysOnJourney days",
                        value = daysOnJourney.toFloat()
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // PROGRESS SHARING
    // ═══════════════════════════════════════════

    fun generateShareData() {
        val uid = authManager.currentUserId ?: return
        viewModelScope.launch {
            val today = LocalDate.now().format(formatter)
            val profile = withContext(Dispatchers.IO) { userDao.getProfileSync() }
            val dayOfWeek = LocalDate.now().dayOfWeek.value // 1=Mon, 7=Sun

            // Get nutrition progress
            val proteinActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "PROTEIN") }
            val proteinTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("PROTEIN")?.targetValue ?: 0f }
            val caloriesActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "CALORIES") }
            val caloriesTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("CALORIES")?.targetValue ?: 0f }
            val waterActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "WATER") }
            val waterTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("WATER")?.targetValue ?: 0f }
            val sleepActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "SLEEP") }
            val sleepTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("SLEEP")?.targetValue ?: 0f }

            // Get workout status
            val checkIn = withContext(Dispatchers.IO) { checkInDao.getCheckInSync(today) }

            // Get current streaks
            val streaks = mutableMapOf<String, Int>()
            for (cat in listOf("WATER", "PROTEIN", "CALORIES", "SLEEP")) {
                streaks[cat] = withContext(Dispatchers.IO) {
                    computeStreakForCategory(cat, LocalDate.now().minusDays(365).format(formatter))
                }
            }

            // Compute DMGS
            val proteinScore = if (proteinTarget > 0f) min(proteinActual / proteinTarget, 1f) else 0f
            val caloriesScore = if (caloriesTarget > 0f) min(caloriesActual / caloriesTarget, 1f) else 0f
            val workoutScore = if (checkIn?.workoutDone == true) 1f else 0f
            val sleepScore = if (sleepTarget > 0f) min(sleepActual / sleepTarget, 1f) else 0f
            val hydrationScore = if (waterTarget > 0f) min(waterActual / waterTarget, 1f) else 0f
            val dmgs = (proteinScore * 0.35f) + (caloriesScore * 0.20f) +
                    (workoutScore * 0.20f) + (sleepScore * 0.15f) + (hydrationScore * 0.10f)

            // Days on journey
            val daysOnJourney = if (profile?.journeyStartDate?.isNotEmpty() == true) {
                ChronoUnit.DAYS.between(
                    LocalDate.parse(profile.journeyStartDate, formatter),
                    LocalDate.now()
                ).toInt()
            } else 0

            // Get workout name for today
            val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val todayDayName = dayNames[dayOfWeek - 1]

            _shareData.value = ProgressShareData(
                userName = profile?.name ?: _currentSocialUser.value?.displayName ?: "Athlete",
                date = today,
                workoutDone = checkIn?.workoutDone ?: false,
                workoutName = todayDayName,
                proteinProgress = proteinActual,
                proteinTarget = proteinTarget,
                caloriesProgress = caloriesActual,
                caloriesTarget = caloriesTarget,
                waterProgress = waterActual,
                waterTarget = waterTarget,
                sleepProgress = sleepActual,
                sleepTarget = sleepTarget,
                currentStreaks = streaks,
                dmgs = dmgs,
                daysOnJourney = daysOnJourney
            )
        }
    }

    fun generateShareText(): String {
        val data = _shareData.value ?: return ""
        val sb = StringBuilder()
        sb.appendLine("🏋️ ${data.userName}'s Daily Progress")
        sb.appendLine("📅 ${data.date}")
        sb.appendLine()
        sb.appendLine(if (data.workoutDone) "✅ Workout Complete (${data.workoutName})" else "⬜ Workout Pending")
        sb.appendLine("🥩 Protein: ${data.proteinProgress.toInt()}/${data.proteinTarget.toInt()}g ${if (data.proteinProgress >= data.proteinTarget) "✅" else ""}")
        sb.appendLine("🔥 Calories: ${data.caloriesProgress.toInt()}/${data.caloriesTarget.toInt()} ${if (data.caloriesProgress >= data.caloriesTarget) "✅" else ""}")
        sb.appendLine("💧 Water: ${"%.1f".format(data.waterProgress)}/${"%.1f".format(data.waterTarget)}L ${if (data.waterProgress >= data.waterTarget) "✅" else ""}")
        sb.appendLine("😴 Sleep: ${"%.1f".format(data.sleepProgress)}/${"%.1f".format(data.sleepTarget)}h ${if (data.sleepProgress >= data.sleepTarget) "✅" else ""}")
        sb.appendLine()
        sb.appendLine("🔥 Streaks: ${data.currentStreaks.entries.joinToString(" | ") { "${it.key}: ${it.value}d" }}")
        sb.appendLine("📊 DMGS: ${(data.dmgs * 100).toInt()}%")
        if (data.daysOnJourney > 0) {
            sb.appendLine("🗓️ Day ${data.daysOnJourney} of fitness journey")
        }
        sb.appendLine()
        sb.appendLine("— GymWorkout App")
        return sb.toString()
    }

    // ═══════════════════════════════════════════
    // SYNC STREAKS TO CLOUD
    // ═══════════════════════════════════════════

    fun syncStreaksToCloud() {
        val uid = authManager.currentUserId ?: return
        viewModelScope.launch {
            val streaks = mutableMapOf<String, Int>()
            for (cat in listOf("WATER", "PROTEIN", "CALORIES", "SLEEP")) {
                streaks[cat] = withContext(Dispatchers.IO) {
                    computeStreakForCategory(cat, LocalDate.now().minusDays(365).format(formatter))
                }
            }

            // Compute today's DMGS
            val today = LocalDate.now().format(formatter)
            val proteinActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "PROTEIN") }
            val proteinTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("PROTEIN")?.targetValue ?: 150f }
            val caloriesActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "CALORIES") }
            val caloriesTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("CALORIES")?.targetValue ?: 2600f }
            val sleepActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "SLEEP") }
            val sleepTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("SLEEP")?.targetValue ?: 7f }
            val waterActual = withContext(Dispatchers.IO) { nutritionDao.getTotalForDateAndCategorySync(today, "WATER") }
            val waterTarget = withContext(Dispatchers.IO) { nutritionDao.getTargetSync("WATER")?.targetValue ?: 3f }
            val checkIn = withContext(Dispatchers.IO) { checkInDao.getCheckInSync(today) }

            val dmgs = (min(proteinActual / proteinTarget, 1f) * 0.35f) +
                    (min(caloriesActual / caloriesTarget, 1f) * 0.20f) +
                    (if (checkIn?.workoutDone == true) 0.20f else 0f) +
                    (min(sleepActual / sleepTarget, 1f) * 0.15f) +
                    (min(waterActual / waterTarget, 1f) * 0.10f)

            withContext(Dispatchers.IO) {
                repo.updateUserStreaks(uid, streaks, dmgs)
            }

            // Also sync battle streaks and challenge progress
            syncBattleStreaks()
            syncChallengeProgress()
            checkAndPostMilestones()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
