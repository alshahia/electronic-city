package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    System,
    Light,
    Dark
}

/**
 * Holds the user's selected [AppThemeMode] and persists it in
 * `SharedPreferences` so the choice survives process death.
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readPersistedMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private fun readPersistedMode(): AppThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, AppThemeMode.System.name)
            ?: AppThemeMode.System.name
        return try {
            AppThemeMode.valueOf(raw)
        } catch (e: Exception) {
            AppThemeMode.System
        }
    }

    companion object {
        private const val PREFS_NAME = "ecommerce_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
