# Physical Device First Test Guide (Settings.Secure.ANDROID_ID)

> **CRITICAL TRANSPARENCY STATEMENT**:  
> **PHYSICAL DEVICE RUNTIME VERIFICATION: NOT PERFORMED (AWAITING PHYSICAL HARDWARE TEST)**  
> This protocol specifies the exact instructions for executing the first real physical-device test on an Android smartphone.

---

## 1. Phone-Side Requirements & Environment

An unmodified, stock Android phone **cannot** load an Xposed/LSPosed hook module without a specialized runtime environment.

### Required Environment on Physical Phone:
1. **Rooted Android Device** (Magisk / KernelSU / APatch) OR **NPatch sandboxed APK container**.
2. **LSPosed Framework** (Zygisk version recommended) installed and showing **"Active" / Green checkmark** in LSPosed Manager.
3. **Android Version**: Android 8.0 (API 26) through Android 14 (API 34).
4. **USB Debugging**: Enabled with `adb` authorized on test PC.

---

## 2. Module & Target Packages Inventory

| Module | Gradle Module | Package Name | Role | Install on Phone? | Add to LSPosed Scope? |
| :--- | :--- | :--- | :--- | :---: | :---: |
| **Controller & Hook Provider** | `:app` | `com.example.deviceidlab` | Generates Profile & Hosts ContentProvider & Module Dex | **YES** | **NO (EXCLUDE)** |
| **Target Demo #1** | `:demo-module` | `com.example.demomodule` | Target app independently calling `Settings.Secure` | **YES** | **YES (INCLUDE)** |
| **Target Demo #2** | `:second-target-app` | `com.example.secondtargetapp` | Second target testing cross-process isolation | **YES** | **YES (INCLUDE)** |

---

## 3. Step-by-Step First Physical-Device Test Procedure

### Step 1: Install APKs
```bash
# Install the Controller & Hook Module APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install Target Application #1
adb install -r demo-module/build/outputs/apk/debug/demo-module-debug.apk

# Install Target Application #2
adb install -r second-target-app/build/outputs/apk/debug/second-target-app-debug.apk
```

### Step 2: Configure LSPosed Module Scope
1. Open **LSPosed Manager** on your phone.
2. Locate module: **DeviceIdRandomizationLab** (`com.example.deviceidlab`).
3. Turn module toggle to **ON**.
4. Check **Scope Selection**:
   - `[X] com.example.demomodule` (Target Demo #1)
   - `[X] com.example.secondtargetapp` (Target Demo #2)
   - `[ ] com.example.deviceidlab` (**DO NOT CHECK THE CONTROLLER**)
5. Force-stop target if running:
   ```bash
   adb shell am force-stop com.example.demomodule
   ```

### Step 3: Activate Profile #1 in Controller
1. Open **Device ID Lab** (`com.example.deviceidlab`) on the phone.
2. Tap **"Set Profile #1 (Pixel 7)"**.
3. Verify on screen:
   - Profile #1 Android ID: `a1b2c3d4e5f60718` (Masked: `a1...18 (e7d9b2)`)

### Step 4: Launch Logcat Monitor on PC
Run in terminal:
```bash
adb logcat -c && adb logcat -s NPatch TargetDemo1
```

### Step 5: Launch Target Demo #1 & Execute First Test
1. Launch **Target Demo #1** (`com.example.demomodule`).
2. Tap **"Run Android ID Audit & Full Verification"**.
3. Observe the top focus card:
   - **TARGET APP**: `com.example.demomodule`
   - **EXPECTED PROFILE #1**: `a1...18 (e7d9b2)`
   - **ACTUAL VALUE OBSERVED**: `a1...18 (e7d9b2)`
   - **STATUS**: `PASS`
   - **DIAGNOSIS**: `GENERATED_VALUE_OBSERVED`

---

## 4. Diagnostic Logcat Evidence Format

### Expected Logcat Event Stream (EXPECTED EXAMPLE):
```text
[NPatch] EVENT: TARGET_PROCESS_STARTED | Package: com.example.demomodule | Process: com.example.demomodule
[NPatch] EVENT: HOOK_REGISTERED | Hook: Settings.Secure.getString(ContentResolver, String)
[NPatch] EVENT: API_INVOCATION_INTERCEPTED | API: Settings.Secure.getString(ANDROID_ID) | Target: com.example.demomodule
[NPatch] EVENT: PROFILE_LOOKUP_SUCCESS | Key: androidId | Val: a1...18 (e7d9b2)
[NPatch] EVENT: VALUE_REPLACED | API: ANDROID_ID | Target: com.example.demomodule | Orig: 4c...a9 (1f2a3c) | Replaced: a1...18 (e7d9b2)
[TargetDemo1] EVENT: TARGET_VERIFICATION_RESULT | API: 1. Settings.Secure.getString(ANDROID_ID) | Status: PASS | Target: com.example.demomodule | Val: a1...18 (e7d9b2)
```

---

## 5. Granular Failure Diagnostics Matrix

If the test does not show `PASS`, use this table to immediately isolate the root cause:

| Observed Status | In-App Diagnosis | Logcat Pattern | Root Cause & Resolution |
| :--- | :--- | :--- | :--- |
| **`FAIL`** | `ORIGINAL_VALUE_OBSERVED` | No `[NPatch]` logs appear in Logcat | **LSPosed did not load module into target**. Verify LSPosed is active and target package is checked in scope. Force-stop target and re-launch. |
| **`FAIL`** | `ORIGINAL_VALUE_OBSERVED` | `HOOK_REGISTERED` appears, but no `API_INVOCATION_INTERCEPTED` | Hook was registered, but target did not trigger the hooked method signature. |
| **`FAIL`** | `PROFILE_LOOKUP_FAILED` | `API_INVOCATION_INTERCEPTED` appears, followed by `PROFILE_LOOKUP_FAILED` | Target cannot query `DeviceIdProvider`. Check if controller app was installed, or if permissions/SELinux blocked Provider IPC. |
| **`FAIL`** | `ORIGINAL_VALUE_NULL` | Target observed `null` | Framework returned `null` and hook was bypassed. |
| **`PASS`** | `GENERATED_VALUE_OBSERVED` | Full sequence: `INTERCEPTED` $\rightarrow$ `LOOKUP_SUCCESS` $\rightarrow$ `VALUE_REPLACED` $\rightarrow$ `TARGET_VERIFICATION_RESULT` | **Interception succeeded on real hardware.** |
