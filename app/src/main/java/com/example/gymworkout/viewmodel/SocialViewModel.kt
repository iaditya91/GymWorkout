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
    // Effective user ID: prefer Firebase UID, fall back to Google ID or cached social user
    private val effectiveUserId: String?
        get() = authManager.currentUserId
            ?: _currentSocialUser.value?.uid?.takeIf { it.isNotEmpty() }

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

    // ── Accountability Partners ──
    private val _partnerships = MutableStateFlow<List<AccountabilityPartnership>>(emptyList())
    val partnerships: StateFlow<List<AccountabilityPartnership>> = _partnerships

    // ── Team Goals ──
    private val _teamGoals = MutableStateFlow<List<TeamGoal>>(emptyList())
    val teamGoals: StateFlow<List<TeamGoal>> = _teamGoals

    // ── Nutrition Duels ──
    private val _duels = MutableStateFlow<List<NutritionDuel>>(emptyList())
    val duels: StateFlow<List<NutritionDuel>> = _duels

    // ── Leaderboard ──
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard

    private val _isProfilePublic = MutableStateFlow(false)
    val isProfilePublic: StateFlow<Boolean> = _isProfilePublic

    // ── Workout Templates ──
    private val _templates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val templates: StateFlow<List<WorkoutTemplate>> = _templates

    private val _myTemplates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val myTemplates: StateFlow<List<WorkoutTemplate>> = _myTemplates

    private val _templateReviews = MutableStateFlow<List<TemplateReview>>(emptyList())
    val templateReviews: StateFlow<List<TemplateReview>> = _templateReviews

    // ── Achievement Badges ──
    private val _badges = MutableStateFlow<List<AchievementBadge>>(emptyList())
    val badges: StateFlow<List<AchievementBadge>> = _badges

    // ── Loading / Error ──
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        android.util.Log.d("SocialVM", "init: isSignedIn=${_isSignedIn.value}, firebaseUid=${authManager.currentUserId}, firebaseSignedIn=${authManager.isSignedIn}")
        if (_isSignedIn.value) {
            loadSocialData()
        }
    }

    // ═══════════════════════════════════════════
    // AUTH
    // ═══════════════════════════════════════════

    fun refreshSignInState() {
        val googleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getLastSignedInAccount(getApplication())
        val googleSignedIn = googleAccount != null
        val wasSignedIn = _isSignedIn.value
        _isSignedIn.value = authManager.isSignedIn || googleSignedIn
        if (_isSignedIn.value && (!wasSignedIn || _currentSocialUser.value == null)) {
            if (!authManager.isSignedIn && googleAccount != null) {
                // Google is signed in but Firebase isn't — sign into Firebase properly
                signInWithGoogle(googleAccount)
            } else if (authManager.isSignedIn) {
                loadSocialData()
            }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            android.util.Log.d("SocialVM", "signInWithGoogle: idToken=${account.idToken?.take(20)}..., id=${account.id}")
            val result = authManager.signInWithGoogle(account)
            android.util.Log.d("SocialVM", "signInWithGoogle result: isSuccess=${result.isSuccess}, error=${result.exceptionOrNull()?.message}")
            result.onSuccess { firebaseUser ->
                android.util.Log.d("SocialVM", "signInWithGoogle SUCCESS: firebaseUid=${firebaseUser.uid}")
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
                onComplete(true)
            }.onFailure {
                _error.value = "Sign-in failed: ${it.message}"
                onComplete(false)
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
            _availableChallenges.value = emptyList()
            _timeline.value = emptyList()
            _partnerships.value = emptyList()
            _teamGoals.value = emptyList()
            _duels.value = emptyList()
            _badges.value = emptyList()
            _templates.value = emptyList()
            _myTemplates.value = emptyList()
            _templateReviews.value = emptyList()
            _leaderboard.value = emptyList()
            _shareData.value = null
            _isProfilePublic.value = false
            _error.value = null
        }
    }

    private fun loadSocialData() {
        val uid = effectiveUserId ?: run {
            android.util.Log.e("SocialVM", "loadSocialData: effectiveUserId is NULL, firebaseUid=${authManager.currentUserId}, socialUser=${_currentSocialUser.value?.uid}")
            return
        }
        android.util.Log.d("SocialVM", "loadSocialData: uid=$uid")
        viewModelScope.launch {
            try {
                // Load user profile, create if missing
                withContext(Dispatchers.IO) {
                    var user = repo.getUser(uid)
                    if (user == null) {
                        val firebaseUser = authManager.currentUser
                        val localProfile = userDao.getProfileSync()
                        user = SocialUser(
                            uid = uid,
                            displayName = localProfile?.name ?: firebaseUser?.displayName ?: "",
                            photoUrl = firebaseUser?.photoUrl?.toString() ?: "",
                            fitnessLevel = localProfile?.fitnessLevel ?: "beginner",
                            friendCode = authManager.generateFriendCode(),
                            joinedAt = Timestamp.now()
                        )
                        repo.createOrUpdateUser(user)
                    }
                    _currentSocialUser.value = user
                    _isProfilePublic.value = user.isPublic
                    repo.updateOnlineStatus(uid, true)
                }
                // Sync local streaks to cloud
                syncStreaksToCloud()
            } catch (e: Exception) {
                // Firestore offline or unavailable — continue with local-only mode
                if (_currentSocialUser.value == null) {
                    val localProfile = withContext(Dispatchers.IO) { userDao.getProfileSync() }
                    _currentSocialUser.value = SocialUser(
                        uid = uid,
                        displayName = localProfile?.name ?: "",
                        fitnessLevel = localProfile?.fitnessLevel ?: "beginner"
                    )
                }
                _error.value = "Social features limited — can't reach server"
            }
        }
        // Observe friends
        viewModelScope.launch {
            try { repo.observeFriends(uid).collect { _friends.value = it } } catch (_: Exception) {}
        }
        // Observe battles
        viewModelScope.launch {
            try { repo.observeMyBattles(uid).collect { _battles.value = it } } catch (_: Exception) {}
        }
        // Observe challenges
        viewModelScope.launch {
            try { repo.observeActiveChallenges(uid).collect { _myChallenges.value = it } } catch (_: Exception) {}
        }
        // Observe timeline
        viewModelScope.launch {
            try {
                val friendIds = withContext(Dispatchers.IO) { repo.getAcceptedFriendIds(uid) }
                repo.observeTimeline(friendIds, uid).collect { _timeline.value = it }
            } catch (_: Exception) {}
        }
        // Load available challenges from friends
        viewModelScope.launch {
            try {
                val friendIds = withContext(Dispatchers.IO) { repo.getAcceptedFriendIds(uid) }
                _availableChallenges.value = withContext(Dispatchers.IO) {
                    repo.getAvailableChallenges(friendIds)
                }
            } catch (_: Exception) {}
        }
        // Observe accountability partners
        viewModelScope.launch {
            try { repo.observePartnerships(uid).collect { _partnerships.value = it } } catch (_: Exception) {}
        }
        // Observe team goals
        viewModelScope.launch {
            try { repo.observeTeamGoals(uid).collect { _teamGoals.value = it } } catch (_: Exception) {}
        }
        // Observe nutrition duels
        viewModelScope.launch {
            try { repo.observeDuels(uid).collect { _duels.value = it } } catch (_: Exception) {}
        }
        // Load badges
        viewModelScope.launch {
            try { _badges.value = withContext(Dispatchers.IO) { repo.getUserBadges(uid) } } catch (_: Exception) {}
        }
        // Load user's own templates
        viewModelScope.launch {
            try { _myTemplates.value = withContext(Dispatchers.IO) { repo.getMyTemplates(uid) } } catch (_: Exception) {}
        }
        // Load all templates
        viewModelScope.launch {
            try { _templates.value = withContext(Dispatchers.IO) { repo.getTemplates(null) } } catch (_: Exception) {}
        }
        // Load leaderboard
        viewModelScope.launch {
            try { _leaderboard.value = withContext(Dispatchers.IO) { repo.getLeaderboard() } } catch (_: Exception) {}
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
            } else if (user.uid == effectiveUserId) {
                _friendSearchError.value = "That's your own code!"
            } else {
                _friendSearchResult.value = user
            }
            _isLoading.value = false
        }
    }

    fun sendFriendRequest(toUid: String) {
        val myUid = effectiveUserId ?: return
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
        val myUid = effectiveUserId ?: return
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
        val uid = effectiveUserId ?: return
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
        val myUid = effectiveUserId ?: return
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
        val myUid = effectiveUserId ?: return
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
        val uid = effectiveUserId ?: return
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
        val uid = effectiveUserId ?: return
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
        val uid = effectiveUserId ?: return
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
        viewModelScope.launch {
            try {
                val today = LocalDate.now().format(formatter)
                val dayOfWeek = LocalDate.now().dayOfWeek.value // 1=Mon, 7=Sun

                withContext(Dispatchers.IO) {
                    val profile = userDao.getProfileSync()

                    // Get nutrition progress
                    val proteinActual = nutritionDao.getTotalForDateAndCategorySync(today, "PROTEIN")
                    val proteinTarget = nutritionDao.getTargetSync("PROTEIN")?.targetValue ?: 0f
                    val caloriesActual = nutritionDao.getTotalForDateAndCategorySync(today, "CALORIES")
                    val caloriesTarget = nutritionDao.getTargetSync("CALORIES")?.targetValue ?: 0f
                    val waterActual = nutritionDao.getTotalForDateAndCategorySync(today, "WATER")
                    val waterTarget = nutritionDao.getTargetSync("WATER")?.targetValue ?: 0f
                    val sleepActual = nutritionDao.getTotalForDateAndCategorySync(today, "SLEEP")
                    val sleepTarget = nutritionDao.getTargetSync("SLEEP")?.targetValue ?: 0f

                    // Get workout status
                    val checkIn = checkInDao.getCheckInSync(today)

                    // Get current streaks
                    val sinceDate = LocalDate.now().minusDays(365).format(formatter)
                    val streaks = mutableMapOf<String, Int>()
                    for (cat in listOf("WATER", "PROTEIN", "CALORIES", "SLEEP")) {
                        streaks[cat] = computeStreakForCategory(cat, sinceDate)
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
            } catch (e: Exception) {
                // On error, show empty data rather than staying stuck on loading
                _shareData.value = ProgressShareData(
                    userName = _currentSocialUser.value?.displayName ?: "Athlete",
                    date = LocalDate.now().format(formatter),
                    workoutDone = false,
                    workoutName = "",
                    proteinProgress = 0f,
                    proteinTarget = 0f,
                    caloriesProgress = 0f,
                    caloriesTarget = 0f,
                    waterProgress = 0f,
                    waterTarget = 0f,
                    sleepProgress = 0f,
                    sleepTarget = 0f,
                    currentStreaks = emptyMap(),
                    dmgs = 0f,
                    daysOnJourney = 0
                )
            }
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
    // ACCOUNTABILITY PARTNERS
    // ═══════════════════════════════════════════

    fun createPartnership(partnerId: String, partnerName: String) {
        val myUid = effectiveUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        viewModelScope.launch {
            val partnership = AccountabilityPartnership(
                user1Id = myUid, user1Name = myName,
                user2Id = partnerId, user2Name = partnerName,
                status = "pending", createdAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.createPartnership(partnership) }
        }
    }

    fun acceptPartnership(partnershipId: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.acceptPartnership(partnershipId) } }
    }

    fun declinePartnership(partnershipId: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.declinePartnership(partnershipId) } }
    }

    fun removePartnership(partnershipId: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.removePartnership(partnershipId) } }
    }

    // ═══════════════════════════════════════════
    // TEAM GOALS
    // ═══════════════════════════════════════════

    fun createTeamGoal(title: String, category: String, targetValue: Float, targetUnit: String, durationDays: Int = 7) {
        val myUid = effectiveUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        val startDate = LocalDate.now().format(formatter)
        val endDate = LocalDate.now().plusDays(durationDays.toLong()).format(formatter)
        viewModelScope.launch {
            val goal = TeamGoal(
                creatorId = myUid, creatorName = myName,
                title = title, category = category,
                targetValue = targetValue, targetUnit = targetUnit,
                startDate = startDate, endDate = endDate,
                members = listOf(TeamMember(userId = myUid, displayName = myName, joinedAt = Timestamp.now())),
                createdAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.createTeamGoal(goal) }
        }
    }

    fun joinTeamGoal(goalId: String) {
        val myUid = effectiveUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.joinTeamGoal(goalId, TeamMember(userId = myUid, displayName = myName, joinedAt = Timestamp.now()))
            }
        }
    }

    fun syncTeamGoalProgress() {
        val uid = effectiveUserId ?: return
        viewModelScope.launch {
            for (goal in _teamGoals.value) {
                val isMember = goal.members.any { it.userId == uid }
                if (!isMember) continue
                val myContribution = withContext(Dispatchers.IO) {
                    computeChallengeProgressForGoal(goal)
                }
                val newTotal = goal.members.sumOf { (if (it.userId == uid) myContribution else it.contribution).toDouble() }.toFloat()
                withContext(Dispatchers.IO) {
                    repo.updateTeamGoalProgress(goal.id, uid, myContribution, newTotal)
                }
            }
        }
    }

    private suspend fun computeChallengeProgressForGoal(goal: TeamGoal): Float {
        val start = LocalDate.parse(goal.startDate, formatter)
        val today = LocalDate.now()
        var total = 0f
        when (goal.category) {
            "WORKOUT" -> {
                var d = start
                while (!d.isAfter(today)) {
                    val checkIn = checkInDao.getCheckInSync(d.format(formatter))
                    if (checkIn?.workoutDone == true) total += 1f
                    d = d.plusDays(1)
                }
            }
            else -> {
                var d = start
                while (!d.isAfter(today)) {
                    total += nutritionDao.getTotalForDateAndCategorySync(d.format(formatter), goal.category)
                    d = d.plusDays(1)
                }
            }
        }
        return total
    }

    // ═══════════════════════════════════════════
    // NUTRITION DUELS
    // ═══════════════════════════════════════════

    fun createDuel(opponentId: String, opponentName: String, category: String, duration: String = "day") {
        val myUid = effectiveUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        val startDate = LocalDate.now().format(formatter)
        val durationDays = if (duration == "week") 7L else 1L
        val endDate = LocalDate.now().plusDays(durationDays).format(formatter)
        viewModelScope.launch {
            val duel = NutritionDuel(
                challengerId = myUid, challengerName = myName,
                opponentId = opponentId, opponentName = opponentName,
                category = category, duration = duration,
                startDate = startDate, endDate = endDate,
                status = "pending", createdAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.createDuel(duel) }
        }
    }

    fun acceptDuel(duelId: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.acceptDuel(duelId) } }
    }

    fun declineDuel(duelId: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repo.declineDuel(duelId) } }
    }

    fun syncDuelProgress() {
        val uid = effectiveUserId ?: return
        viewModelScope.launch {
            for (duel in _duels.value.filter { it.status == "active" }) {
                val isChallenger = duel.challengerId == uid
                val start = LocalDate.parse(duel.startDate, formatter)
                val today = LocalDate.now()
                var total = 0f
                var d = start
                while (!d.isAfter(today)) {
                    total += withContext(Dispatchers.IO) {
                        nutritionDao.getTotalForDateAndCategorySync(d.format(formatter), duel.category)
                    }
                    d = d.plusDays(1)
                }
                withContext(Dispatchers.IO) { repo.updateDuelProgress(duel.id, isChallenger, total) }

                val endDate = LocalDate.parse(duel.endDate, formatter)
                if (today.isAfter(endDate) || today.isEqual(endDate)) {
                    val updatedDuel = if (isChallenger) duel.copy(challengerProgress = total) else duel.copy(opponentProgress = total)
                    val winnerId = when {
                        updatedDuel.challengerProgress > updatedDuel.opponentProgress -> updatedDuel.challengerId
                        updatedDuel.opponentProgress > updatedDuel.challengerProgress -> updatedDuel.opponentId
                        else -> "tie"
                    }
                    withContext(Dispatchers.IO) { repo.completeDuel(duel.id, winnerId) }
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // LEADERBOARDS
    // ═══════════════════════════════════════════

    fun loadLeaderboard(fitnessLevel: String? = null) {
        viewModelScope.launch {
            _leaderboard.value = withContext(Dispatchers.IO) { repo.getLeaderboard(fitnessLevel) }
        }
    }

    fun toggleProfilePublic() {
        val uid = effectiveUserId ?: return
        val newValue = !_isProfilePublic.value
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.setProfilePublic(uid, newValue) }
            _isProfilePublic.value = newValue
        }
    }

    // ═══════════════════════════════════════════
    // WORKOUT TEMPLATES
    // ═══════════════════════════════════════════

    fun publishWorkoutTemplate(title: String, description: String, fitnessLevel: String) {
        val myUid = effectiveUserId ?: run {
            android.util.Log.e("SocialVM", "publishWorkoutTemplate: effectiveUserId is null, not signed in")
            return
        }
        val myName = _currentSocialUser.value?.displayName ?: ""
        viewModelScope.launch {
            try {
                val exercises = withContext(Dispatchers.IO) { exerciseDao.getAllSync() }
                android.util.Log.d("SocialVM", "publishWorkoutTemplate: uid=$myUid, exercises=${exercises.size}")
                if (exercises.isEmpty()) {
                    android.util.Log.e("SocialVM", "publishWorkoutTemplate: no exercises to publish")
                    return@launch
                }
                val templateExercises = exercises.map { ex ->
                    TemplateExercise(
                        dayOfWeek = ex.dayOfWeek, name = ex.name,
                        sets = ex.sets, reps = ex.reps,
                        restTimeSeconds = ex.restTimeSeconds, orderIndex = ex.orderIndex
                    )
                }
                val daysUsed = exercises.map { it.dayOfWeek }.distinct().size
                val template = WorkoutTemplate(
                    creatorId = myUid, creatorName = myName,
                    title = title, description = description,
                    fitnessLevel = fitnessLevel, daysPerWeek = daysUsed,
                    exercises = templateExercises, createdAt = Timestamp.now()
                )
                withContext(Dispatchers.IO) { repo.publishTemplate(template) }
                loadTemplates()
                loadMyTemplates()
            } catch (e: Exception) {
                android.util.Log.e("SocialVM", "publishWorkoutTemplate FAILED", e)
            }
        }
    }

    fun loadTemplates(fitnessLevel: String? = null) {
        viewModelScope.launch {
            try {
                _templates.value = withContext(Dispatchers.IO) { repo.getTemplates(fitnessLevel) }
            } catch (e: Exception) {
                android.util.Log.e("SocialVM", "loadTemplates FAILED", e)
            }
        }
    }

    fun loadMyTemplates() {
        val uid = effectiveUserId ?: return
        viewModelScope.launch {
            try {
                _myTemplates.value = withContext(Dispatchers.IO) { repo.getMyTemplates(uid) }
            } catch (e: Exception) {
                android.util.Log.e("SocialVM", "loadMyTemplates FAILED", e)
            }
        }
    }

    fun downloadTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            // Replace local exercises with template exercises
            withContext(Dispatchers.IO) {
                exerciseDao.deleteAll()
                for (ex in template.exercises) {
                    exerciseDao.insert(
                        com.example.gymworkout.data.Exercise(
                            dayOfWeek = ex.dayOfWeek, name = ex.name,
                            sets = ex.sets, reps = ex.reps,
                            restTimeSeconds = ex.restTimeSeconds, orderIndex = ex.orderIndex
                        )
                    )
                }
                repo.incrementTemplateDownloads(template.id)
            }
            loadTemplates()
        }
    }

    fun addTemplateReview(templateId: String, rating: Int, comment: String) {
        val myUid = effectiveUserId ?: return
        val myName = _currentSocialUser.value?.displayName ?: ""
        viewModelScope.launch {
            val review = TemplateReview(
                templateId = templateId, userId = myUid, userName = myName,
                rating = rating, comment = comment, createdAt = Timestamp.now()
            )
            withContext(Dispatchers.IO) { repo.addTemplateReview(review) }
            loadReviewsForTemplate(templateId)
        }
    }

    fun loadReviewsForTemplate(templateId: String) {
        viewModelScope.launch {
            _templateReviews.value = withContext(Dispatchers.IO) { repo.getTemplateReviews(templateId) }
        }
    }

    // ═══════════════════════════════════════════
    // ACHIEVEMENT BADGES
    // ═══════════════════════════════════════════

    fun checkAndAwardBadges() {
        val uid = effectiveUserId ?: return
        val user = _currentSocialUser.value ?: return
        viewModelScope.launch {
            val badgeDefs = listOf(
                Triple("streak_7", "First 7-Day Streak", "Maintained a 7-day streak in any category") to "🔥",
                Triple("streak_30", "30-Day Warrior", "Maintained a 30-day streak") to "💪",
                Triple("streak_100", "100-Day Legend", "Maintained a 100-day streak") to "🏆",
                Triple("workouts_10", "Getting Started", "Logged 10 workouts") to "🏋️",
                Triple("workouts_50", "Dedicated Athlete", "Logged 50 workouts") to "⭐",
                Triple("workouts_100", "Century Club", "Logged 100 workouts") to "💯",
                Triple("macros_month", "Macro Master", "Tracked every macro for 30 days") to "🥩",
                Triple("journey_30", "One Month In", "30 days on fitness journey") to "📅",
                Triple("journey_90", "Quarter Champion", "90 days on fitness journey") to "🗓️",
                Triple("journey_365", "Year-Round Athlete", "365 days on fitness journey") to "🎉",
                Triple("first_duel_win", "Duelist", "Won your first nutrition duel") to "⚔️",
                Triple("first_battle_win", "Battle Victor", "Won your first streak battle") to "🥇",
                Triple("template_shared", "Sharing is Caring", "Shared a workout template") to "📤",
                Triple("five_friends", "Social Butterfly", "Made 5 friends") to "🦋"
            )

            for ((info, emoji) in badgeDefs) {
                val (key, title, description) = info
                val hasBadge = withContext(Dispatchers.IO) { repo.hasBadge(uid, key) }
                if (hasBadge) continue

                val earned = withContext(Dispatchers.IO) { checkBadgeCondition(key) }
                if (earned) {
                    val badge = AchievementBadge(key = key, title = title, description = description, icon = emoji, earnedAt = Timestamp.now())
                    withContext(Dispatchers.IO) { repo.awardBadge(uid, badge) }
                    postEvent(type = "goal_reached", title = "Badge Earned: $title!", description = description, value = 0f)
                }
            }
            _badges.value = withContext(Dispatchers.IO) { repo.getUserBadges(uid) }
        }
    }

    private suspend fun checkBadgeCondition(key: String): Boolean {
        val categories = listOf("WATER", "PROTEIN", "CALORIES", "SLEEP")
        return when (key) {
            "streak_7" -> categories.any { computeStreakForCategory(it, LocalDate.now().minusDays(365).format(formatter)) >= 7 }
            "streak_30" -> categories.any { computeStreakForCategory(it, LocalDate.now().minusDays(365).format(formatter)) >= 30 }
            "streak_100" -> categories.any { computeStreakForCategory(it, LocalDate.now().minusDays(365).format(formatter)) >= 100 }
            "workouts_10", "workouts_50", "workouts_100" -> {
                val target = key.split("_")[1].toInt()
                val count = countTotalWorkouts()
                count >= target
            }
            "macros_month" -> {
                // Check if protein was tracked every day for last 30 days
                var d = LocalDate.now().minusDays(29)
                var allTracked = true
                while (!d.isAfter(LocalDate.now())) {
                    val total = nutritionDao.getTotalForDateAndCategorySync(d.format(formatter), "PROTEIN")
                    if (total <= 0f) { allTracked = false; break }
                    d = d.plusDays(1)
                }
                allTracked
            }
            "journey_30", "journey_90", "journey_365" -> {
                val target = key.split("_")[1].toInt()
                val profile = userDao.getProfileSync()
                if (profile?.journeyStartDate?.isNotEmpty() == true) {
                    val days = ChronoUnit.DAYS.between(LocalDate.parse(profile.journeyStartDate, formatter), LocalDate.now()).toInt()
                    days >= target
                } else false
            }
            "first_duel_win" -> _duels.value.any { it.status == "completed" && it.winnerId == effectiveUserId }
            "first_battle_win" -> _battles.value.any { it.status == "completed" && it.winnerId == effectiveUserId }
            "template_shared" -> _myTemplates.value.isNotEmpty()
            "five_friends" -> _friends.value.count { !it.isPending } >= 5
            else -> false
        }
    }

    private suspend fun countTotalWorkouts(): Int {
        var count = 0
        var d = LocalDate.now().minusDays(365)
        while (!d.isAfter(LocalDate.now())) {
            val checkIn = checkInDao.getCheckInSync(d.format(formatter))
            if (checkIn?.workoutDone == true) count++
            d = d.plusDays(1)
        }
        return count
    }

    fun loadBadgesForUser(uid: String) {
        viewModelScope.launch {
            _badges.value = withContext(Dispatchers.IO) { repo.getUserBadges(uid) }
        }
    }

    // ═══════════════════════════════════════════
    // SYNC STREAKS TO CLOUD
    // ═══════════════════════════════════════════

    fun syncStreaksToCloud() {
        val uid = effectiveUserId
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

            // Sync to Firestore if Firebase auth is available
            if (uid != null) {
                withContext(Dispatchers.IO) {
                    repo.updateUserStreaks(uid, streaks, dmgs)
                }
            }

            // Always update local social user so UI reflects new dmgs
            _currentSocialUser.value = _currentSocialUser.value?.copy(
                streaks = streaks,
                dmgs = dmgs
            )

            // Cloud-dependent features
            if (uid != null) {
                syncBattleStreaks()
                syncChallengeProgress()
                syncDuelProgress()
                syncTeamGoalProgress()
                checkAndPostMilestones()
                checkAndAwardBadges()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
