package com.example.ui.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Phase 6.4 — Haptic feedback helper.
 *
 * Returns a stable lambda that performs a [HapticFeedbackType.LongPress]
 * click. Use in `onClick` handlers that should feel "tactile" (Phase 6.4
 * targets add-to-cart and toggle-favorite). The lambda is remembered so
 * it stays referentially stable across recompositions and won't trigger
 * Compose's `remember`-on-lambda invalidation.
 *
 * Why LongPress and not a TextHandleMove or Compose's newer
 * Confirm/ContextClick? LongPress is the broadest-supported, no-permission
 * haptic type on Android; Toggle/Confirm are 1.4+ and gated on device
 * support, so we stay on LongPress for now.
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { haptic }
}

/**
 * Variant for callers that want a plain () -> Unit. Use when an onClick
 * signature doesn't accept [HapticFeedback] directly.
 */
@Composable
fun rememberHapticClick(): () -> Unit {
    val haptic = rememberHapticFeedback()
    return remember(haptic) {
        { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    }
}

/**
 * Wrap a click handler with [HapticFeedbackType.LongPress] feedback.
 * The wrapper performs the haptic first, then invokes [onClick], so
 * callers can drop the result straight into an `onClick` slot. The
 * returned lambda is remembered so it stays referentially stable across
 * recompositions.
 */
@Composable
fun rememberHapticClick(onClick: () -> Unit): () -> Unit {
    val haptic = rememberHapticFeedback()
    return remember(haptic, onClick) {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }
}
