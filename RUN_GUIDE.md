# Setup & Run Guide: إلكترونك سيتي (Electronic City)

This guide provides step-by-step instructions to compile, deploy, and troubleshoot the **Electronic City** Android application starting from a fresh environment or a new agent session.

---

## 🚀 Quick Start Steps

### 1. Set Up Environment Variables
Ensure the local Android SDK paths are correctly set in your session. The active SDK resides in the user's AppData directory:
```powershell
$env:ANDROID_SDK_ROOT = "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk"
$env:ANDROID_HOME = "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk"
```

### 2. Verify or Install Android CLI
If the `android` command-line tool is not recognized, install it using the command prompt:
```cmd
curl.exe -fsSL https://dl.google.com/android/cli/latest/windows_x86_64/install.cmd -o "%TEMP%\i.cmd" && "%TEMP%\i.cmd"
```
The binary will be installed to: `C:\Users\Ahmad Mahmoud\AppData\AndroidCLI\android.exe`.

### 3. Start the Emulator (Cold Boot)
If the emulator fails to start or encounters snapshot errors, bypass snapshot loading using:
```powershell
& "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Small_Phone -no-snapshot-load
```
Wait for `adb devices` to report the emulator as `device` and `adb shell getprop sys.boot_completed` to return `1`.

### 4. Build the App
Run the Gradle assembly build using the locally downloaded Gradle 9.3.1 toolchain:
```powershell
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" assembleDebug
```

### 5. Deploy and Launch
Install the generated APK onto the active emulator:
```powershell
& "C:\Users\Ahmad Mahmoud\AppData\Local\AndroidCLI\android.exe" run --device=emulator-5554 --apks="e:\antigravity_projects\إلكترونك-سيتي\app\build\outputs\apk\debug\app-debug.apk"
```

### 6. Run Static Analysis (Phase 7B)
The build is wired with detekt + Android Lint. Both must pass cleanly:
```powershell
# detekt (style, complexity, potential bugs; warnings break the build)
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" detekt

# Android Lint (warnings as errors for release builds)
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" lint
```
See `config/detekt/detekt.yml` for the project-tuned rule set.

### 7. Build a Release APK (Phase 7B)
Release builds enable R8 minification + resource shrinking. The signing
config is **fail-loud** — the build aborts if any of the required env
vars are missing (no silent debug-signed fallback).
```powershell
$env:KEYSTORE_PATH   = "C:\path\to\your.jks"
$env:STORE_PASSWORD = "your-store-password"
$env:KEY_PASSWORD    = "your-key-password"
$env:KEY_ALIAS       = "your-alias"   # optional, defaults to "key0"
& "e:\antigravity_projects\إلكترونك-سيتي\.gradle_dist\gradle-9.3.1\bin\gradle.bat" assembleRelease
```
The minified APK lands at:
`e:\antigravity_projects\إلكترونك-سيتي\app\build\outputs\apk\release\app-release.apk`

---

## 🛠️ Troubleshooting Common Issues

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
* **Symptoms**: `./gradlew assembleRelease` (or `gradle.bat assembleRelease`) fails with: `*** KEYSTORE_PATH env var is required for release builds`.
* **Resolution**: Set all four env vars before invoking the build — see Step 7 above. There is no silent fallback; this is intentional (D8.6).

### ⚠️ Issue: detekt / lint fails the build
* **Symptoms**: `gradle detekt` reports >0 issues, or `gradle assembleRelease` fails on the `lint` task.
* **Resolution**: Read the rule's message and fix the source. For detekt rule thresholds see `config/detekt/detekt.yml`. For lint, the baseline is `app/lint-baseline.xml` (currently empty). Don't grow the baseline; fix the warning.

### ⚠️ Issue: Build fails on missing `google-services.json` (D9.3)
* **Symptoms**: `gradle assembleDebug` (or `assembleRelease`) fails with: `File google-services.json is missing. (...)` emitted by the `com.google.gms.google-services` Gradle plugin.
* **Resolution (fresh clone — no Firebase project)**: delete the plugin apply call at the bottom of `app/build.gradle.kts`, OR keep the file present and let the `if (file("google-services.json").exists())` guard in `app/build.gradle.kts` skip plugin application. The build will succeed and use the in-memory `FirebaseDatabaseServiceImpl` (the demo "Toggle connectivity" admin action works). `BuildConfig.USE_FIREBASE` is `false` in debug and `true` in release, so the impl is selected automatically.
* **Resolution (Firebase project available)**: drop a real `google-services.json` (downloaded from the Firebase console for the `com.aistudio.arabianshop.ecity` applicationId) into `app/`. The plugin auto-applies, `BuildConfig.USE_FIREBASE` flips to `true`, and `ServiceLocator.getRemoteService(...)` returns the `FirestoreRemoteDatabaseService` (currently a stub — see its class doc for what's pending). The file is gitignored (line 22 of `.gitignore`).
* **D9.3 not yet complete**: `FirestoreRemoteDatabaseService` methods are stub `TODO` bodies. They return `emptyList()` / `false` so the build succeeds, but the app will appear empty when wired to the real backend. Replace each `TODO` with the real Firestore call once the schema is locked.
