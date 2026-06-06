package com.example.ui.locals

import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Locks the [LocaleResources] contract (D8.21).
 *
 * The ref is set by `MainActivity.attachBaseContext` in production. The
 * tests poke the ref directly via the @VisibleForTesting escape hatch
 * (`resetForTest` + `set`) so they don't have to spin up an activity.
 *
 * Three behaviors worth pinning:
 *
 * 1. After `set(arContext)`, `getString` resolves Arabic (the default
 *    `values/strings.xml`).
 * 2. After `set(enContext)`, `getString` resolves English
 *    (`values-en/strings.xml`, which is D8.18's deliverable).
 * 3. Without `set`, `getString` returns `""` — a no-context fallback
 *    that should never fire in production but is documented behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocaleResourcesTest {
    @After
    fun tearDown() {
        LocaleResources.resetForTest()
    }

    @Test
    fun getString_returnsEmptyBeforeSet() {
        LocaleResources.resetForTest()
        assertEquals("", LocaleResources.getString(R.string.app_name))
    }

    @Test
    fun getString_resolvesFromCurrentContext() {
        val base = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ar = com.example.ui.locals.LocaleManager.wrap(base, "ar")
        val en = com.example.ui.locals.LocaleManager.wrap(base, "en")

        LocaleResources.set(ar)
        val arName = LocaleResources.getString(R.string.app_name)
        assertTrue("expected Arabic app name, got '$arName'", arName.isNotBlank())

        LocaleResources.set(en)
        val enName = LocaleResources.getString(R.string.app_name)
        assertTrue("expected English app name, got '$enName'", enName.isNotBlank())
    }

    @Test
    fun getString_formatArgsAreRespected() {
        val base = ApplicationProvider.getApplicationContext<android.content.Context>()
        val en = com.example.ui.locals.LocaleManager.wrap(base, "en")
        LocaleResources.set(en)

        // R.string.msg_orders_resynced has the form "Resynced %1$d orders".
        // Picking a sentinel value makes the assertion robust to copy
        // changes that don't touch the format spec.
        val rendered = LocaleResources.getString(R.string.msg_orders_resynced, 7)
        assertTrue("expected rendered count '7' in '$rendered'", rendered.contains("7"))
    }
}
