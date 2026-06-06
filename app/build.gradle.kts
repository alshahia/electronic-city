plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.detekt)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.arabianshop.ecity"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      // D8.6 — fail loud if any keystore credential is missing in CI.
      val keystorePath = System.getenv("KEYSTORE_PATH")
        ?: error("KEYSTORE_PATH env var is required for release builds (D8.6)")
      val storePassword = System.getenv("STORE_PASSWORD")
        ?: error("STORE_PASSWORD env var is required for release builds (D8.6)")
      val keyPassword = System.getenv("KEY_PASSWORD")
        ?: error("KEY_PASSWORD env var is required for release builds (D8.6)")

      storeFile = file(keystorePath)
      this.storePassword = storePassword
      keyAlias = "upload"
      this.keyPassword = keyPassword
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // Phase 5.1 — R8 + resource shrinking for release builds.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      // D9.3 — flag flipped per build type so debug uses the in-memory
      // demo backend (no Firebase project required to run the app)
      // and release uses real Firestore. The flag is read by
      // `ServiceLocator.getRemoteService(...)` to pick the impl.
      buildConfigField("boolean", "USE_FIREBASE", "true")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      // D9.3 — debug builds keep the in-memory `FirebaseDatabaseServiceImpl`
      // (which the demo "toggle connectivity" UI controls) so a fresh
      // clone runs without dropping in `google-services.json`.
      buildConfigField("boolean", "USE_FIREBASE", "false")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

  // Phase 5.3 — lint warnings treated as errors for release. Baseline lets
  // us suppress existing issues while we clean them up incrementally.
  lint {
    warningsAsErrors = true
    abortOnError = true
    baseline = file("lint-baseline.xml")
    checkReleaseBuilds = true
  }
}

// Phase 5.3 (D4) — detekt static analysis.
detekt {
  toolVersion = libs.versions.detekt.get()
  config.setFrom("$rootDir/config/detekt/detekt.yml")
  baseline = file("$rootDir/config/detekt/detekt-baseline.xml")
  buildUponDefaultConfig = true
  allRules = false
  parallel = true
  autoCorrect = false
  ignoreFailures = false  // Detekt failures break the build (D4)
  source.setFrom(
    "src/main/java",
    "src/test/java",
    "src/androidTest/java",
  )
  reports {
    sarif.required.set(true)
    html.required.set(true)
    xml.required.set(false)
    md.required.set(false)
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // D9.4 — FCM needs POST_NOTIFICATIONS on Android 13+. The
  // accompanist-permissions Compose helper is used inside
  // `AppRoot.NotificationPermissionEffect` to prompt the user once on
  // first composition. Uncommenting per the D9.4 plan.
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.windowsizeclass)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // D9.3 — Real Firebase Auth + Firestore. The `firebase-ai` artifact
  // (previously commented) is replaced by these two KTX modules
  // (BOM-managed so no version is needed). Anonymous auth via
  // `Firebase.auth.signInAnonymously()` is performed lazily by
  // `FirestoreRemoteDatabaseService` on first use.
  implementation(libs.firebase.firestore.ktx)
  implementation(libs.firebase.auth.ktx)
  // D9.4 — FCM push notifications. Pulls in `kotlinx-coroutines-play-services`
  // transitively, which gives us `Task<T>.await()` for the token fetch in
  // `MainActivity.fetchAndUploadFcmToken()`. The service is registered in
  // `AndroidManifest.xml` and the channel is created in
  // `MainActivity.onCreate` (both guarded by `BuildConfig.USE_FIREBASE`).
  implementation(libs.firebase.messaging.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)

  // Phase 5.3 — detekt formatting rules
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}

// D9.3 — apply the google-services plugin ONLY when
// `app/google-services.json` is present. The plugin reads the file at
// configuration time and synthesizes a `<package>.R` resource set; if
// the file is missing the build fails with a hard error. This
// conditional check lets a fresh clone (no JSON) still build and run
// against the in-memory `FirebaseDatabaseServiceImpl` for demo
// purposes. Drop a real `google-services.json` into `app/` to enable
// the Firestore backend — the `BuildConfig.USE_FIREBASE` flag in
// `buildTypes { ... }` then tells `ServiceLocator` to use the real
// impl in release builds.
val googleServicesJson = file("google-services.json")
if (googleServicesJson.exists()) {
  apply(plugin = libs.plugins.google.services.get().pluginId)
}

// H2 / Phase 7B-2 — Room schema export location, required by
// `AppDatabase.exportSchema = true`. The exported JSON files land
// in `app/schemas/` and are checked in so `MigrationTest` can boot
// a v2 schema in Robolectric and run `MIGRATION_2_3` against it.
ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}
