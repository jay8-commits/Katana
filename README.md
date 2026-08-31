# Device ID Randomization Lab

An educational laboratory Android project demonstrating how Android applications retrieve hardware and platform device identifiers, the security and privacy restrictions on modern Android versions, deterministic one-million test identity allocation, SQLite/Room persistence, and controlled runtime method interception.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Scope & Safety Boundaries](#scope--safety-boundaries)
3. [Technology Stack & Requirements](#technology-stack--requirements)
4. [Project Architecture](#project-architecture)
5. [Device Identifiers & Android Restrictions](#device-identifiers--android-restrictions)
6. [One-Million Identity Pool Engine](#one-million-identity-pool-engine)
7. [Persistence Architecture](#persistence-architecture)
8. [NPatch / Interception Layer Demonstration](#npatch--interception-layer-demonstration)
9. [Build & Installation Instructions](#build--installation-instructions)
10. [Unit & Integration Testing](#unit--integration-testing)
11. [GitHub Actions CI/CD](#github-actions-cicd)
12. [Troubleshooting](#troubleshooting)

---

## Project Overview

Modern Android operating systems enforce strict access controls on hardware identifiers (such as IMEI, MEID, and Serial Numbers) and provide scoped platform identifiers (such as `Settings.Secure.ANDROID_ID`).

**DeviceIdRandomizationLab** provides an interactive, hands-on environment to:
- Read real device identifiers via standard Android APIs and inspect runtime platform responses.
- Understand why hardware identifiers are restricted for non-system apps on Android 10+ (API 29+).
- Generate deterministic, cryptographically collision-resistant simulated test identities from a 1,000,000 identity pool.
- Guarantee that no identity is ever reused across restarts using Room database uniqueness constraints.
- Demonstrate how an instrumentation layer (e.g. NPatch / Xposed / Bytecode hook) substitutes test identifiers exclusively for the controlled test application (`com.example.deviceidlab`).

---

## Scope & Safety Boundaries

This repository is strictly an **educational test laboratory**:
- Interception hooks and simulated identity substitutions are **strictly scoped** to package `com.example.deviceidlab`.
- It does **not** modify third-party apps, system-wide settings, or evade security/licensing mechanisms.
- All real and simulated device identifiers remain strictly local to the device—no telemetry, network uploads, or analytics are used.

---

## Technology Stack & Requirements

- **Language:** Kotlin 2.2+
- **UI Framework:** Jetpack Compose with Material Design 3 (M3)
- **Database:** AndroidX Room with KSP (Kotlin Symbol Processing)
- **Target SDK:** 36 (Android 15+)
- **Minimum SDK:** 24 (Android 7.0 Nougat+)
- **Build System:** Gradle (Kotlin DSL - `build.gradle.kts`)
- **JDK:** Java 17
- **CI/CD:** GitHub Actions (`ubuntu-latest`)

---

## Project Architecture

```
DeviceIdRandomizationLab/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/deviceidlab/
│   │   │   │   ├── MainActivity.kt             # Material 3 UI & State Controller
│   │   │   │   ├── DeviceIdReader.kt           # Real Android & Telephony API Reader
│   │   │   │   ├── DeviceIdentityManager.kt    # 1,000,000 Pool Allocation & State
│   │   │   │   ├── RandomIdGenerator.kt        # Deterministic 16-hex & 15-digit generator
│   │   │   │   ├── database/
│   │   │   │   │   ├── AppDatabase.kt          # Room SQLite Database holder
│   │   │   │   │   ├── UsedIdentityDao.kt      # DAO with uniqueness & batch queries
│   │   │   │   │   └── UsedIdentityEntity.kt   # Entity with unique index constraint
│   │   │   │   └── model/
│   │   │   │       └── DeviceIdentity.kt       # Identity Data Model
│   │   │   └── res/                            # Vector Drawables, Strings, Themes
│   │   └── test/
│   │       └── java/com/example/deviceidlab/
│   │           ├── DeviceIdentityManagerTest.kt # Batch allocations & Lifecycle tests
│   │           ├── RandomIdGeneratorTest.kt     # Determinism & Format validation tests
│   │           └── DeviceIdHookDemoTest.kt      # Package safety & Hook tests
│   └── build.gradle.kts
│
├── demo-module/
│   ├── src/main/java/com/example/deviceidlab/demo/
│   │   ├── DeviceIdHookDemo.kt                  # Package-scoped method interceptor
│   │   ├── HookInvocationLog.kt                 # Interception event model
│   │   ├── InterceptionBridge.kt                # In-process thread-safe state bridge
│   │   └── NPatchAdapter.kt                     # Hooking framework integration adapter
│   └── build.gradle.kts
│
├── .github/workflows/
│   └── android.yml                             # Automated GitHub Actions build & test
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

---

## Device Identifiers & Android Restrictions

### 1. `Settings.Secure.ANDROID_ID`
- **API Call:** `Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)`
- **Format:** 64-bit number represented as a 16-character lowercase hexadecimal string (e.g. `91d04b7e8fa3c2d1`).
- **Behavior:** Since Android 8.0 (API 26), `ANDROID_ID` is scoped per application signing key, user, and device. Different apps signed with different keys receive different values on the same device.

### 2. `TelephonyManager.getDeviceId()` / `getImei()`
- **API Call:** `telephonyManager.imei` or `telephonyManager.deviceId`
- **Format:** 15 decimal digits (e.g. `384729105837421`).
- **Android 10+ Restriction:** Starting with Android 10 (API 29), access to non-resettable hardware identifiers (IMEI, MEID, serial numbers) is strictly restricted. Third-party applications lack the privileged system permission `READ_PRIVILEGED_PHONE_STATE`. Calling these methods throws a `SecurityException` or returns `null`.
- **Handling in Lab:** `DeviceIdReader.kt` catches `SecurityException` and API restrictions gracefully, reporting meaningful diagnostics such as `"Restricted (Android 10+ requires READ_PRIVILEGED_PHONE_STATE)"` without crashing.

---

## One-Million Identity Pool Engine

The lab manages a fixed pool of **1,000,000 unique identities** indexed from `1` to `1,000,000`.

### Selection Algorithm
1. **Secure Random Generation:** Uses `java.security.SecureRandom` to select a candidate index in `1..1,000,000`.
2. **Usage Check:** Verifies whether the index has already been allocated in SQLite via `UsedIdentityDao.isUsed(candidate)`.
3. **Reservation & Transaction:** Inserts the identity into the database. A unique index constraint on `identityNumber` prevents concurrent or duplicate allocations.
4. **Collision Handling & Fallback:** If collisions occur repeatedly or fewer than 10 identities remain, a deterministic search scans for the first available unused index in $O(N)$ with memory-backed hashing.
5. **Deterministic Identifier Derivation:**
   - **Android Test ID (16 hex):** Computed via SHA-256 HMAC of the identity index with a fixed salt, taking the first 8 bytes (16 hex characters).
   - **Telephony Test ID (15 digits):** Computed via SHA-256 digest of the index mapped into a 15-digit decimal range with a valid cellular TAC prefix.
   - For example, index `#482731` will *always* generate the exact same test identifiers, preserving identity state across restarts.
6. **Pool Exhaustion:** When all 1,000,000 identities have been allocated, the engine halts further generation and displays `ID POOL EXHAUSTED: All 1,000,000 test identities have been used.`

---

## Persistence Architecture

Room SQLite database persists identity state:
- Table: `used_identities`
- Columns: `id` (Auto Inc), `identityNumber` (Unique Index), `androidTestId`, `telephonyTestId`, `createdAt`.
- Upon application restart, the latest active identity and current pool usage count are reloaded into memory.
- The **Reset Database** feature safely clears `used_identities`, returning the pool usage to `0 / 1,000,000`.

---

## NPatch 1.0.7 / Xposed Module & Real-Device Validation Guide

This module can modify `ANDROID_ID` requests made through the hooked Java `Settings.Secure.getString()` API when the target application is running under a compatible NPatch 1.0.7 or Xposed/LSPosed instrumentation environment.

> **Compatibility Boundary:** Applications using native NDK/C direct reads of `/data/system/users/0/settings_ssaid.xml`, direct Binder IPC, or privileged telephony APIs require specialized system-level hooks and are not intercepted by standard Java-level `Settings.Secure` hooks.

### Real-Device Deployment Checklist (Non-Root NPatch 1.0.7)

1. **Module APK:** Build `app-debug.apk` (this application acts as both the management dashboard and the NPatch/Xposed module containing `NPatchHookEntry`).
2. **Target APK:** The application to test (e.g. `com.example.targetdemo` or `com.example.deviceidlab`).
3. **Module Installation:** Install `DeviceIdRandomizationLab` directly onto the device via `adb install -r app-debug.apk`.
4. **Target Patching:**
   - Open **NPatch 1.0.7** on the device.
   - Tap `+` (Patch Application) and select the target APK file or installed target app.
   - Under **Embed Modules**, select `DeviceIdRandomizationLab`.
   - Tap **Start Patching**.
5. **Install Patched Target:**
   - NPatch outputs the patched APK to `/sdcard/NPatch/` (or internal storage).
   - If the target APK was previously installed from Google Play or standard debug build, uninstall the original target first due to signature differences.
   - Install the generated `*-patched.apk`.
6. **Configure & Inject:**
   - Open `DeviceIdRandomizationLab`.
   - In the **NPatch 1.0.7 Hook Injection Test** card, enter the desired 16-hex Android ID and tap **SAVE INJECT ID**.
7. **Process Restart & Verification:**
   - Force-stop / completely close the target app.
   - Launch the patched target app.
   - In `DeviceIdRandomizationLab`, tap **TEST ID INJECTION** or observe the target app displaying the injected ID directly.

### LSPosed / Rooted Environments Checklist

1. Install `DeviceIdRandomizationLab` APK on the rooted device.
2. Open **LSPosed Manager** $\rightarrow$ **Modules** $\rightarrow$ Enable `DeviceIdRandomizationLab`.
3. Set the **Scope** to include your target applications.
4. Reboot the device or restart system server.
5. In `DeviceIdRandomizationLab`, configure the injected ID.
6. Force-stop and restart the target application.

---

## Build & Installation Instructions

### Build Command

To assemble the debug APK on Linux/macOS/Windows:

```bash
./gradlew assembleDebug
```

For Windows PowerShell / Command Prompt:
```cmd
gradlew.bat assembleDebug
```

### Generated Artifact Location

```
app/build/outputs/apk/debug/app-debug.apk
```

### Installation onto Device or Emulator

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Unit & Integration Testing

Run the full local JVM test suite (including Robolectric database tests):

```bash
./gradlew testDebugUnitTest
```

### Test Suite Highlights
- `RandomIdGeneratorTest`: Verifies 16-hex and 15-digit formats, deterministic repeatability, and distinct output generation.
- `DeviceIdentityManagerTest`:
  - 100 consecutive allocations without duplicates.
  - 1,000 consecutive allocations without duplicates.
  - 10,000 consecutive allocations without duplicates.
  - Application restart state persistence and recovery.
  - Database reset functionality.
  - Hard pool limit exhaustion and boundary handling.
- `DeviceIdHookDemoTest`: Verifies strict package scoping, parameter matching, and pass-through semantics.

---

## GitHub Actions CI/CD

The workflow in `.github/workflows/android.yml` automatically:
1. Triggers on `push` and `pull_request` to `main`/`master`.
2. Sets up JDK 17 with Temurin distribution.
3. Configures the Android SDK and Gradle build environment.
4. Executes `./gradlew testDebugUnitTest`.
5. Compiles `./gradlew assembleDebug`.
6. Uploads the build artifact `device-id-randomization-debug` (`app-debug.apk`).

---

## Troubleshooting

- **Telephony Device ID shows "Restricted":**
  This is expected on Android 10+ (API 29+) as Google restricted hardware IMEI access to privileged system apps. The lab gracefully handles this restriction and demonstrates how test environments substitute identifiers.
- **Gradle permission denied on Linux:**
  Run `chmod +x gradlew` before executing build commands.
- **Database Reset:**
  Use the "RESET TEST DATABASE" button in the app to clear all allocated identities and restart from zero used identities.
