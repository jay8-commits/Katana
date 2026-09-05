# Location API Coverage & Interception Strategy

## 1. Catalog of 14 Standard Android Location APIs

The location subsystem catalogs 14 standard Android framework and Google Play Services Location APIs.

| # | Target API Signature | Framework Class | Interception Strategy |
| :--- | :--- | :--- | :--- |
| 1 | `getLastKnownLocation(String provider)` | `android.location.LocationManager` | **ACTIVE (NPatch hook replaces return Location)** |
| 2 | `isProviderEnabled(String provider)` | `android.location.LocationManager` | **ACTIVE (Returns `true` for GPS/Network)** |
| 3 | `requestLocationUpdates(String, long, float, LocationListener)` | `android.location.LocationManager` | Synthetic callback delivery via hook |
| 4 | `requestLocationUpdates(LocationRequest, LocationListener, Looper)` | `android.location.LocationManager` | Synthetic callback delivery via hook |
| 5 | `requestLocationUpdates(String, long, float, PendingIntent)` | `android.location.LocationManager` | Synthetic Intent broadcast |
| 6 | `requestSingleUpdate(String, LocationListener, Looper)` | `android.location.LocationManager` | Immediate single Location delivery |
| 7 | `requestSingleUpdate(Criteria, LocationListener, Looper)` | `android.location.LocationManager` | Immediate single Location delivery |
| 8 | `getCurrentLocation(String, CancellationSignal, Executor, Consumer)` | `android.location.LocationManager` (API 30+) | Single callback to Consumer |
| 9 | `getProviders(boolean enabledOnly)` | `android.location.LocationManager` | Returns `["gps", "network", "passive", "fused"]` |
| 10 | `getBestProvider(Criteria, boolean)` | `android.location.LocationManager` | Returns `"gps"` |
| 11 | `getLastLocation()` | `com.google.android.gms.location.FusedLocationProviderClient` | Task-based mock return |
| 12 | `getCurrentLocation(int, CancellationToken)` | `com.google.android.gms.location.FusedLocationProviderClient` | Task-based mock return |
| 13 | `requestLocationUpdates(LocationRequest, LocationCallback, Looper)` | `com.google.android.gms.location.FusedLocationProviderClient` | Dispatches LocationResult |
| 14 | `getLatitude()` / `getLongitude()` | `android.location.Location` | Getter interception on synthetic object |

---

## 2. Location Return Object Construction

When the hook intercepts `LocationManager.getLastKnownLocation(provider)`, it constructs a genuine `android.location.Location` object populated from the active profile:

```kotlin
val mockLocation = Location(provider ?: "gps").apply {
    latitude = activeProfile.latitude
    longitude = activeProfile.longitude
    altitude = activeProfile.altitude
    accuracy = activeProfile.accuracy
    speed = activeProfile.speed
    bearing = activeProfile.bearing
    time = System.currentTimeMillis()
    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
}
```

---

## 3. Worldwide Profile Integration

The constructed location object is coupled with the worldwide metadata:
- **City & Country**: Verified against the canonical 80+ city catalog.
- **Timezone**: Matching IANA identifier (`Asia/Tokyo`, `Europe/London`, etc.).
- **Synthetic IP**: Deterministic RFC 5737 TEST-NET-3 address (`203.0.113.x`).
- **Profile Consistency**: Enforces that city, country, coordinates, timezone, and synthetic IP are coherently aligned.

---

## 4. Preservation of 21-API Device Identity Baseline

The Location subsystem is strictly additive. The 21-API device identity baseline remains fully preserved:
- **14 Dynamic APIs**: No app restart required.
- **7 Static APIs**: Require target restart.
- **0 APIs removed, renamed, or weakened.**

---

## 5. Verification Status

- **Unit & Domain Tests**: **PASS (15/15 tests)**
- **Android Gradle Build**: **BLOCKED (Environment — No JDK/Android SDK)**
- **Physical Device Runtime**: **NOT_PERFORMED**
