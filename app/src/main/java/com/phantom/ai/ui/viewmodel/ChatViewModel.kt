package com.phantom.ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phantom.ai.data.SettingsRepository
import com.phantom.ai.network.StreamerClient
import com.phantom.ai.ui.overlay.ChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val client = StreamerClient()
    private val settingsRepo = SettingsRepository(app)

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("🐴 Trojan Horse deployed. What's the mission?", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _connectionStatus = MutableStateFlow("disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    val settingsFlow = settingsRepo.settingsFlow

    init {
        viewModelScope.launch {
            com.phantom.ai.ui.overlay.ChatEventBus.events.collect { msg ->
                _messages.value = _messages.value + msg
            }
        }
    }

    private var streamJob: Job? = null
    private var currentUrl: String = ""

    fun connect(baseUrl: String) {
        if (baseUrl.isBlank() || baseUrl == currentUrl) return
        currentUrl = baseUrl

        streamJob?.cancel()
        _connectionStatus.value = "connecting"

        streamJob = viewModelScope.launch {
            _connectionStatus.value = "connected"
            client.stream(baseUrl).collect { msg ->
                _messages.value = _messages.value + msg
            }
            // If flow ends (disconnected), update status
            _connectionStatus.value = "reconnecting"
        }
    }

    fun sendPrompt(baseUrl: String, text: String) {
        if (text.isBlank()) return
        // Add user bubble immediately (optimistic)
        _messages.value = _messages.value + ChatMessage(text, isUser = true)

        viewModelScope.launch {
            val ok = client.sendPrompt(baseUrl, text)
            if (!ok) {
                _messages.value = _messages.value +
                    ChatMessage("⚠️ Failed to reach VPS streamer — check connection.", isUser = false)
            }
        }
    }

    fun clearMessages() {
        _messages.value = listOf(ChatMessage("🐴 Chat cleared. Ready.", isUser = false))
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
