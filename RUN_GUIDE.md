# Setup & Run Guide: إلكترونك سيتي (Electronic City)

This guide provides step-by-step instructions to compile, deploy, and troubleshoot the **Electronic City** Android application starting from a fresh environment or a new agent session.

> **Note:** The `applicationId` in `app/build.gradle.kts` is **`com.aistudio.arabianshop.ecity`**. The internal source package `com.example` is **not** the install/launch id — `adb` commands that target `com.example` will fail with `Activity class does not exist`.

---
## 🚀 Quick Start Steps

### 1. Set Up Environment Variables
Ensure the local Android SDK paths are correctly set in your session. The active SDK resides in the user's AppData directory:
```powershell
$env:ANDROID_SDK_ROOT = "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk"
$env:ANDROID_HOME = "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk"
```
The system-level `ANDROID_HOME` may be set to a non-writable path (`C:\Users\andriod\Sdk`, lowercase). This override is required for both Gradle and the emulator. For release builds, also set the four signing env vars described in Step 6.

### 2. Start the Emulator (Cold Boot)
If the emulator fails to start or encounters snapshot errors, bypass snapshot loading using:
```powershell
& "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Small_Phone -no-snapshot-load
```
Wait for `adb devices` to report the emulator as `device` and `adb shell getprop sys.boot_completed` to return `1`.

### 3. Build the App
Run the Gradle assembly build using the locally downloaded Gradle 9.3.1 toolchain:
```powershell
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" assembleDebug
```

### 4. Deploy and Launch
Install the generated APK onto the active emulator and launch the launcher activity. Do not use `com.example` — the install path is the `applicationId` (see header note).
```powershell
$adb = "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Install (or re-install with -r)
& $adb -s emulator-5554 install -r "e:\antigravity_projects\إلكترونك-سيتي\app\build\outputs\apk\debug\app-debug.apk"

# Launch via monkey (resolves LAUNCHER intent without knowing the activity class)
& $adb -s emulator-5554 shell monkey -p com.aistudio.arabianshop.ecity -c android.intent.category.LAUNCHER 1
```
Expected logcat line: `Displayed com.aistudio.arabianshop.ecity/com.example.MainActivity for user 0: <time>ms` with no `FATAL EXCEPTION`.

### 5. Run Static Analysis
The build is wired with detekt + Android Lint. Both must pass cleanly:
```powershell
# detekt (style, complexity, potential bugs; warnings break the build)
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" detekt

# Android Lint (warnings as errors for release builds)
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" lint
```
See `config/detekt/detekt.yml` for the project-tuned rule set.

### 6. Build a Release APK
Release builds enable R8 minification + resource shrinking. The signing config is **fail-loud** — the build aborts if any of the required env vars are missing (no silent debug-signed fallback). The four env vars ([D8.6](./DECISIONS.md)) must be set in the same session that runs the build:
```powershell
$env:KEYSTORE_PATH   = "C:\path\to\your.jks"
$env:STORE_PASSWORD = "your-store-password"
$env:KEY_PASSWORD    = "your-key-password"
$env:KEY_ALIAS       = "your-alias"   # optional, defaults to "key0"
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" assembleRelease
```
The minified APK lands at:
`e:\antigravity_projects\إلكترونك-سيتي\app\build\outputs\apk\release\app-release.apk`

### 7. Smoke Test (manual)
After `assembleDebug` + install + launch, exercise the core flows to confirm the app is functional:
1. **Home** loads with the product carousel; pull-to-refresh works.
2. **Products** tab lists 8 items; tap one to open detail.
3. **Add to cart** from detail; the badge in the bottom bar increments.
4. **Cart** screen shows the item with the correct total price; qty +/− works.
5. **Account** → toggle **Language** to Arabic; layout flips to RTL; toggle back to English.
6. **Favorites** and **Search** empty states show the four art drawables (no theme-attr crash).
7. **Theme** switch: Account → Theme → Dark; app re-themes without crash.

If any step crashes, capture the slice with `& $adb -s emulator-5554 logcat -d -t 200 *:E` and check the troubleshooting section below.

---
## 🛠️ Troubleshooting Common Issues

### ⚠️ Issue: Kotlin compile errors during `assembleDebug`
* **Symptoms**: `e: file://...kt:N:1 ...` lines on the `compileDebugKotlin` task, build fails.
* **Resolution**: The repo was last verified to build green on commit **`998b163`** ("fix: build green on assembleDebug"). That commit's message is the canonical list of every compile-time patch applied: pinned `firebase-{firestore,auth,messaging}-ktx` versions (BOM 34.12.0 does not manage them), removed `?attr/colorControlNormal` from 4 empty-state drawables, added a `rememberHapticClick(onClick)` overload, fixed `order.X` → `order.order.X` in `OrderItemCard` (`@Embedded` does not flatten fields in Kotlin KSP), added a `MainActivity` cast for `setAppLocale`, added Coil imports to `CartScreen.kt`, stubbed Firebase refs and added the `requireAdmin` override in `FirestoreRemoteDatabaseService.kt`, and added `@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)` on `MainActivity.onCreate` and `DefaultWindowSizeClass`. If your working tree has diverged from `998b163`, rebase or cherry-pick it.

### ⚠️ Issue: Non-ASCII Character Path Warning
* **Symptoms**: Gradle compilation fails with: `Your project path contains non-ASCII characters...`
* **Resolution**: Ensure `android.overridePathCheck=true` is present in [gradle.properties](file:///e:/antigravity_projects/إلكترونك-سيتي/gradle.properties):
  ```properties
  android.overridePathCheck=true
  ```

### ⚠️ Issue: Missing `debug.keystore`
* **Symptoms**: Build fails on `:app:validateSigningDebug` with error: `Keystore file .../debug.keystore not found`.
* **Resolution**: Copy the system-generated debug keystore to the root directory of the project:
  ```powershell
  Copy-Item -Path "C:\Users\Ahmad Mahmoud\.android\debug.keystore" -Destination "e:\antigravity_projects\إلكترونك-سيتي\debug.keystore"
  ```

### ⚠️ Issue: Wrong Gradle Version Check
* **Symptoms**: Error message: `Minimum supported Gradle version is 9.3.1. Current version is 8.14.3.`
* **Resolution**: Do not use the globally installed Gradle. Use the downloaded binary located at:
  `e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat`

### ⚠️ Issue: Broken AVD System Path (Wrong ANDROID_SDK_ROOT)
* **Symptoms**: Emulator log shows `Broken AVD system path. Check your ANDROID_SDK_ROOT [C:\Users\andriod\Sdk]`.
* **Resolution**: Override the environment variable in your terminal session before starting the emulator:
  ```powershell
  $env:ANDROID_SDK_ROOT = 'C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk'
  ```

### ⚠️ Issue: Release build aborts with "Keystore ... not set"
* **Symptoms**: `gradle.bat assembleRelease` fails with: `*** KEYSTORE_PATH env var is required for release builds`.
* **Resolution**: Set all four env vars before invoking the build — see Step 6 above. There is no silent fallback; this is intentional ([D8.6](./DECISIONS.md)).

### ⚠️ Issue: detekt / lint fails the build
* **Symptoms**: `gradle detekt` reports >0 issues, or `gradle assembleRelease` fails on the `lint` task.
* **Resolution**: Read the rule's message and fix the source. For detekt rule thresholds see `config/detekt/detekt.yml`. For lint, the baseline is `app/lint-baseline.xml` (currently empty). Don't grow the baseline; fix the warning.

### ⚠️ Issue: Build fails on missing `google-services.json` ([D9.3](./DECISIONS.md))
* **Symptoms**: `gradle assembleDebug` (or `assembleRelease`) fails with: `File google-services.json is missing. (...)` emitted by the `com.google.gms.google-services` Gradle plugin.
* **Resolution (fresh clone — no Firebase project)**: delete the plugin apply call at the bottom of `app/build.gradle.kts`, OR keep the file present and let the `if (file("google-services.json").exists())` guard in `app/build.gradle.kts` skip plugin application. The build will succeed and use the in-memory `FirebaseDatabaseServiceImpl` (the demo "Toggle connectivity" admin action works). `BuildConfig.USE_FIREBASE` is `false` in debug and `true` in release, so the impl is selected automatically.
* **Resolution (Firebase project available)**: drop a real `google-services.json` (downloaded from the Firebase console for the `com.aistudio.arabianshop.ecity` applicationId) into `app/`. The plugin auto-applies, `BuildConfig.USE_FIREBASE` flips to `true`, and `ServiceLocator.getRemoteService(...)` returns the `FirestoreRemoteDatabaseService` (currently a stub — see its class doc for what's pending). The file is gitignored (line 22 of `.gitignore`).
* **D9.3 not yet complete** ([D9.3](./DECISIONS.md)): `FirestoreRemoteDatabaseService` methods are stub `TODO` bodies. They return `emptyList()` / `false` so the build succeeds, but the app will appear empty when wired to the real backend. Replace each `TODO` with the real Firestore call once the schema is locked.
