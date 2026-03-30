package com.example.gymworkout.data.sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleDriveSync(private val accessToken: String) {

    companion object {
        private const val TAG = "GoogleDriveSync"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val BACKUP_FILENAME = "gym_workout_backup.json"
    }

    private var lastError: String? = null

    fun getLastError(): String? = lastError

    suspend fun uploadBackup(backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingId = findExistingFileId()
            if (existingId != null) {
                updateFileContent(existingId, backupJson)
            } else {
                createNewFile(backupJson)
            }
        } catch (e: Exception) {
            lastError = e.message
            Log.e(TAG, "uploadBackup failed", e)
            false
        }
    }

    suspend fun downloadBackup(): String? = withContext(Dispatchers.IO) {
        try {
            val fileId = findExistingFileId() ?: return@withContext null
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
                    Log.e(TAG, "downloadBackup: $lastError")
                    null
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            lastError = e.message
            Log.e(TAG, "downloadBackup failed", e)
            null
        }
    }

    private fun findExistingFileId(): String? {
        val query = URLEncoder.encode("name='$BACKUP_FILENAME'", "UTF-8")
        val fields = URLEncoder.encode("files(id,name)", "UTF-8")
        val url = URL("$DRIVE_FILES_URL?spaces=appDataFolder&q=$query&fields=$fields")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        return try {
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val files = json.getJSONArray("files")
                if (files.length() > 0) {
                    files.getJSONObject(0).getString("id")
                } else null
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "findExistingFileId failed (${conn.responseCode}): $errorBody")
                lastError = "Find file failed (${conn.responseCode}): $errorBody"
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun createNewFile(content: String): Boolean {
        val boundary = "gym_workout_boundary_${System.currentTimeMillis()}"
        val url = URL("$DRIVE_UPLOAD_URL?uploadType=multipart")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }

        val metadata = JSONObject().apply {
            put("name", BACKUP_FILENAME)
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

    private fun updateFileContent(fileId: String, content: String): Boolean {
        val url = URL("$DRIVE_UPLOAD_URL/$fileId?uploadType=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("X-HTTP-Method-Override", "PATCH")
        }
        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(content) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                lastError = "Update file failed ($code): $errorBody"
                Log.e(TAG, "updateFileContent: $lastError")
            }
            code in 200..299
        } finally {
            conn.disconnect()
        }
    }
}
