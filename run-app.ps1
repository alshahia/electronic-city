# run-app.ps1
# Build, install, and launch Electronic City on the local Android emulator.
# Idempotent: skips emulator start if a device is already attached; uses
# incremental Gradle build; install -r is fast on no-change.
#
# Usage (from any directory):
#   powershell -NoProfile -ExecutionPolicy Bypass -File "run-app.ps1"
# Or double-click run-app.bat in the project root.
#
# The script uses $PSScriptRoot, so it does not need a hardcoded
# project path (the project root contains the Arabic directory name).
# Edit $AndroidSdk below if your SDK lives elsewhere.

$ErrorActionPreference = "Continue"

# --- Config (edit $AndroidSdk here if your SDK is elsewhere) ---
$ProjectRoot = $PSScriptRoot
$AndroidSdk  = "C:\Users\Ahmad Mahmoud\AppData\Local\Android\Sdk"
$AvdName     = "Small_Phone"
$AppId       = "com.aistudio.arabianshop.ecity"
$Adb         = Join-Path $AndroidSdk "platform-tools\adb.exe"
$EmulatorExe = Join-Path $AndroidSdk "emulator\emulator.exe"
$Gradle      = Join-Path $ProjectRoot ".gradle_dist\gradle-9.3.1\bin\gradle.bat"
$Apk         = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"

# --- Sanity checks ---
if (-not (Test-Path $Gradle))      { throw "Gradle not found: $Gradle (see RUN_GUIDE.md Step 3)" }
if (-not (Test-Path $Adb))         { throw "adb not found: $Adb (is ANDROID_SDK_ROOT correct?)" }
if (-not (Test-Path $EmulatorExe)) { throw "Emulator not found: $EmulatorExe" }

# --- Override the broken system ANDROID_HOME ---
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:ANDROID_HOME     = $AndroidSdk

# --- Signing env vars (D8.6 fail-loud is checked at config time, so
#     even assembleDebug needs them set; default to the debug keystore) ---
if (-not $env:KEYSTORE_PATH) {
    $debugKs = Join-Path $ProjectRoot "debug.keystore"
    if (Test-Path $debugKs) {
        $env:KEYSTORE_PATH   = $debugKs
        $env:STORE_PASSWORD  = "android"
        $env:KEY_PASSWORD    = "android"
        $env:KEY_ALIAS       = "androiddebugkey"
    }
}

Set-Location $ProjectRoot

function Get-AttachedDevice {
    $line = (& $Adb devices 2>$null | Where-Object { $_ -match '^\S+\s+device\s*$' } | Select-Object -First 1)
    if ($line) { ($line -split '\s+')[0] } else { $null }
}

# --- 1. Ensure a device is attached ---
$Device = Get-AttachedDevice
if ($Device) {
    Write-Host "[1/4] Device already attached: $Device" -ForegroundColor Green
} else {
    Write-Host "[1/4] No device found. Starting emulator '$AvdName' (cold boot)..." -ForegroundColor Yellow
    Start-Process -FilePath $EmulatorExe -ArgumentList "-avd $AvdName -no-snapshot-load"
    & $Adb start-server 2>$null | Out-Null
    & $Adb wait-for-device 2>$null | Out-Null
    Write-Host "       Waiting for boot_completed=1..." -NoNewline
    while ($true) {
        $val = & $Adb shell getprop sys.boot_completed 2>$null
        if ($val -match '^1$') { break }
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }
    Write-Host ""
    $Device = Get-AttachedDevice
    if (-not $Device) { throw "Emulator started but no device is in 'device' state. Check 'adb devices'." }
    Write-Host "       Boot complete on $Device." -ForegroundColor Green
}

# --- 2. Build (incremental - fast on no-op) ---
Write-Host "[2/4] Building app-debug APK..." -ForegroundColor Yellow
$buildOutput = & $Gradle assembleDebug --no-daemon 2>&1
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $Apk)) {
    Write-Host $buildOutput
    throw "Build failed (exit $LASTEXITCODE). See output above."
}
$apkSize = [math]::Round((Get-Item $Apk).Length / 1MB, 1)
Write-Host "       APK built: $apkSize MB" -ForegroundColor Green

# --- 3. Install ---
Write-Host "[3/4] Installing $AppId on $Device..." -ForegroundColor Yellow
& $Adb -s $Device install -r $Apk 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Install failed." }
Write-Host "       OK." -ForegroundColor Green

# --- 4. Launch ---
Write-Host "[4/4] Launching $AppId..." -ForegroundColor Yellow
& $Adb -s $Device shell monkey -p $AppId -c android.intent.category.LAUNCHER 1 2>$null | Out-Null
Write-Host ""
Write-Host "Done. App is running on $Device." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:"
Write-Host "  - Smoke test:  see RUN_GUIDE.md Step 7"
Write-Host "  - Logcat:      & '$Adb' -s $Device logcat -d -t 200 *:E"
Write-Host "  - Re-run:      re-run this script (idempotent, fast incremental build)"
