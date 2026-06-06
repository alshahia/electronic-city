# ─────────────────────────────────────────────────────────────────────
# إلكترونك-سيتي — R8 / ProGuard keep rules
#
# Most dependencies (Compose, Room, Moshi, Coil, Retrofit, OkHttp,
# Coroutines, Firebase) ship their own consumer-proguard-rules.pro and
# are handled automatically by R8. The rules below are explicit
# defensive keeps for our own code and for anything that uses reflection.
# ─────────────────────────────────────────────────────────────────────

# Preserve Kotlin metadata for any library that uses Kotlin reflection
# (e.g. Moshi reflective adapter, kotlinx-serialization reflection).
-keep class kotlin.Metadata { *; }

# ─── Room ────────────────────────────────────────────────────────────
# Room generates *_Impl classes for every @Database and @Dao via KSP.
# Those impls are referenced by name in the generated code, which R8
# can usually track. The keeps below are belt-and-suspenders: protect
# the public surface of our entities and DAOs in case R8 ever needs
# reflection-based instantiation.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ─── Moshi (KSP codegen) ─────────────────────────────────────────────
# Moshi's KSP codegen generates a `<ClassName>JsonAdapter` next to every
# @JsonClass(generateAdapter = true). The generated adapter references
# the @JsonClass by name; keep both the annotated type and the adapter.
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class **JsonAdapter { *; }
-keep class **JsonAdapter$* { *; }
# If you ever switch to the reflective adapter (without codegen), also
# need: -keepclassmembers class * { @com.squareup.moshi.Json *; }
-dontwarn com.squareup.moshi.**

# ─── Compose ─────────────────────────────────────────────────────────
# Compose runtime is fine out of the box (its consumer rules are
# bundled). Keep Composable function lambdas' generated synthetic
# classes just in case reflection-based tooling is used.
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ─── Coroutines ──────────────────────────────────────────────────────
# Defensive: coroutines ship consumer rules, but stack-trace recovery
# uses reflection on internal class names.
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    public *** *(...);
}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    public *** *(...);
}
-dontwarn kotlinx.coroutines.**

# ─── Our DTOs (defensive) ────────────────────────────────────────────
# Anything in data/model that is serialized over the wire and survives
# without @JsonClass (or before codegen runs) is still reflected on by
# name in places like OrderRepository.
-keep class com.example.data.model.** { *; }
