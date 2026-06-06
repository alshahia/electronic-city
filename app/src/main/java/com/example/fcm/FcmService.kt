package com.example.fcm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.di.ServiceLocator
import com.example.ui.locals.LocaleResources
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * D9.4 — Firebase Cloud Messaging service.
 *
 * Handles two callbacks from the FCM SDK:
 *  - [onNewToken] — fires once on first install and again whenever the
 *    token rotates. We persist the new token to `SharedPreferences`
 *    (so the value survives process death and the user can re-trigger
 *    upload from the admin screen) and forward it to the active
 *    [com.example.data.remote.RemoteDatabaseService] so the backend
 *    can target this device.
 *  - [onMessageReceived] — fires when a notification payload arrives
 *    while the app is in the foreground. We build a
 *    [NotificationCompat] using the `ecity_orders` channel and post it
 *    to the system tray. Title/body default to locale-aware strings
 *    if the payload doesn't include them.
 *
 * Manifest: declared in `AndroidManifest.xml` as
 * `<service android:name=".fcm.FcmService" android:exported="false">`
 * with the `com.google.firebase.MESSAGING_EVENT` intent filter. The
 * declaration is always present; if Firebase isn't configured
 * (no `google-services.json`), the service is never instantiated.
 *
 * Proguard: the Firebase keep rules in `app/proguard-rules.pro`
 * cover `FirebaseMessagingService` and its `RemoteMessage` payload.
 */
class FcmService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        // Cache the token locally so we can re-upload after sign-out /
        // sign-in and so the admin can read the current value.
        getSharedPreferences(FCM_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEVICE_TOKEN, token)
            .apply()

        // Forward to the active backend. ServiceLocator.getRemoteService
        // returns the contract, so this works for both the in-memory
        // demo impl (no-op) and the Firestore impl (writes to
        // users/{uid}/devices/{tokenId}).
        scope.launch {
            runCatching {
                ServiceLocator.getRemoteService(this@FcmService)
                    .uploadDeviceToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Resolve locale-aware fallback text via LocaleResources so the
        // notification matches the active language (D8.21 invariant).
        val defaultTitle = LocaleResources.getString(R.string.notification_default_title)
        val defaultBody = LocaleResources.getString(R.string.notification_default_body)

        val title = message.notification?.title
            ?: message.data[TITLE_KEY]
            ?: defaultTitle
        val body = message.notification?.body
            ?: message.data[BODY_KEY]
            ?: defaultBody

        // Tapping the notification re-opens the app at MainActivity.
        // FLAG_IMMUTABLE is required on Android 12+ (API 31+).
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ORDERS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // NotificationManagerCompat.notify is a no-op on devices
        // where notifications are disabled or POST_NOTIFICATIONS was
        // denied, so we don't need a runtime check here.
        NotificationManagerCompat.from(this).notify(message.messageId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ORDERS_ID: String = "ecity_orders"
        const val FCM_PREFS: String = "fcm_prefs"
        const val KEY_DEVICE_TOKEN: String = "device_token"
        private const val TITLE_KEY: String = "title"
        private const val BODY_KEY: String = "body"
    }
}
