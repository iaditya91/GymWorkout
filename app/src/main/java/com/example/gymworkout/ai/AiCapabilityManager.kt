package com.example.gymworkout.ai

import android.os.Build
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for whether on-device Gemini Nano is usable on this
 * device. Drives visibility of AI Chat and AI Objective Generator UI. AI Daily
 * Planner has a rule-based fallback and does not depend on this flag.
 */
object AiCapabilityManager {

    private const val TAG = "AiCapabilityManager"

    private val _isAiSupported = MutableStateFlow<Boolean?>(null)
    /** null = not yet determined, true/false = known. */
    val isAiSupported: StateFlow<Boolean?> = _isAiSupported.asStateFlow()

    suspend fun refresh() {
        if (Build.VERSION.SDK_INT < 31) {
            _isAiSupported.value = false
            return
        }
        val supported = try {
            val status = Generation.getClient().checkStatus()
            Log.d(TAG, "Gemini Nano feature status: $status")
            status == FeatureStatus.AVAILABLE || status == FeatureStatus.DOWNLOADABLE
        } catch (e: Exception) {
            Log.w(TAG, "Gemini Nano capability check failed", e)
            false
        }
        _isAiSupported.value = supported
    }
}
