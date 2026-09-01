# Generated Build Artifacts

This directory documents the APK build artifacts produced by Gradle for the NPatch Device ID Lab.

---

## 📦 Artifact Inventory

| Artifact File | Source Gradle Module | Package Name | Role |
| :--- | :--- | :--- | :--- |
| **`app-debug.apk`** | `:app` | `com.example.deviceidlab` | Controller application, Profile Generator, ContentProvider IPC service, and NPatch / Xposed hook entry. |
| **`demo-module-debug.apk`** | `:demo-module` | `com.example.demomodule` | Target Application #1 with full in-app verification dashboard test harness. |
| **`second-target-app-debug.apk`** | `:second-target-app` | `com.example.secondtargetapp` | Target Application #2 with independent verification dashboard test harness for cross-package validation. |

---

## 🏗️ Generating Artifacts

To compile and produce all three debug APKs:

```bash
./gradlew assembleDebug
```

Outputs are placed in each respective module's build directory:
- `app/build/outputs/apk/debug/app-debug.apk`
- `demo-module/build/outputs/apk/debug/demo-module-debug.apk`
- `second-target-app/build/outputs/apk/debug/second-target-app-debug.apk`
