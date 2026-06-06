package com.example.ui.locals

import android.content.Context
import androidx.annotation.StringRes
import java.util.concurrent.atomic.AtomicReference

/**
 * Locale-aware resource lookup for code that lives outside a Composable
 * (D8.21). Specifically, the [androidx.lifecycle.ViewModel]s that publish
 * [MessageBus] strings.
 *
 * ## Why a singleton?
 *
 * The cleanest pre-D8.19 pattern was to call
 * `getApplication<Application>().getString(R.string.xxx)` from each VM.
 * That has two problems after the in-app locale switcher landed in D8.19:
 *
 * 1. The `Application` instance is created once per process and its
 *    `attachBaseContext` only runs on process start. The activity's
 *    `attachBaseContext` runs again on every recreate (including locale
 *    change), so wrapping the *activity's* base context — not the
 *    application's — is what makes a freshly-chosen language visible.
 * 2. VMs don't see Compose state, so they can't read
 *    `LocalContext.current`. They need a side-channel.
 *
 * The fix: the activity publishes its currently-wrapped base context here
 * on every `attachBaseContext` (which runs before `super.onCreate`, so
 * the ref is always set before any VM is constructed). VMs read from
 * this object instead of `getApplication()`.
 *
 * ## Thread safety
 *
 * `AtomicReference` gives us a memory-visibility guarantee for the
 * cross-thread read. Writes happen on the main thread (in
 * `attachBaseContext`); reads can happen from any VM coroutine. There
 * is a benign race when the activity is being recreated — the VM's
 * coroutines are cancelled by the activity's `ViewModelStore.onCleared`
 * before the new `attachBaseContext` writes the new context, so the
 * race never produces a stale string the user sees.
 *
 * ## Memory
 *
 * Holds a strong reference to the activity's base context (which
 * transitively references the activity). This is bounded by activity
 * lifetime: the next `attachBaseContext` replaces it. No need for a
 * `WeakReference` here.
 */
object LocaleResources {
    private val ref = AtomicReference<Context>()

    /**
     * Publishes [context] as the current locale-aware source. Called from
     * `MainActivity.attachBaseContext` on every activity create/recreate
     * (including locale changes). The wrapped context's `getString` will
     * resolve to the right `values-<tag>/strings.xml` for the persisted
     * tag.
     */
    fun set(context: Context) {
        ref.set(context)
    }

    /**
     * Resolves [id] against the currently-published context. Returns
     * `""` if no context has been published yet (which should never
     * happen in normal flow — the activity's `attachBaseContext` runs
     * before the VM factory is invoked).
     */
    fun getString(@StringRes id: Int): String =
        ref.get()?.getString(id) ?: ""

    /**
     * Resolves [id] with [formatArgs]. Same null-context fallback as
     * the no-arg overload.
     */
    fun getString(@StringRes id: Int, vararg formatArgs: Any): String =
        ref.get()?.getString(id, *formatArgs) ?: ""

    /**
     * Test-only escape hatch so unit tests can swap in a controlled
     * context. Production code should not call this — the activity
     * owns the ref.
     */
    @androidx.annotation.VisibleForTesting
    internal fun resetForTest() {
        ref.set(null)
    }
}
