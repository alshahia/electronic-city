package com.example.ui.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide one-shot event bus used by ViewModels to surface
 * toast/snackbar messages without depending on Android `Context`.
 *
 * Kept internal-scope: only ViewModels that own user-visible state publish
 * here, and the host composable observes the stream in a single place.
 */
object MessageBus {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun publish(message: String) {
        _messages.tryEmit(message)
    }
}
