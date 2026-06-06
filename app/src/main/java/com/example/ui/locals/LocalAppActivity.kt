package com.example.ui.locals

import androidx.activity.ComponentActivity
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Provides the host [ComponentActivity] to composables below the root.
 *
 * Replaces the [android.content.ContextWrapper] walk in `MainScreen` that
 * used to look up the activity for `activity.finish()` from the back-press
 * handler. Set once in `MainActivity.onCreate`; reads are O(1).
 */
val LocalAppActivity = staticCompositionLocalOf<ComponentActivity> {
    error(
        "LocalAppActivity has no default — provide it in MainActivity via " +
            "CompositionLocalProvider(LocalAppActivity provides this) { ... }"
    )
}
