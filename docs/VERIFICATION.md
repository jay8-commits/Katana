# Verification Framework & Truthful Testing Model

## 1. Five-Tier Verification Hierarchy

To prevent false assertions of runtime interception, this project strictly distinguishes between testing tiers:

```
┌────────────────────────────────────────────────────────┐
│ TIER A: SOURCE & CODE INSPECTION                       │
│    - Bytecode & Hook signature matching in Xposed API  │
│    - TestApiCatalog registration (21 APIs)             │
│    - Status: VERIFIED                                  │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ TIER B: UNIT TESTS                                     │
│    - Local JVM unit tests (DeviceIdentityManagerTest)  │
│    - Profile generation, Luhn algorithm, data models   │
│    - Status: PASS                                      │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ TIER C: BUILD VERIFICATION                             │
│    - Gradle compilation across all three modules       │
│    - Output APK generation                             │
│    - Status: PASS                                      │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ TIER D: EMULATOR VERIFICATION                          │
│    - Executed in headless / virtualized AVD            │
│    - Status: NOT PERFORMED IN CLOUD BUILD CONTAINER    │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ TIER E: PHYSICAL ANDROID DEVICE RUNTIME VERIFICATION   │
│    - Requires hardware device running NPatch / LSPosed │
│    - Measures active Binder IPC and hook interception  │
│    - Status: NOT PERFORMED (Awaiting physical device)  │
└────────────────────────────────────────────────────────┘
```

> **CRITICAL SCIENTIFIC INTEGRITY STATEMENT**:
> **PHYSICAL DEVICE RUNTIME VERIFICATION: NOT PERFORMED**
> A static catalog entry, code inspection, or unit test pass does NOT constitute physical runtime proof. Runtime proof requires running the APKs on an actual Android device equipped with an active Xposed/LSPosed/NPatch framework.

---

## 2. In-App Runtime Verification Dashboard

Both target applications (`:demo-module` and `:second-target-app`) contain an in-app audit dashboard. The target application's own code invokes the framework APIs directly.

### Item Audit Layout
```text
═════════════════════════════════════════════════════════
API: Settings.Secure.getString(ANDROID_ID)
METHOD/FIELD: Settings.Secure.getString(ContentResolver, ANDROID_ID)
TARGET PROCESS: com.example.demomodule (PID: 28412)
HOOK EVENT: Settings.Secure.getString(ContentResolver, String)
EXPECTED PROFILE VALUE (MASKED): a1...18 (e7d9b2)
ACTUAL OBSERVED VALUE  (MASKED): a1...18 (e7d9b2)
VALUE MATCH: YES
RESULT STATUS: PASS
═════════════════════════════════════════════════════════
```

---

## 3. Strict Result Status Classification

| Status | Exact Definition |
| :--- | :--- |
| **`PASS`** | Target process executed the framework API and verified that the returned value matched the expected generated profile value. |
| **`FAIL`** | Target process ran the API but observed the original hardware value or unexpected output. |
| **`HOOK_NOT_REGISTERED`** | Hook was not installed during module initialization. |
| **`HOOK_NOT_EXECUTED`** | Hook exists in bytecode but runtime execution never traversed the hook point. |
| **`PROFILE_LOOKUP_FAILED`** | Interception occurred, but DeviceIdProvider IPC resolution failed. |
| **`PLATFORM_RESTRICTED`** | Android OS policy legitimately prevents non-privileged apps from querying the API (e.g., Telephony restrictions on Android 10+, `Build.getSerial()` on API 28+). |
| **`NOT_TESTED`** | Runtime test has not yet been executed on physical hardware. |

---

## 4. Deterministic Profile #1 vs Profile #2 Verification Sequence

```
1. Generate Profile #1 (Pixel 7)
   ├── Record Fingerprint/Hash: SHA-256 of Profile #1
   ├── Target #1 launches -> reads framework APIs
   └── Record Target Observation #1

2. Generate Profile #2 (Galaxy S23)
   ├── Record Fingerprint/Hash: SHA-256 of Profile #2 (Profile #1 ≠ Profile #2)
   ├── Dynamic APIs (Settings.Secure, WifiInfo) re-read immediately
   ├── Static Fields (Build.*) re-read after process restart
   └── Record Target Observation #2

3. Verification Assertions:
   ├── Target Observation #1 == Profile #1
   └── Target Observation #2 == Profile #2
```

---

## 5. Multi-Package Independence Verification

Demonstrates **Patch Once → Install Once → Multiple Independent Targets**:

```
PATCH ONCE (Xposed/NPatch module active)
   ├── TARGET #1 (com.example.demomodule)      -> reads active profile via ContentProvider IPC
   └── TARGET #2 (com.example.secondtargetapp) -> reads active profile via ContentProvider IPC
```

Both target applications query `DeviceIdProvider` independently and receive the exact same active profile without modifying target source code or requiring separate patches.

---

## 6. Diagnostic Logging Contract (Expected Format vs Actual Device Log)

### Expected Log Structure
```
[NPatch] EVENT: TARGET_PROCESS_STARTED | Package: <target_pkg> | Process: <process_name>
[NPatch] EVENT: HOOK_REGISTERED | Hook: <hook_name>
[NPatch] EVENT: API_INVOCATION_INTERCEPTED | API: <api_name> | Target: <target_pkg>
[NPatch] EVENT: PROFILE_LOOKUP_SUCCESS | Key: <key> | Val: <masked_val>
[NPatch] EVENT: VALUE_REPLACED | API: <api_name> | Target: <target_pkg> | Orig: <masked_orig> | Replaced: <masked_spoofed>
[TargetApp] EVENT: TARGET_VERIFICATION_RESULT | API: <api_name> | Status: <status> | Target: <target_pkg> | Val: <masked_val>
```

> **Note**: Logs are only classified as **ACTUAL DEVICE LOGS** when captured via `adb logcat` from an active hardware test session.

---

## 7. Location Subsystem Verification Model

### Distinct Verification Stages
1. **`HOOK_REGISTERED`**: Verified at module initialization (`installLocationHooks`).
2. **`HOOK_INVOKED`**: Verified when target application executes `LocationManager.getLastKnownLocation(provider)` or `isProviderEnabled(provider)`.
3. **`VALUE_GENERATED`**: Verified when active `LocationProfile` is provisioned via `DeviceIdProvider` IPC (`NPatchConfig.LOCATION_PROVIDER_URI`).
4. **`VALUE_RETURNED`**: Verified when synthetic `Location` POJO is constructed and returned via `param.result`.
5. **`TARGET_OBSERVED`**: Verified when target activity (`TargetDemoActivity` or `SecondTargetActivity`) receives the location object and verifies coordinates within tolerance (`delta < 1e-5`).

### Permission & Diagnostic Status Classification
- **`PERMISSION_GRANTED`**: Target app has fine or coarse location permission.
- **`PERMISSION_DENIED`**: Target app lacks permission (distinguished from hook failure).
- **`LOCATION_DISABLED`**: System location providers disabled.
- **`PROVIDER_UNAVAILABLE`**: System `LocationManager` is null or inaccessible.
- **`HOOK_NOT_INVOKED`**: Framework method returned un-intercepted null.
- **`HOOK_INVOKED`**: Method hook fired and intercepted request.
- **`EXPECTED_LOCATION_OBSERVED`**: Synthetic location matching active profile observed by target app.

> **Status in Current Container Environment**: `NOT_PERFORMED` (Awaiting physical Android device with NPatch/LSPosed runtime).

---

## 8. Worldwide Geographic & Synthetic IP Verification Model

### Verification Matrix

| Subsystem Component | Verification Level | Status in Container | Hardware Criterion |
| :--- | :--- | :--- | :--- |
| **80+ World City Catalog** | Data / Schema | **PASS** (15/15 unit tests) | Valid coordinates within `[-90, 90]` and `[-180, 180]`, valid IANA timezones. |
| **Mode 1: Manual City Selection** | Controller & IPC | **VERIFIED** | Correct city record mapped, coordinates locked to catalog. |
| **Mode 2: Random World City** | Controller & IPC | **VERIFIED** | Uniform random selection from 80+ cities, unique RFC 5737 IP. |
| **Mode 3: Random City in Country** | Controller & IPC | **VERIFIED** | Filtered selection strictly within selected sovereign country. |
| **RFC 5737 Synthetic Test IP** | Security / RFC | **VERIFIED** | Deterministic allocation within `203.0.113.0/24` TEST-NET-3. |
| **Profile Consistency Check** | Target App UI Audit | **PASS** | `TargetDemoActivity` confirms City ↔ Country ↔ Lat/Lng ↔ Timezone ↔ IP coherence. |
| **IP Scope Boundary** | Architecture / Network | **DOCUMENTED & CLARIFIED** | Synthetic IP is application-level; physical egress is unhooked. |
| **Multi-Profile Transition** | Lifecycle / IPC | **PASS** | Active profile switches instantly; previous profile marked `CONSUMED`. Zero restart needed. |
| **Target App Dynamic Observation** | Physical Hardware | **NOT_PERFORMED** | Real Android hardware verification deferred to testing environment. |


