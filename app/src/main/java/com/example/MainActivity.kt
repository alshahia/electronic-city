package com.example

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import com.example.data.di.ServiceLocator
import com.example.fcm.FcmService
import com.example.ui.locals.LocalAppActivity
import com.example.ui.locals.LocalWindowSizeClass
import com.example.ui.locals.LocaleManager
import com.example.ui.locals.LocaleResources
import com.example.ui.screens.AppRoot
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.AppViewModelsFactory
import com.example.ui.viewmodel.MessageBus
import com.example.ui.viewmodel.rememberAppViewModels
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.messaging.FirebaseMessaging
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    // D8.19 — wrap the base context with the persisted locale before
    // super.onCreate so Android's resource resolution picks the right
    // values-<tag>/ folder and the layout direction derives from the
    // locale instead of a hardcoded LayoutDirection.Rtl.
    //
    // D8.21 — also publish the wrapped context to LocaleResources so
    // non-Composable code (specifically the ViewModels) can resolve
    // strings in the active locale. Runs before super.onCreate, so
    // any VM constructed in onCreate sees a non-null ref.
    override fun attachBaseContext(newBase: Context) {
        val wrapped = LocaleManager.wrapFromPrefs(newBase)
        LocaleResources.set(wrapped)
        super.attachBaseContext(wrapped)
    }

    /**
     * Switches the app's language to [tag] (BCP-47, e.g. "ar" or "en"),
     * persists the choice, and recreates the activity so the change takes
     * effect immediately. Called by the in-app "Language" card in
     * [com.example.ui.screens.AccountScreen].
     */
    fun setAppLocale(tag: String) {
        LocaleManager.writeLanguageTag(this, tag)
        recreate()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Supports full drawing on notched and gesture system bars
        enableEdgeToEdge()

        // D9.4 — create the FCM notification channel before any
        // message can arrive. Channels are idempotent on re-creation
        // (the system keeps the user's per-channel preferences
        // intact), so this is safe to call on every cold start.
        // Guarded by `USE_FIREBASE` so debug builds without a
        // google-services.json don't pollute system settings with a
        // channel the app will never use.
        if (BuildConfig.USE_FIREBASE) {
            createOrdersNotificationChannel()
            fetchAndUploadFcmToken()
        }

        val factory = AppViewModelsFactory(application)

        setContent {
            val vms = rememberAppViewModels(factory = factory)
            val themeMode by vms.theme.themeMode.collectAsState()

            val isDarkTheme = when (themeMode) {
                AppThemeMode.System -> isSystemInDarkTheme()
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }

            // The transient snackbar text is no longer a StateFlow field on a
            // single mega-VM; we drain the process-wide MessageBus here and
            // hold the most recent message in local state for the snackbar.
            var syncMessage by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                MessageBus.messages.collectLatest { msg ->
                    syncMessage = msg
                    delay(3_500)
                    if (syncMessage == msg) syncMessage = null
                }
            }

            // D9.4 — ask for POST_NOTIFICATIONS on Android 13+. The
            // accompanist helper launches the system dialog the first
            // time it's invoked and remembers the result via
            // PermissionStatus. No-op on older Android versions.
            if (BuildConfig.USE_FIREBASE) {
                NotificationPermissionEffect()
            }

            // D8.19 — LayoutDirection is no longer pinned here. Compose
            // derives it from the active locale (set by attachBaseContext),
            // so Arabic shows RTL and English shows LTR automatically.
            //
            // D9.1 — WindowSizeClass is computed once per activity and
            // provided to composables that branch on form factor
            // (ProductsScreen, FavoritesScreen, HomeScreen carousels,
            // AccountScreen admin tabs). calculateWindowSizeClass reads
            // the activity's current window metrics, so it is correct
            // after configuration changes and on multi-window / foldables.
            val windowSizeClass = calculateWindowSizeClass(this)
            CompositionLocalProvider(
                LocalAppActivity provides this,
                LocalWindowSizeClass provides windowSizeClass,
            ) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    AppRoot(
                        vms = vms,
                        content = { bundle ->
                            com.example.ui.screens.MainScreen(
                                vms = bundle,
                                transientMessage = syncMessage,
                                onTransientMessageShown = { syncMessage = null }
                            )
                        }
                    )
                }
            }
        }
    }

    /**
     * D9.4 — Creates the `ecity_orders` notification channel used by
     * [FcmService]. Idempotent: the system ignores calls for an
     * existing channel ID with matching properties, but keeps the
     * user's per-channel settings.
     */
    private fun createOrdersNotificationChannel() {
        val channel = NotificationChannel(
            FcmService.CHANNEL_ORDERS_ID,
            LocaleResources.getString(R.string.notification_channel_orders_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = LocaleResources.getString(R.string.notification_channel_orders_desc)
        }
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    /**
     * D9.4 — Reads the current FCM registration token and forwards
     * it to the active [com.example.data.remote.RemoteDatabaseService].
     * The Firestore impl writes the token to
     * `users/{uid}/devices/{tokenId}`; the in-memory impl no-ops.
     * Runs on the activity's `lifecycleScope` so the result is
     * cancelled when the activity is destroyed.
     */
    private fun fetchAndUploadFcmToken() {
        lifecycleScope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                ServiceLocator.getRemoteService(this@MainActivity)
                    .uploadDeviceToken(token)
            }
        }
    }
}

/**
 * D9.4 — Top-level composable that asks the user to grant
 * `POST_NOTIFICATIONS` on Android 13+ (API 33). The
 * accompanist-permissions API returns a `PermissionState` whose
 * `launchPermissionRequest` triggers the system dialog. We only
 * fire it once per process (via `LaunchedEffect(Unit)`); if the
 * user already granted or denied, the call is a no-op.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NotificationPermissionEffect() {
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
        if (permissionState.status !is PermissionStatus.Granted) {
            permissionState.launchPermissionRequest()
        }
    }
}
