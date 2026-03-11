package com.example.gymworkout.ai

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel

object AiChatEngine {

    private const val TAG = "AiChatEngine"

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    private var generativeModel: GenerativeModel? = null
    private var modelAvailable: Boolean? = null
    private val conversationHistory = mutableListOf<ChatMessage>()

    suspend fun isAvailable(): Boolean {
        if (modelAvailable != null) return modelAvailable!!
        return try {
            val model = Generation.getClient()
            val status = model.checkStatus()
            Log.d(TAG, "Feature status: $status")
            when (status) {
                FeatureStatus.AVAILABLE -> {
                    generativeModel = model
                    modelAvailable = true
                    true
                }
                FeatureStatus.DOWNLOADABLE -> {
                    Log.d(TAG, "Model downloadable, starting download...")
                    model.download().collect { downloadStatus ->
                        Log.d(TAG, "Download status: $downloadStatus")
                    }
                    generativeModel = model
                    modelAvailable = true
                    true
                }
                else -> {
                    Log.w(TAG, "Gemini Nano status: $status")
                    modelAvailable = false
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Nano not available", e)
            modelAvailable = false
            false
        }
    }

    suspend fun sendMessage(userMessage: String): String {
        conversationHistory.add(ChatMessage(userMessage, isUser = true))

        val model = generativeModel ?: return "Gemini Nano is not available on this device."

        return try {
            val prompt = buildPrompt(userMessage)
            val response = model.generateContent(prompt)
            val reply = response.candidates.firstOrNull()?.text?.trim()
                ?: "Sorry, I couldn't generate a response."
            conversationHistory.add(ChatMessage(reply, isUser = false))
            reply
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            val error = "Sorry, something went wrong: ${e.message}"
            conversationHistory.add(ChatMessage(error, isUser = false))
            error
        }
    }

    private fun buildPrompt(currentMessage: String): String {
        val recentHistory = conversationHistory
            .dropLast(1)
            .takeLast(10)

        if (recentHistory.isEmpty()) return currentMessage

        val sb = StringBuilder()
        sb.appendLine("Previous conversation:")
        for (msg in recentHistory) {
            val role = if (msg.isUser) "User" else "Assistant"
            sb.appendLine("$role: ${msg.text}")
        }
        sb.appendLine()
        sb.appendLine("User: $currentMessage")
        sb.appendLine("Assistant:")
        return sb.toString()
    }

    fun getHistory(): List<ChatMessage> = conversationHistory.toList()

    fun clearHistory() {
        conversationHistory.clear()
    }
}
