package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.ai.AiChatEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val messages: List<AiChatEngine.ChatMessage> = emptyList(),
        val isLoading: Boolean = false,
        val isModelAvailable: Boolean? = null // null = still checking
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        _uiState.value = UiState(messages = AiChatEngine.getHistory())
        checkAvailability()
    }

    private fun checkAvailability() {
        viewModelScope.launch {
            val available = AiChatEngine.isAvailable()
            _uiState.value = _uiState.value.copy(isModelAvailable = available)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = AiChatEngine.ChatMessage(text.trim(), isUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            isLoading = true
        )
        viewModelScope.launch {
            val reply = AiChatEngine.sendMessage(text.trim())
            _uiState.value = _uiState.value.copy(
                messages = AiChatEngine.getHistory(),
                isLoading = false
            )
        }
    }

    fun clearChat() {
        AiChatEngine.clearHistory()
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }
}
