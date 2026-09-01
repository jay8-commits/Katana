# Device ID Randomization Lab (React TypeScript)

An educational interactive laboratory web application demonstrating how Android applications retrieve hardware and platform device identifiers, the security and privacy restrictions on modern Android versions (Android 10+), deterministic one-million test identity allocation, persistent database storage, and controlled dynamic method interception using NPatch 1.0.7 runtime ContentProvider IPC architecture.

---

## Features

1. **NPatch 1.0.7 Dynamic Hook Injection Lab**
   - Single-patch runtime ContentProvider IPC architecture simulator.
   - Dynamic injected Android ID (16-hex) and Telephony ID (15-digit IMEI) inputs.
   - Quick sequential test ID generation and deterministic seed randomization.
   - Live dual-ID verification terminal dashboard (Android ID PASS/FAIL & Telephony ID PASS/FAIL).
   - Embedded standalone Target Demo Activity (`com.example.targetdemo`) process simulator.

2. **Real Device & Platform Identifiers**
   - Live queries to standard Android platform APIs (`Settings.Secure.ANDROID_ID` and `TelephonyManager.getDeviceId() / getImei()`).
   - Detailed diagnostics explaining Android 10+ privacy restrictions on hardware identifiers.
   - Client environment hardware telemetry (Canvas 2D hash, WebGL renderer, screen resolution, CPU concurrency).

3. **1,000,000 Simulated Identity Pool Engine**
   - Deterministic 16-hex Android ID derivation via SHA-256 HMAC.
   - Deterministic 15-digit Telephony IMEI derivation via SHA-256 TAC mapping.
   - Zero-duplicate guarantee with persistent storage and live usage progress bar.
   - Batch allocation stress tests (+10, +100, +1,000 unique identities).
   - Safe database reset confirmation workflow.

4. **Bytecode Interception Sandbox**
   - Interactive method execution sandbox for `Settings.Secure.getString(ANDROID_ID)` and `TelephonyManager.getDeviceId()`.
   - Toggleable interception layer with active hook feedback.

5. **Real-time Invocation Logs**
   - Live audit stream of all method interceptions and pass-through events.
   - Filter by status (All, Intercepted, Passed Through) with search and payload copy.

---

## Technology Stack

- **Runtime:** React 18+ with TypeScript
- **Styling:** Tailwind CSS v4 with Material 3 dark cyber aesthetic
- **Icons:** Lucide React
- **Animations:** Motion & Canvas-Confetti
- **Build System:** Vite
