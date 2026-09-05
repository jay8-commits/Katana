# Synthetic IP Profile & Network Egress Architecture

## 1. Executive Summary & Architectural Reality Check

> **CRITICAL REALITY CHECK**:
> In Android application security testing, setting a **Synthetic Test IP** provides an application-level simulation parameter (RFC 5737) to test how target apps parse, log, or correlate device identity records.
>
> Bytecode hooks (NPatch / LSPosed / ART runtime method interception) **DO NOT modify the physical device's TCP/IP network egress, socket routing tables, or actual public IP**.
>
> Actual public IP addresses are determined strictly by the physical Wi-Fi router, cellular baseband carrier NAT, or an active VPN/proxy tunnel. Any claims that an Xposed hook alone changed your real public Internet IP are technically false.

---

## 2. Four-Tier IP Source Classification

The `DeviceIdRandomizationLab` system formalizes four distinct categories of IP addresses on Android:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID NETWORK IP TIERS                        │
├────────────────────────────────┬───────────────────────────────────────┤
│ Tier 1: Local Loopback IP      │ 127.0.0.1 / ::1 (lo interface)        │
│                                │ Intra-device IPC only.                │
├────────────────────────────────┼───────────────────────────────────────┤
│ Tier 2: Private LAN IP         │ 192.168.1.x / 10.0.0.x (wlan0/rmnet)  │
│                                │ Assigned by local router DHCP.        │
├────────────────────────────────┼───────────────────────────────────────┤
│ Tier 3: Synthetic Test IP      │ 203.0.113.x (IETF RFC 5737 TEST-NET-3)│
│                                │ Application test profile parameter.   │
│                                │ Controllable via IPC / hooks.        │
├────────────────────────────────┼───────────────────────────────────────┤
│ Tier 4: Actual Public IP       │ External carrier/ISP routable IP.     │
│                                │ Observed by external web servers.     │
│                                │ NOT modified by NPatch hooks.         │
└────────────────────────────────┴───────────────────────────────────────┘
```

### Detailed Tier Comparison Table

| Attribute | Tier 1: Loopback | Tier 2: Private LAN | Tier 3: Synthetic Test IP | Tier 4: Actual Public Egress IP |
| :--- | :--- | :--- | :--- | :--- |
| **Address Space** | `127.0.0.1` / `::1` | `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16` | `203.0.113.0/24` (RFC 5737 TEST-NET-3) | Globally routable IPv4/IPv6 |
| **Governing Entity** | Linux Kernel `lo` | Local DHCP Server (Wi-Fi/Cellular AP) | `DeviceIdRandomizationLab` Profile Subsystem | Carrier Gateway, ISP, or VPN provider |
| **Hook Modifiable?** | No | No (OS interface) | **YES (Simulated Profile Field)** | **NO (Hardware/Network level)** |
| **Public Geolocatable?**| No | No | **NO (Reserved Test Space)** | **YES (MaxMind / IP2Location / ISP)** |
| **Purpose in Testing** | Local daemon comms | Network topology | Target app profile verification & correlation | External fraud engine egress inspection |

---

## 3. RFC 5737 Compliance

To prevent test IP pollution and accidental collision with real routable Internet addresses, `DeviceIdRandomizationLab` strictly restricts synthetic IPv4 generation to:

- **Primary Range**: `203.0.113.0/24` (`TEST-NET-3`)
- Reserved by IETF RFC 5737 explicitly for documentation, testing, and simulation.
- Guaranteed never to be assigned to any public commercial entity or routing table.
- Range: `203.0.113.1` to `203.0.113.254` (host values 1..254 generated deterministically from the profile seed).

---

## 4. Multi-Profile Switching: Why No App Restart is Required

Traditional device spoofers frequently require a full OS reboot or package force-stop because they alter static system properties (e.g. `/system/build.prop`).

In contrast, the `DeviceIdRandomizationLab` architecture achieves instant, zero-restart multi-profile switching through:

1. **ContentProvider Shared State**: The active profile lives in memory/SharedPreferences managed by `LocationProfileManager` and exposed by `DeviceIdProvider`.
2. **On-Demand Hook Hooking**: When the target process calls `LocationManager.getLastKnownLocation()` or queries profile attributes, the hook reads the latest state from the ContentProvider cursor dynamically.
3. **Seamless Transitions**: Switching from Tokyo (`35.6762, 139.6503`, `203.0.113.42`) to London (`51.5074, -0.1278`, `203.0.113.88`) happens in milliseconds via IPC.
4. **Lifecycle Tracking**: The previous profile is transitioned to `CONSUMED` to maintain cryptographic auditability and avoid profile reuse.

---

## 5. Verification Status

| Component | Status | Environment |
| :--- | :--- | :--- |
| **RFC 5737 IP Generation Logic** | **PASS** | Node.js / TypeScript unit tests |
| **Kotlin SyntheticIpGenerator** | **VERIFIED** | Source audit and IPC contract tests |
| **IPC Cursor Transmission** | **VERIFIED** | ContentProvider query mapping |
| **Physical Egress Hooking** | **N/A (EXPLICITLY SCOPED)** | Synthetic IP is application-level; egress is unhooked |
| **Physical Device Verification** | **NOT_PERFORMED** | Real Android hardware verification deferred |
