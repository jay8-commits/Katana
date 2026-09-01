# System Architecture & Data Flow

## 1. High-Level Concept

The NPatch Device ID Lab architecture enables deterministic testing and verification of Android device identifier APIs without rebuilding or repatching target APKs when test identities change.

```
                    ┌────────────────────────────┐
                    │     Controller Module      │
                    │ (com.example.deviceidlab)  │
                    │  - Profile Generator       │
                    │  - DeviceIdentityManager   │
                    │  - DeviceIdProvider (IPC)  │
                    └─────────────┬──────────────┘
                                  │ ContentProvider IPC
                                  ▼
                    ┌────────────────────────────┐
                    │      NPatchHookEntry       │
                    │  (Xposed Hook Interface)   │
                    │  - Recursion Prevention    │
                    │  - TestApiCatalog Guard    │
                    │  - Masked Diagnostics      │
                    └──────┬──────────────┬──────┘
                           │              │
             Injected into │              │ Injected into
             Process #1    ▼              ▼ Process #2
           ┌────────────────────┐   ┌────────────────────────────┐
           │   Target Demo #1   │   │     Target Demo #2         │
           │(com.example.       │   │(com.example.               │
           │ demomodule)        │   │ secondtargetapp)           │
           │ - Dashboard        │   │ - Independent Dashboard    │
           └────────────────────┘   └────────────────────────────┘
```

---

## 2. Invariant: Patch Once $\rightarrow$ Install Once $\rightarrow$ Dynamic Profile Switching

1. **Patch Once**: Target applications are patched once via NPatch / LSPosed to load the `NPatchHookEntry` hook module.
2. **Install Once**: Target APKs and Controller APK are installed once onto the host system or virtual environment.
3. **Generate Profile**: The controller generates and persists a new `DeviceProfile` in `SharedPreferences`.
4. **IPC Query**: When an injected target calls an identity API (e.g. `Settings.Secure.getString(ContentResolver, "android_id")`), the hook queries `DeviceIdProvider` over Android Binder IPC.
5. **Dynamic Replacement**: The hook replaces the result register with the active profile value and returns it immediately to the caller.

---

## 3. Handling Static vs Dynamic Fields

| Identifier Type | Mechanism | Dynamic Switching Requirement |
| :--- | :--- | :--- |
| **`Settings.Secure.ANDROID_ID`** | `XC_MethodHook.afterHookedMethod` | **Instant** — updates on subsequent method calls. |
| **`Build.getSerial()`** | `XC_MethodHook.afterHookedMethod` | **Instant** — updates on subsequent method calls. |
| **`Build.MODEL / BRAND / FINGERPRINT`** | `XposedHelpers.setStaticObjectField` | **Process Restart** — static fields are initialized during class loading / app startup. |
| **`TelephonyManager.getDeviceId()`** | `XC_MethodHook.afterHookedMethod` | **Instant** (pre-Q or permissioned test environments). |
| **`WifiInfo.getMacAddress()`** | `XC_MethodHook.afterHookedMethod` | **Instant** — updates on subsequent method calls. |

---

## 4. Safety & Anti-Recursion Controls

- **ThreadLocal Guard**: `isHookExecuting` flag prevents re-entrant loops when hooks invoke framework ContentResolver internally.
- **Excluded Controller Package**: `lpparam.packageName == "com.example.deviceidlab"` aborts hook installation in the controller process to preserve genuine OS queries.
- **Log Masking**: All diagnostic logs pass through `TestApiCatalog.maskValue(raw)`, outputting only truncated prefixes/suffixes and SHA-256 fingerprint prefixes.
