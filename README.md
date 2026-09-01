# NPatch Device ID Lab

> **Educational & Testing Laboratory for Authorized Applications**  
> Demonstrating multi-process Android device identity retrieval, platform restrictions, dynamic profile generation, and Hook-based test identity interception.

---

## 📌 Project Overview

This repository provides an educational and auditable testbed for analyzing Android device identifiers, framework security boundaries across API levels, and dynamic interception using the **Patch Once $\rightarrow$ Install Once $\rightarrow$ Dynamic Profile Switching** architecture.

### Key Architecture Invariant
```
[ Device ID Lab Controller ] (com.example.deviceidlab)
        │
        │ Generates Test Profiles & Serves DeviceIdProvider (ContentProvider IPC)
        ▼
[ NPatch Hook Entry ] (com.example.deviceidlab.hook.NPatchHookEntry)
        │
        │ Dynamically intercepts target processes (zero hardcoded package bindings)
        ├────────────────────────────────┬────────────────────────────────┐
        ▼                                ▼                                ▼
[ Target Demo #1 ]             [ Target Demo #2 ]              [ Additional Scoped Targets ]
(com.example.demomodule)       (com.example.secondtargetapp)   (Any scoped package)
```

---

## 🔬 Testing Tier Classification

- **Tier A (Source & Hook Inspection)**: VERIFIED (16 APIs cataloged with hook bindings and signatures)
- **Tier B (Unit Tests)**: PASS (`DeviceIdentityManagerTest` verifying profile integrity and Luhn validation)
- **Tier C (Build Verification)**: PASS (Gradle build configuration and multi-module layout)
- **Tier D (Emulator Verification)**: NOT PERFORMED
- **Tier E (Physical Device Verification)**: **NOT PERFORMED** (Awaiting deployment to physical Android hardware with active LSPosed/NPatch)

---

## 📂 Modules

1. **`:app`** (`com.example.deviceidlab`):
   - Device profile generation engine (UUID, Luhn-valid IMEI, Android ID, Build parameters, MAC).
   - `DeviceIdProvider`: Fast MatrixCursor-backed IPC ContentProvider.
   - `NPatchHookEntry`: Generalized Xposed / NPatch / LSPosed hook entry point with ThreadLocal anti-recursion guards and masked diagnostic logging.
   - `TestApiCatalog`: Centralized, auditable test API allowlist (16 registered APIs).
2. **`:demo-module`** (`com.example.demomodule`):
   - Primary target demonstration consumer application.
   - Real Runtime Verification Dashboard & Test Harness independently invoking framework APIs.
3. **`:second-target-app`** (`com.example.secondtargetapp`):
   - Independent second target consumer application demonstrating multi-package cross-process injection without re-patching.

---

## 🛠️ Reproducible Build Commands

```bash
# Clean previous build artifacts
./gradlew clean

# Run unit tests across all modules
./gradlew test

# Assemble debug APKs for all three modules
./gradlew assembleDebug

# Output APKs:
# - app/build/outputs/apk/debug/app-debug.apk
# - demo-module/build/outputs/apk/debug/demo-module-debug.apk
# - second-target-app/build/outputs/apk/debug/second-target-app-debug.apk
```

---

## 📚 Documentation Index

- [Architecture & Data Flow](docs/ARCHITECTURE.md)
- [API Coverage & Truthful Matrix](docs/API-COVERAGE.md)
- [Verification Framework & Diagnostics](docs/VERIFICATION.md)
- [Physical Device Testing Guide](docs/PHYSICAL_DEVICE_TEST.md)
- [Artifacts Guide](artifacts/README.md)
