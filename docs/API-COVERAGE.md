# API Coverage & Centralized Allowlist

All supported test APIs are defined in `com.example.deviceidlab.hook.TestApiCatalog`.

---

## 📋 Centralized API Inventory (16 Total)

| # | API Name | Method / Field Signature | Replacement Source | Runtime Mechanism | Platform Security / OS Restrictions |
| :- | :--- | :--- | :--- | :--- | :--- |
| 1 | **`Settings.Secure.getString()`** | `getString(ContentResolver, "android_id")` | `DeviceProfile.androidId` | Dynamic Method Hook (`XC_MethodHook`) | Standard public API across all Android versions |
| 2 | **`Settings.Secure.getStringForUser()`** | `getStringForUser(ContentResolver, "android_id", int)` | `DeviceProfile.androidId` | Dynamic Method Hook (`XC_MethodHook`) | Internal system method |
| 3 | **`Build.MODEL`** | `android.os.Build.MODEL` (String) | `DeviceProfile.buildModel` | Static Field Reflection | Requires process launch / restart |
| 4 | **`Build.MANUFACTURER`** | `android.os.Build.MANUFACTURER` (String) | `DeviceProfile.buildManufacturer` | Static Field Reflection | Requires process launch / restart |
| 5 | **`Build.BRAND`** | `android.os.Build.BRAND` (String) | `DeviceProfile.buildBrand` | Static Field Reflection | Requires process launch / restart |
| 6 | **`Build.PRODUCT`** | `android.os.Build.PRODUCT` (String) | `DeviceProfile.buildProduct` | Static Field Reflection | Requires process launch / restart |
| 7 | **`Build.DEVICE`** | `android.os.Build.DEVICE` (String) | `DeviceProfile.buildDevice` | Static Field Reflection | Requires process launch / restart |
| 8 | **`Build.FINGERPRINT`** | `android.os.Build.FINGERPRINT` (String) | `DeviceProfile.buildFingerprint` | Static Field Reflection | Requires process launch / restart |
| 9 | **`Build.SERIAL`** | `android.os.Build.SERIAL` (String) | `DeviceProfile.serialNumber` | Static Field Reflection | Deprecated in API 26 (returns `UNKNOWN` on modern OS) |
| 10 | **`Build.getSerial()`** | `android.os.Build.getSerial()` | `DeviceProfile.serialNumber` | Dynamic Method Hook (`XC_MethodHook`) | Requires `READ_PRIVILEGED_PHONE_STATE` on Android 9+ |
| 11 | **`TelephonyManager.getDeviceId()`** | `getDeviceId()` & `getDeviceId(int)` | `DeviceProfile.imei` | Dynamic Method Hook (`XC_MethodHook`) | Restricted on Android 10+ (API 29+) |
| 12 | **`TelephonyManager.getImei()`** | `getImei()` & `getImei(int)` | `DeviceProfile.imei` | Dynamic Method Hook (`XC_MethodHook`) | Restricted on Android 10+ (API 29+) |
| 13 | **`TelephonyManager.getMeid()`** | `getMeid()` & `getMeid(int)` | `DeviceProfile.imei` | Dynamic Method Hook (`XC_MethodHook`) | Requires CDMA device / Restricted on Android 10+ |
| 14 | **`TelephonyManager.getSimSerialNumber()`** | `getSimSerialNumber()` | `DeviceProfile.serialNumber` | Dynamic Method Hook (`XC_MethodHook`) | Restricted on Android 10+ (API 29+) |
| 15 | **`TelephonyManager.getSubscriberId()`** | `getSubscriberId()` | `DeviceProfile.imei` | Dynamic Method Hook (`XC_MethodHook`) | Restricted on Android 10+ (API 29+) |
| 16 | **`WifiInfo.getMacAddress()`** | `WifiInfo.getMacAddress()` | `DeviceProfile.macAddress` | Dynamic Method Hook (`XC_MethodHook`) | Returns `02:00:00:00:00:00` by default on Android 6.0+ |

---

## 🔍 Truthful API Verification Matrix

> **Testing Environment Note**: Physical hardware testing was not performed in this build environment. All physical runtime fields are marked `NOT_TESTED` to prevent false assertions of physical execution.

| API | Code Hook | Build Test | Target Call Implemented | Physical Runtime Tested | Interception Observed | Generated Value Observed | Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Settings.Secure.getString(ANDROID_ID)** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Settings.Secure.getStringForUser(ANDROID_ID)** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.MODEL** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.MANUFACTURER** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.BRAND** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.PRODUCT** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.DEVICE** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.FINGERPRINT** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.SERIAL (Field)** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |
| **Build.getSerial() (Method)** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **PLATFORM_RESTRICTED** |
| **TelephonyManager.getDeviceId() & (int)** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **PLATFORM_RESTRICTED** |
| **TelephonyManager.getImei() & (int)** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **PLATFORM_RESTRICTED** |
| **TelephonyManager.getMeid() & (int)** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **PLATFORM_RESTRICTED** |
| **TelephonyManager.getSimSerialNumber()** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **PLATFORM_RESTRICTED** |
| **TelephonyManager.getSubscriberId()** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **PLATFORM_RESTRICTED** |
| **WifiInfo.getMacAddress()`** | YES | PASS | YES | NOT_TESTED | NOT_TESTED | NOT_TESTED | **NOT_TESTED** |

---

## 🔒 Security Invariant & Explicit Exclusions

The following are explicitly excluded to maintain compliance with research integrity:
- **No Keystore Private Key Tampering**: Hardware TrustZone keys remain untouched.
- **No Widevine DRM Interception**: Level 1 DRM certificates are not spoofed.
- **Masked Diagnostic Logs**: All logged values are masked with prefix, suffix, and SHA-256 hash checksums.
