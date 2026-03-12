package com.phantom.ai.ui.overlay

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChatEventBus {
    private val _events = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 10)
    val events: SharedFlow<ChatMessage> = _events.asSharedFlow()

    fun postMessage(message: ChatMessage) {
        _events.tryEmit(message)
    }
}
