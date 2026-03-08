package com.example.gymworkout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymworkout.data.WorkoutDatabase
import com.example.gymworkout.data.sync.BackupManager
import com.example.gymworkout.data.sync.GoogleDriveSync
import com.example.gymworkout.data.sync.SyncPreference
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoBackupReceiver : BroadcastReceiver() {

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val NOTIFICATION_ID = 9502
    }

    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if user is signed in
                val account = GoogleSignIn.getLastSignedInAccount(context)
                if (account?.account == null) {
                    // Not signed in, skip backup and reschedule
                    reschedule(context)
                    return@launch
                }

                // Get access token
                val accessToken = GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    "oauth2:$DRIVE_APPDATA_SCOPE"
                )

                // Create backup
                val db = WorkoutDatabase.getDatabase(context)
                val backupManager = BackupManager(db, context)
                val backupData = backupManager.createBackup()
                val json = withContext(Dispatchers.Default) { Gson().toJson(backupData) }

                // Upload to Google Drive
                val driveSync = GoogleDriveSync(accessToken)
                val success = driveSync.uploadBackup(json)

                if (success) {
                    SyncPreference.setLastSyncTime(context, System.currentTimeMillis())

                    // Show silent success notification
                    NotificationHelper.createAutoBackupNotificationChannel(context)
                    NotificationHelper.showNotification(
                        context, NOTIFICATION_ID,
                        "Auto Backup Complete",
                        "Your workout data was backed up to Google Drive.",
                        channelId = "auto_backup"
                    )
                }
            } catch (_: Exception) {
                // Silent fail - don't bother user with auto-backup errors
            }

            // Reschedule for tomorrow
            reschedule(context)
        }
    }

    private fun reschedule(context: Context) {
        if (AutoBackupScheduler.isEnabled(context)) {
            AutoBackupScheduler.schedule(context)
        }
    }
}
