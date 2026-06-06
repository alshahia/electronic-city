package com.example.ui.locals

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Provides the host [WindowSizeClass] to composables below the root.
 *
 * Set once in `MainActivity.onCreate` via `calculateWindowSizeClass(this)`
 * and a `CompositionLocalProvider(LocalWindowSizeClass provides ...)`.
 * Default fallback is a phone-shaped size so preview composables and
 * Robolectric tests do not need to wire it up explicitly.
 *
 * Why `staticCompositionLocalOf` (not `compositionLocalOf`)? The value is
 * read by relatively few composables and never changes during a single
 * activity lifetime, so a static read is faster and emits fewer invalidations
 * than the dynamic flavor.
 */
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
    DefaultWindowSizeClass
}

/**
 * Phone-shaped fallback used by [LocalWindowSizeClass] when no provider is
 * present. Matches a typical 360×640dp phone so callers get a sensible
 * Compact-width layout instead of crashing.
 */
val DefaultWindowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(
    DpSize(width = 360.dp, height = 640.dp)
)
