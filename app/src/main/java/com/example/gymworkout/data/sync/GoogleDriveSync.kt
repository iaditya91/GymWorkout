package com.example.gymworkout.data.sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Metadata for a single backup version stored in Google Drive.
 */
data class DriveBackupInfo(
    val id: String,
    val name: String,
    val createdTime: String
)

class GoogleDriveSync(private val accessToken: String) {

    companion object {
        private const val TAG = "GoogleDriveSync"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

        // All backup versions share this prefix. Each upload creates a new timestamped
        // file (e.g. "gym_workout_backup_1718000000000.json") rather than overwriting a
        // single file, so a bad/empty backup can never destroy earlier good copies.
        private const val BACKUP_PREFIX = "gym_workout_backup"

        // How many historical versions to keep in Drive. Older ones are pruned.
        private const val MAX_VERSIONS = 7
    }

    private var lastError: String? = null

    fun getLastError(): String? = lastError

    /**
     * Uploads a new backup version. Never overwrites existing versions; instead creates
     * a fresh timestamped file and prunes the oldest beyond [MAX_VERSIONS].
     */
    suspend fun uploadBackup(backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val name = "${BACKUP_PREFIX}_${System.currentTimeMillis()}.json"
            val created = createNewFile(name, backupJson)
            if (created) {
                // Best-effort prune; failure to prune must not fail the backup.
                try {
                    pruneOldBackups(MAX_VERSIONS)
                } catch (e: Exception) {
                    Log.w(TAG, "pruneOldBackups failed", e)
                }
            }
            created
        } catch (e: Exception) {
            lastError = e.message
            Log.e(TAG, "uploadBackup failed", e)
            false
        }
    }

    /**
     * Downloads the most recent backup version (newest by Drive createdTime).
     */
    suspend fun downloadBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val newest = listBackupFiles().firstOrNull()
            if (newest == null) {
                lastError = "No backup found on Google Drive"
                return@withContext null
            }
            downloadBackupById(newest.id)
        } catch (e: Exception) {
            lastError = e.message
            Log.e(TAG, "downloadBackup failed", e)
            null
        }
    }

    /**
     * Lists all available backup versions, newest first. Enables a restore picker so the
     * user can recover from an older version if the latest is unwanted.
     */
    suspend fun listBackups(): List<DriveBackupInfo> = withContext(Dispatchers.IO) {
        try {
            listBackupFiles()
        } catch (e: Exception) {
            lastError = e.message
            Log.e(TAG, "listBackups failed", e)
            emptyList()
        }
    }

    /**
     * Downloads a specific backup version by its Drive file id.
     */
    suspend fun downloadBackupById(fileId: String): String? = withContext(Dispatchers.IO) {
        val url = URL("$DRIVE_FILES_URL/$fileId?alt=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        try {
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                lastError = "Download failed (${conn.responseCode}): $errorBody"
                Log.e(TAG, "downloadBackupById: $lastError")
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Returns all backup files in the app data folder, ordered newest first.
     * Matches both the new "gym_workout_backup_<ts>.json" versions and any legacy
     * single "gym_workout_backup.json" file (both start with [BACKUP_PREFIX]).
     */
    private fun listBackupFiles(): List<DriveBackupInfo> {
        val query = URLEncoder.encode(
            "name contains '$BACKUP_PREFIX' and trashed = false", "UTF-8"
        )
        val fields = URLEncoder.encode("files(id,name,createdTime)", "UTF-8")
        val orderBy = URLEncoder.encode("createdTime desc", "UTF-8")
        val url = URL(
            "$DRIVE_FILES_URL?spaces=appDataFolder&q=$query&fields=$fields&orderBy=$orderBy"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return try {
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val files = JSONObject(response).getJSONArray("files")
                val result = ArrayList<DriveBackupInfo>(files.length())
                for (i in 0 until files.length()) {
                    val f = files.getJSONObject(i)
                    result.add(
                        DriveBackupInfo(
                            id = f.getString("id"),
                            name = f.optString("name"),
                            createdTime = f.optString("createdTime")
                        )
                    )
                }
                result
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "listBackupFiles failed (${conn.responseCode}): $errorBody")
                lastError = "List backups failed (${conn.responseCode}): $errorBody"
                emptyList()
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Deletes the oldest versions, keeping the newest [keep] backups.
     */
    private fun pruneOldBackups(keep: Int) {
        val files = listBackupFiles()
        if (files.size <= keep) return
        files.drop(keep).forEach { deleteFile(it.id) }
    }

    private fun deleteFile(fileId: String): Boolean {
        val url = URL("$DRIVE_FILES_URL/$fileId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                Log.w(TAG, "deleteFile failed ($code): $errorBody")
            }
            code in 200..299
        } finally {
            conn.disconnect()
        }
    }

    private fun createNewFile(name: String, content: String): Boolean {
        val boundary = "gym_workout_boundary_${System.currentTimeMillis()}"
        val url = URL("$DRIVE_UPLOAD_URL?uploadType=multipart")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }

        val metadata = JSONObject().apply {
            put("name", name)
            put("parents", org.json.JSONArray().put("appDataFolder"))
            put("mimeType", "application/json")
        }.toString()

        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(content)
            append("\r\n--$boundary--")
        }

        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                lastError = "Create file failed ($code): $errorBody"
                Log.e(TAG, "createNewFile: $lastError")
            }
            code in 200..299
        } finally {
            conn.disconnect()
        }
    }
}
