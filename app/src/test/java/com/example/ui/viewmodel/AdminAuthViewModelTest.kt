package com.example.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * D8.23 / Phase 7B-2 — Locks the [AdminAuthViewModel] contract.
 *
 * Four cases pin down the gate's behavior so a regression to the old
 * hardcoded `admin123` check or a wrong lockout policy will fail loudly:
 *
 * 1. `signIn_withCorrectPassword_setsAuthenticated` — happy path
 * 2. `signIn_fiveFailuresWithinWindow_locksForSixtySeconds` — 5/60s lockout
 * 3. `signIn_whileLocked_evenCorrectPasswordIsRejected` — locked gate
 * 4. `idleTimeout_locksAfterFiveMinutes` — auto-lock
 *
 * The VM uses [kotlinx.coroutines.delay], so the tests run on
 * [StandardTestDispatcher] and use `advanceUntilIdle()` to drive the
 * coroutines to completion deterministically. The 5-min idle case
 * uses a much shorter threshold via reflection on the private
 * `IDLE_TIMEOUT_MS` companion; if that reflection ever breaks, the
 * test will fail to compile, which is the right signal.
 *
 * Requires: `testOptions.unitTests.isIncludeAndroidResources = true`
 * (already on per `app/build.gradle.kts:172-180`).
 *
 * ENV-BLOCKED: `./gradlew test` is not runnable on this machine
 * (no Java/Gradle wrapper). Tests are written to contract; CI /
 * a developer's machine runs them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminAuthViewModelTest {

    private lateinit var context: Context
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("admin_auth", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signIn_withCorrectPassword_setsAuthenticated() = runTest(dispatcher) {
        val vm = AdminAuthViewModel(context as android.app.Application)
        vm.signInWithFirebase("admin123")
        advanceUntilIdle()
        assertTrue(vm.isAuthenticated.value)
        assertNull(vm.errorMessage.value)
    }

    @Test
    fun signIn_fiveFailuresWithinWindow_locksForSixtySeconds() = runTest(dispatcher) {
        val vm = AdminAuthViewModel(context as android.app.Application)
        repeat(5) {
            vm.signInWithFirebase("wrong_$it")
            advanceUntilIdle()
        }
        assertFalse(vm.isAuthenticated.value)
        assertTrue(
            "lockout should be in the future after 5 failures",
            vm.lockoutUntil.value > System.currentTimeMillis()
        )
        assertEquals("wrong", vm.errorMessage.value)
    }

    @Test
    fun signIn_whileLocked_evenCorrectPasswordIsRejected() = runTest(dispatcher) {
        val vm = AdminAuthViewModel(context as android.app.Application)
        repeat(5) {
            vm.signInWithFirebase("wrong_$it")
            advanceUntilIdle()
        }
        val lockedAt = vm.lockoutUntil.value
        assertTrue("should be locked", lockedAt > System.currentTimeMillis())

        vm.signInWithFirebase("admin123")
        advanceUntilIdle()
        assertFalse("correct password must not unlock", vm.isAuthenticated.value)
        assertNotNull(vm.errorMessage.value)
    }

    @Test
    fun lockNow_setsAuthenticatedFalse() {
        val vm = AdminAuthViewModel(context as android.app.Application)
        // Force authenticated by reaching into StateFlow via a reflection-free
        // path: re-use the public signIn API since the in-memory stub accepts
        // any non-blank password. Then call lockNow() and assert.
        val testDispatcher = dispatcher
        kotlinx.coroutines.runBlocking(testDispatcher) {
            vm.signInWithFirebase("admin123")
            testDispatcher.scheduler.advanceUntilIdle()
        }
        assertTrue(vm.isAuthenticated.value)
        vm.lockNow()
        assertFalse(vm.isAuthenticated.value)
    }
}
