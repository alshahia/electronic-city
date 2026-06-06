package com.example.ui.locals

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.VisibleForTesting
import java.util.Locale

/**
 * Lightweight per-app locale switcher (D8.19).
 *
 * Persists a single BCP-47 language tag in a private SharedPreferences file
 * and rewraps the base context of [android.app.Activity] via
 * [Context.createConfigurationContext] so Android's resource resolution
 * picks the matching `values-<tag>/` strings.xml and the layout direction
 * derives from the locale (Arabic = RTL, English = LTR).
 *
 * Why not `AppCompatDelegate.setApplicationLocales(...)`? The app is
 * Compose-only and uses [androidx.activity.ComponentActivity]; pulling in
 * `androidx.appcompat:appcompat` just for per-app locales would add ~700KB
 * and force a base-class swap. The trade-off is that this implementation
 * does not surface in the Android 13+ system per-app-locale settings —
 * switching happens only via the in-app [com.example.ui.screens.AccountScreen]
 * "Language" card. Acceptable for now; revisit if a system-settings entry
 * becomes a hard requirement.
 *
 * Default language tag is `"ar"` (matches the Arabic-first `values/strings.xml`
 * default and the original hardcoded `LocalLayoutDirection.Rtl` behavior, so
 * users on Arabic system locales see no visual change on first launch).
 */
object LocaleManager {
    const val PREFS_NAME: String = "locale_prefs"
    const val KEY_LANGUAGE_TAG: String = "language_tag"
    const val DEFAULT_LANGUAGE_TAG: String = "ar"

    /**
     * Returns the persisted BCP-47 language tag, or [DEFAULT_LANGUAGE_TAG]
     * if nothing is persisted yet. Never returns `null`.
     */
    fun readLanguageTag(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, null)
            ?: DEFAULT_LANGUAGE_TAG

    /**
     * Persists [tag] in private SharedPreferences. Pass `null` to reset to
     * [DEFAULT_LANGUAGE_TAG] (currently identical to "ar", but using the
     * named constant makes the intent explicit and survives a future
     * change of default).
     */
    fun writeLanguageTag(context: Context, tag: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val effective = tag ?: DEFAULT_LANGUAGE_TAG
        prefs.edit().putString(KEY_LANGUAGE_TAG, effective).apply()
    }

    /**
     * Wraps [base] with the locale resolved from [tag]. Sets
     * [Locale.setDefault] (required so [String.toLowerCase] /
     * `SimpleDateFormat` etc. follow the same locale) and returns a new
     * context whose resources resolve to `values-<tag>/` if present.
     *
     * Safe to call from `Activity.attachBaseContext` (runs before
     * `super.onCreate`).
     */
    fun wrap(base: Context, tag: String): Context {
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * Wraps [base] using the currently persisted tag. Convenience for
     * `attachBaseContext`.
     */
    fun wrapFromPrefs(base: Context): Context = wrap(base, readLanguageTag(base))

    @VisibleForTesting
    internal const val SUPPORTED_TAGS: String = "ar,en"
}
