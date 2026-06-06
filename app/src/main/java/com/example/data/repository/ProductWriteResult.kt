package com.example.data.repository

import com.example.R

/**
 * D8.23 / H1 / Phase 7B-2 — The four possible outcomes of the admin
 * "Upload product" flow.
 *
 * The previous `Result<Unit>` shape collapsed three different
 * failure modes into one boolean: a transient offline state, a
 * local-DB write failure, and a remote-upload failure for a *non*
 * offline reason all looked identical to the UI, which always said
 * "saved to cloud" via [com.example.R.string.msg_shop_admin_ok].
 *
 * This sealed interface makes the four outcomes explicit so the
 * AccountScreen admin form can:
 *  - **reset the form** on a clean `BothOk` or an expected `LocalOnlyOffline`,
 *  - **keep the form so the admin can retry** on `BothFailed` or
 *    `LocalFailedButRemoteOk`,
 *  - and surface a precise MessageBus message keyed off
 *    [messageRes] (resolved through
 *    [com.example.ui.locals.LocaleResources] so it respects the
 *    current locale per D8.21).
 *
 * The original [Throwable] is preserved on the failure variants for
 * logcat / Crashlytics; the UI never reads it.
 */
sealed interface ProductWriteResult {
    /** String resource id; resolved at the UI layer via [com.example.ui.locals.LocaleResources]. */
    val messageRes: Int

    /** Local + remote both succeeded. The "happy path". */
    data object BothOk : ProductWriteResult {
        override val messageRes: Int = R.string.msg_shop_admin_ok_both
    }

    /**
     * Local write succeeded; remote skipped because
     * [com.example.data.remote.RemoteDatabaseService.checkConnectionDirect]
     * returned `false`. The product is on disk and will resync on the
     * next online refresh. Form **resets** because the data is
     * durable.
     */
    data class LocalOnlyOffline(val remoteError: Throwable? = null) : ProductWriteResult {
        override val messageRes: Int = R.string.msg_shop_admin_ok_local_only
    }

    /**
     * Local write *and* remote write both failed. Rare — only when
     * the device is online but the upload itself errors (e.g. a
     * network blip mid-`uploadProductOnline`). Form **stays** so the
     * admin can retry.
     */
    data class BothFailed(val throwable: Throwable) : ProductWriteResult {
        override val messageRes: Int = R.string.msg_shop_admin_both_failed
    }

    /**
     * Local write succeeded but the remote write failed for a
     * *non-offline* reason (auth, schema mismatch, etc.). The local
     * cache is the source of truth and the remote will reconcile on
     * the next sync. Form **stays** so the admin can retry.
     */
    data class LocalFailedButRemoteOk(val throwable: Throwable) : ProductWriteResult {
        override val messageRes: Int = R.string.msg_shop_admin_local_failed
    }
}
