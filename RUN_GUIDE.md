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
& "C:\Users\Ahmad Mahmoud\AppData\AndroidCLI\android.exe" run --device=emulator-5554 --apks="e:\antigravity_projects\إلكترونك-سيتي\app\build\outputs\apk\debug\app-debug.apk"
```

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
