# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- D9.3 — Firebase keep rules ----------------------------------------
# R8 strips Firebase reflection without these. The
# `firebase-firestore-ktx` SDK uses reflection to deserialize document
# fields into `@Keep`-annotated data classes; the Play Services Tasks
# API uses reflection to wire coroutines bridges; and the Auth SDK
# dispatches to `com.google.android.gms` internals.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Preserve generic type information so Firestore can resolve
# `CollectionReference<Product>` to the right converter at runtime.
-keepattributes Signature

# Preserve annotation metadata that Firestore / Auth may consult
# (e.g. `@Keep`, `@SerializedName`).
-keepattributes *Annotation*

# Don't strip the @Keep-annotated entry points in our own data
# classes (Firestore + Moshi share this contract).
-keep,allowobfuscation @interface com.google.firebase.annotations.Keep
-keep @com.google.firebase.annotations.Keep class * { *; }
-keepclassmembers class * {
    @com.google.firebase.annotations.Keep *;
}
# --- end D9.3 ----------------------------------------------------------

# --- D9.4 — FCM keep rules ---------------------------------------------
# The FCM service class is referenced by name in AndroidManifest.xml
# (`<service android:name=".fcm.FcmService">`); R8 may rename it during
# shrinking, which would break the intent filter match. The
# `com.google.firebase.**` rule above already covers the SDK, but
# the FCM message payload uses reflection on the onNewToken /
# onMessageReceived callbacks, so we explicitly pin those.
-keep class com.example.fcm.FcmService { *; }
-keep class com.example.fcm.FcmService$* { *; }
# --- end D9.4 ----------------------------------------------------------
