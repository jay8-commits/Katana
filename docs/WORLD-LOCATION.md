# Worldwide Location Profile System Architecture

## 1. Overview & Architectural Role

The Worldwide Location Subsystem extends `DeviceIdRandomizationLab` with a coherent, multi-city geographic simulation engine. Rather than generating detached, random floating-point coordinates or locking tests to a single hard-coded city, the subsystem provides a database of **80+ curated, canonical global cities** across **9 geographic regions**.

Every generated profile enforces mathematical and logical consistency across:
- **City Name** (e.g., Tokyo, London, Manila, New York)
- **Country Name & ISO-3166 2-Letter Code** (e.g., Japan / JP, United Kingdom / GB)
- **Geographic Region** (e.g., Asia, Europe, North America, Oceania)
- **Valid Latitude & Longitude** bound strictly to official city coordinates within `[-90.0, 90.0]` and `[-180.0, 180.0]`
- **Official IANA Timezone ID** (e.g., `Asia/Tokyo`, `Europe/London`, `America/New_York`)
- **Synthetic Test IPv4 Address** allocated from IETF RFC 5737 `TEST-NET-3` (`203.0.113.0/24`)
- **Sensor Parameters**: Altitude (meters), Accuracy (meters), Speed (m/s), Bearing (degrees), Provider (`gps` / `fused` / `network`)

---

## 2. Supported City Selection Modes

The subsystem implements three distinct operational modes:

### Mode 1: Manual City Selection
- The user or test script selects an exact city by canonical ID (e.g., `as_tok` for Tokyo, `eu_lon` for London, `as_mnl` for Manila).
- Search and region filtering (Asia, Europe, North America, South America, Oceania, Africa, Middle East, Caribbean, Central America) allow fast target lookup.
- Coordinates, country, timezone, and synthetic IP are immediately locked to that canonical city record.

### Mode 2: Random World City Selection
- The subsystem draws an entry uniformly from the entire 80+ city catalog.
- Accepts an optional deterministic seed for reproducible test automation.
- Re-generates a unique RFC 5737 synthetic IP for the selected city.

### Mode 3: Random City Within Selected Country
- The user selects a specific country (e.g., Japan, United States, Germany, Australia, Philippines).
- The subsystem filters the catalog for all cities registered under that country and randomly draws one.
- Guarantees that target country requirements in enterprise geofence or region compliance tests are strictly respected.

---

## 3. Worldwide Catalog Coverage (80+ Cities across 9 Regions)

| Region | Sample Cities |
| :--- | :--- |
| **Asia** | Tokyo (JP), Osaka (JP), Kyoto (JP), Sapporo (JP), Seoul (KR), Busan (KR), Beijing (CN), Shanghai (CN), Shenzhen (CN), Hong Kong (HK), Taipei (TW), Singapore (SG), Manila (PH), Cebu (PH), Bangkok (TH), Kuala Lumpur (MY), Jakarta (ID), Hanoi (VN), Mumbai (IN), Delhi (IN), Bengaluru (IN) |
| **Europe** | London (GB), Manchester (GB), Edinburgh (GB), Paris (FR), Lyon (FR), Marseille (FR), Berlin (DE), Munich (DE), Frankfurt (DE), Hamburg (DE), Amsterdam (NL), Rotterdam (NL), Brussels (BE), Zurich (CH), Geneva (CH), Vienna (AT), Rome (IT), Milan (IT), Madrid (ES), Barcelona (ES), Lisbon (PT), Dublin (IE), Stockholm (SE), Oslo (NO), Copenhagen (DK), Helsinki (FI), Warsaw (PL), Prague (CZ), Budapest (HU), Athens (GR) |
| **North America** | New York (US), Los Angeles (US), Chicago (US), Houston (US), Miami (US), San Francisco (US), Seattle (US), Toronto (CA), Montreal (CA), Vancouver (CA), Calgary (CA), Mexico City (MX), Guadalajara (MX), Monterrey (MX) |
| **South America** | São Paulo (BR), Rio de Janeiro (BR), Buenos Aires (AR), Santiago (CL), Bogota (CO), Lima (PE) |
| **Oceania** | Sydney (AU), Melbourne (AU), Brisbane (AU), Perth (AU), Auckland (NZ), Wellington (NZ) |
| **Africa** | Cairo (EG), Johannesburg (ZA), Cape Town (ZA), Nairobi (KE), Lagos (NG), Casablanca (MA) |
| **Middle East** | Dubai (AE), Abu Dhabi (AE), Riyadh (SA), Doha (QA), Tel Aviv (IL), Istanbul (TR) |
| **Caribbean & Central America** | San Juan (PR), Havana (CU), Kingston (JM), Panama City (PA), San José (CR) |

---

## 4. Multi-Profile Transition & Lifecycle Invariants

1. **Atomic Consumption**: When a new location profile is activated, the currently active profile is transitioned to `CONSUMED`.
2. **State Tagging**:
   - `ACTIVE`: The profile currently exposed via `DeviceIdProvider` ContentProvider IPC to Xposed hooks and target applications.
   - `CONSUMED`: Previously used profiles kept in historical audit memory for anti-reuse tracking.
3. **Dynamic Reconfigurability**: Profile transitions occur purely through ContentProvider / SharedPreferences updates. **No target app restart or NPatch re-patching is required.**

---

## 5. IPC Schema (ContentProvider `com.example.deviceidlab.provider`)

The location subsystem exposes these additional columns in cursor query `content://com.example.deviceidlab.provider/device_ids`:

| Column Key | Description | Example Value |
| :--- | :--- | :--- |
| `loc_city` | Canonical City Name | `"Tokyo"` |
| `loc_country` | Country Full Name | `"Japan"` |
| `loc_country_code` | ISO-3166-1 Alpha-2 | `"JP"` |
| `loc_region` | Continental / Regional Group | `"Asia"` |
| `loc_latitude` | Decimal Latitude | `"35.6762"` |
| `loc_longitude` | Decimal Longitude | `"139.6503"` |
| `loc_timezone` | IANA Timezone Identifier | `"Asia/Tokyo"` |
| `loc_synthetic_ip` | RFC 5737 Test IPv4 | `"203.0.113.42"` |
| `loc_state` | Lifecycle State | `"ACTIVE"` / `"CONSUMED"` |

---

## 6. Verification Status

| Verification Tier | Environment | Status | Details |
| :--- | :--- | :--- | :--- |
| **Domain Logic & Catalog Integrity** | TypeScript / Node.js unit tests | **PASS (15/15 tests)** | Catalog integrity, coordinate bounds, timezone validity, and selection modes verified. |
| **Android Framework Code** | Kotlin source classes | **SYNTAX & ARCHITECTURE VERIFIED** | Validated via source inspect and IPC contracts. |
| **Gradle Android Build** | Build Container | **BLOCKED (Environment)** | JDK and Android SDK unavailable in cloud container. |
| **Physical Device Runtime** | NPatch / LSPosed Android 10-14 | **NOT_PERFORMED** | Real hardware verification scheduled for physical target device. |
