package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * D8.23 / Phase 7B-2 — Owns admin auth state for the AccountScreen
 * admin tabs. Replaces the hardcoded `admin123` check that lived in
 * `AccountScreen.kt:896` and the `remember { mutableStateOf(false) }`
 * at line 65 (which made `Activity.recreate()` keep the gate open).
 *
 * Responsibilities:
 * 1. **Lockout** (5 wrong passwords in any 60s window ⇒ 60s lockout).
 *    Persisted in `SharedPreferences` so it survives process death.
 * 2. **Idle timeout** (5 min of no admin-area interaction ⇒ auto-lock).
 *    In-memory only (resets on process death; the lockout-on-restart
 *    policy lives with Firebase Auth, D9.3).
 * 3. **Server-side auth claim** via
 *    [com.example.data.remote.RemoteDatabaseService.requireAdmin]
 *    (in-memory stub accepts any non-blank password until D9.3).
 * 4. **Lock-now** explicit action wired to the back arrow of the
 *    admin sheet.
 */
class AdminAuthViewModel(application: Application) : AndroidViewModel(application) {

    private val remote = ServiceLocator.getRemoteService(application)

    private val prefs = application.getSharedPreferences(PREFS, Application.MODE_PRIVATE)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _lockoutUntil = MutableStateFlow(0L)
    val lockoutUntil: StateFlow<Long> = _lockoutUntil.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastActivityAt: Long = System.currentTimeMillis()
    private var idleWatcherJob: Job? = null

    init {
        _lockoutUntil.value = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
    }

    /**
     * Attempt to sign in as admin. Drives the lockout window
     * forward on every failure. On success, resets the lockout
     * counters and starts the idle-timeout watcher.
     */
    fun signInWithFirebase(password: String) {
        if (_isAuthenticating.value) return
        if (System.currentTimeMillis() < _lockoutUntil.value) {
            _errorMessage.value = "lockout"
            return
        }
        _isAuthenticating.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = remote.requireAdmin(password)
            _isAuthenticating.value = false
            result.fold(
                onSuccess = {
                    prefs.edit().remove(KEY_LOCKOUT_UNTIL).apply()
                    _lockoutUntil.value = 0L
                    _isAuthenticated.value = true
                    recordActivity()
                    startIdleWatcher()
                },
                onFailure = {
                    registerFailure()
                    _errorMessage.value = "wrong"
                }
            )
        }
    }

    fun lockNow() {
        _isAuthenticated.value = false
        idleWatcherJob?.cancel()
        idleWatcherJob = null
    }

    /**
     * Called from every admin-area `onClick` so the idle clock
     * resets. Cheap to call (just a Long write).
     */
    fun recordActivity() {
        lastActivityAt = System.currentTimeMillis()
    }

    fun consumeError() {
        _errorMessage.value = null
    }

    private fun registerFailure() {
        val now = System.currentTimeMillis()
        val firstFailure = prefs.getLong(KEY_FIRST_FAILURE, 0L)
        val failures = prefs.getInt(KEY_FAILURES, 0)
        val reset = firstFailure == 0L || now - firstFailure > LOCKOUT_WINDOW_MS
        val next = if (reset) 1 else failures + 1
        prefs.edit()
            .putLong(KEY_FIRST_FAILURE, if (reset) now else firstFailure)
            .putInt(KEY_FAILURES, next)
            .apply()
        if (next >= MAX_FAILURES) {
            val until = now + LOCKOUT_DURATION_MS
            prefs.edit().putLong(KEY_LOCKOUT_UNTIL, until).apply()
            _lockoutUntil.value = until
            prefs.edit().remove(KEY_FAILURES).remove(KEY_FIRST_FAILURE).apply()
        }
    }

    private fun startIdleWatcher() {
        idleWatcherJob?.cancel()
        idleWatcherJob = viewModelScope.launch {
            while (_isAuthenticated.value) {
                delay(IDLE_CHECK_INTERVAL_MS)
                if (System.currentTimeMillis() - lastActivityAt >= IDLE_TIMEOUT_MS) {
                    _isAuthenticated.value = false
                    break
                }
            }
        }
    }

    override fun onCleared() {
        idleWatcherJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val PREFS = "admin_auth"
        const val KEY_LOCKOUT_UNTIL = "lockout_until"
        const val KEY_FIRST_FAILURE = "first_failure"
        const val KEY_FAILURES = "failures"
        const val MAX_FAILURES = 5
        const val LOCKOUT_WINDOW_MS = 60_000L
        const val LOCKOUT_DURATION_MS = 60_000L
        const val IDLE_TIMEOUT_MS = 5 * 60_000L
        const val IDLE_CHECK_INTERVAL_MS = 30_000L
    }
}
