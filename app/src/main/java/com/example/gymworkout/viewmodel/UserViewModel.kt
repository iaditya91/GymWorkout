package com.example.gymworkout.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.ChecklistItem
import com.example.gymworkout.data.MotivationalQuote
import com.example.gymworkout.data.QuotePreference
import com.example.gymworkout.data.UserProfile
import com.example.gymworkout.data.WorkoutDatabase
import com.example.gymworkout.data.WorkoutReminder
import com.example.gymworkout.notification.QuoteReminderScheduler
import com.example.gymworkout.notification.WorkoutReminderScheduler
import com.example.gymworkout.data.sync.BackupData
import com.example.gymworkout.data.sync.BackupManager
import com.example.gymworkout.data.sync.GoogleDriveSync
import com.example.gymworkout.data.sync.SyncPreference
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed class SyncState {
    data object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WorkoutDatabase.getDatabase(application)
    private val dao = db.userDao()
    private val backupManager = BackupManager(db, application)
    private val gson = Gson()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val prefs = application.getSharedPreferences("checklist_reset", Context.MODE_PRIVATE)

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val KEY_LAST_DAILY_RESET = "last_daily_reset"
        private const val KEY_LAST_WEEKLY_RESET = "last_weekly_reset"
    }

    init {
        checkAndAutoReset()
        QuotePreference.init(application)
    }

    private fun checkAndAutoReset() {
        viewModelScope.launch {
            val today = Calendar.getInstance()
            val todayDay = today.get(Calendar.DAY_OF_YEAR)
            val todayYear = today.get(Calendar.YEAR)
            val todayKey = todayYear * 1000 + todayDay

            val lastDailyReset = prefs.getInt(KEY_LAST_DAILY_RESET, 0)
            if (todayKey != lastDailyReset) {
                dao.resetAllChecklistItems()
                prefs.edit().putInt(KEY_LAST_DAILY_RESET, todayKey).apply()
            }

            val currentWeek = today.get(Calendar.WEEK_OF_YEAR)
            val weekKey = todayYear * 100 + currentWeek
            val lastWeeklyReset = prefs.getInt(KEY_LAST_WEEKLY_RESET, 0)
            if (weekKey != lastWeeklyReset) {
                // Weekly reset also resets all items (in case daily already ran, this is idempotent)
                dao.resetAllChecklistItems()
                prefs.edit().putInt(KEY_LAST_WEEKLY_RESET, weekKey).apply()
            }
        }
    }

    // --- Profile methods ---

    fun getProfile(): Flow<UserProfile?> = dao.getProfile()

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { dao.upsertProfile(profile) }
    }

    fun getDos(): Flow<List<ChecklistItem>> = dao.getChecklistItems("DO")
    fun getDonts(): Flow<List<ChecklistItem>> = dao.getChecklistItems("DONT")

    fun addChecklistItem(type: String, text: String) {
        viewModelScope.launch { dao.insertChecklistItem(ChecklistItem(type = type, text = text)) }
    }

    fun toggleChecklistItem(id: Int, checked: Boolean) {
        viewModelScope.launch { dao.toggleChecklistItem(id, checked) }
    }

    fun deleteChecklistItem(item: ChecklistItem) {
        viewModelScope.launch { dao.deleteChecklistItem(item) }
    }

    fun resetDayChecklist() {
        viewModelScope.launch {
            dao.resetAllChecklistItems()
            val today = Calendar.getInstance()
            val todayKey = today.get(Calendar.YEAR) * 1000 + today.get(Calendar.DAY_OF_YEAR)
            prefs.edit().putInt(KEY_LAST_DAILY_RESET, todayKey).apply()
        }
    }

    fun resetWeekChecklist() {
        viewModelScope.launch {
            dao.resetAllChecklistItems()
            val today = Calendar.getInstance()
            val dayKey = today.get(Calendar.YEAR) * 1000 + today.get(Calendar.DAY_OF_YEAR)
            val weekKey = today.get(Calendar.YEAR) * 100 + today.get(Calendar.WEEK_OF_YEAR)
            prefs.edit()
                .putInt(KEY_LAST_DAILY_RESET, dayKey)
                .putInt(KEY_LAST_WEEKLY_RESET, weekKey)
                .apply()
        }
    }

    fun addPhotoUri(currentProfile: UserProfile?, uri: String) {
        viewModelScope.launch {
            val profile = currentProfile ?: UserProfile()
            val existing = if (profile.photoUris.isBlank()) emptyList()
            else profile.photoUris.split(",")
            val updated = (existing + uri).joinToString(",")
            dao.upsertProfile(profile.copy(photoUris = updated))
        }
    }

    fun removePhotoUri(currentProfile: UserProfile?, uri: String) {
        viewModelScope.launch {
            val profile = currentProfile ?: return@launch
            val updated = profile.photoUris.split(",")
                .filter { it != uri }
                .joinToString(",")
            dao.upsertProfile(profile.copy(photoUris = updated))
        }
    }

    // --- Google Sign-In ---

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                SyncPreference.setAccount(
                    getApplication(),
                    account?.email,
                    account?.displayName
                )
                _syncState.value = SyncState.Success("Signed in as ${account?.email}")
            } catch (e: ApiException) {
                _syncState.value = SyncState.Error("Sign-in failed (code ${e.statusCode})")
            }
        }
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(getApplication()) != null
    }

    fun signOut(context: Context) {
        getGoogleSignInClient(context).signOut().addOnCompleteListener {
            SyncPreference.clear(context)
            _syncState.value = SyncState.Idle
        }
    }

    // --- Backup / Restore ---

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(getApplication())
            ?: throw Exception("Not signed in")
        val androidAccount = account.account
            ?: throw Exception("No account available")
        GoogleAuthUtil.getToken(
            getApplication(),
            androidAccount,
            "oauth2:$DRIVE_APPDATA_SCOPE"
        )
    }

    fun backupToGoogleDrive() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Creating backup...")
            try {
                val backupData = backupManager.createBackup()
                val json = withContext(Dispatchers.Default) { gson.toJson(backupData) }

                _syncState.value = SyncState.Loading("Uploading to Google Drive...")
                val token = getAccessToken()
                val driveSync = GoogleDriveSync(token)
                val success = driveSync.uploadBackup(json)

                if (success) {
                    val now = System.currentTimeMillis()
                    SyncPreference.setLastSyncTime(getApplication(), now)
                    _syncState.value = SyncState.Success("Backup complete")
                } else {
                    _syncState.value = SyncState.Error("Upload failed")
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun restoreFromGoogleDrive() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Downloading from Google Drive...")
            try {
                val token = getAccessToken()
                val driveSync = GoogleDriveSync(token)
                val json = driveSync.downloadBackup()

                if (json == null) {
                    _syncState.value = SyncState.Error("No backup found on Google Drive")
                    return@launch
                }

                _syncState.value = SyncState.Loading("Restoring data...")
                val backupData = withContext(Dispatchers.Default) {
                    gson.fromJson(json, BackupData::class.java)
                }
                backupManager.restoreBackup(backupData)

                val now = System.currentTimeMillis()
                SyncPreference.setLastSyncTime(getApplication(), now)
                _syncState.value = SyncState.Success("Restore complete")
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Restore failed")
            }
        }
    }

    fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    // --- Workout Reminders ---

    fun getWorkoutReminders(): Flow<List<WorkoutReminder>> = dao.getAllWorkoutReminders()

    fun saveWorkoutReminder(reminder: WorkoutReminder) {
        viewModelScope.launch {
            dao.upsertWorkoutReminder(reminder)
            if (reminder.enabled && reminder.time.isNotBlank()) {
                WorkoutReminderScheduler.scheduleForDay(getApplication(), reminder)
            } else {
                WorkoutReminderScheduler.cancelForDay(getApplication(), reminder.dayOfWeek)
            }
        }
    }

    fun saveAllWorkoutReminders(reminders: List<WorkoutReminder>) {
        viewModelScope.launch {
            dao.upsertAllWorkoutReminders(reminders)
            reminders.forEach { reminder ->
                if (reminder.enabled && reminder.time.isNotBlank()) {
                    WorkoutReminderScheduler.scheduleForDay(getApplication(), reminder)
                } else {
                    WorkoutReminderScheduler.cancelForDay(getApplication(), reminder.dayOfWeek)
                }
            }
        }
    }

    // --- Motivational Quotes ---

    fun getCustomQuotes(): Flow<List<MotivationalQuote>> = dao.getAllCustomQuotes()

    fun addCustomQuote(text: String) {
        viewModelScope.launch { dao.insertCustomQuote(MotivationalQuote(text = text)) }
    }

    fun deleteCustomQuote(quote: MotivationalQuote) {
        viewModelScope.launch { dao.deleteCustomQuote(quote) }
    }

    fun setQuoteEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        QuotePreference.setEnabled(app, enabled)
        if (enabled) {
            val time = QuotePreference.getTime(app)
            QuoteReminderScheduler.schedule(app, time)
        } else {
            QuoteReminderScheduler.cancel(app)
        }
    }

    fun setQuoteSource(source: String) {
        QuotePreference.setSource(getApplication(), source)
    }

    fun setQuoteTime(time: String) {
        val app = getApplication<Application>()
        QuotePreference.setTime(app, time)
        if (QuotePreference.getEnabled(app)) {
            QuoteReminderScheduler.schedule(app, time)
        }
    }
}
