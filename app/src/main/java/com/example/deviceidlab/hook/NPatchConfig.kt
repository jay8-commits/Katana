package com.example.deviceidlab.hook

/**
 * Shared configuration constants for NPatch 1.0.7 runtime communication.
 *
 * Isolated from Xposed framework interfaces so standard Android controller components
 * (MainActivity, DeviceIdProvider, DeviceIdReader, NPatchAuditManager) can access configuration
 * without triggering classloading of Xposed runtime interfaces (which are compileOnly).
 */
object NPatchConfig {
    const val PREF_FILE = "npatch_config"
    const val KEY_ACTIVE_ANDROID_ID = "active_android_id"
    const val KEY_ACTIVE_TELEPHONY_ID = "active_telephony_id"
    const val KEY_INTERCEPTION_ENABLED = "interception_enabled"
    const val KEY_TARGET_PACKAGE_FILTER = "target_package_filter"

    const val PROVIDER_AUTHORITY = "com.example.deviceidlab.provider"
    const val PROVIDER_URI_STRING = "content://$PROVIDER_AUTHORITY"

    // Network Configuration Keys
    const val KEY_ACTIVE_SYNTHETIC_IP = "active_synthetic_ip"
    const val KEY_ACTIVE_MAC_ADDRESS = "active_mac_address"
    const val KEY_ACTIVE_WIFI_SSID = "active_wifi_ssid"
    const val KEY_ACTIVE_WIFI_BSSID = "active_wifi_bssid"

    // Location Subsystem Keys
    const val KEY_ACTIVE_LATITUDE = "active_latitude"
    const val KEY_ACTIVE_LONGITUDE = "active_longitude"
    const val KEY_ACTIVE_CITY = "active_city"
    const val KEY_ACTIVE_COUNTRY = "active_country"
    const val KEY_ACTIVE_TIMEZONE = "active_timezone"

    // Profile Lifecycle States (AVAILABLE -> ACTIVE -> CONSUMED)
    const val KEY_PROFILE_LIFECYCLE = "profile_lifecycle_state"
    const val STATE_AVAILABLE = "AVAILABLE"
    const val STATE_ACTIVE = "ACTIVE"
    const val STATE_CONSUMED = "CONSUMED"

    // RFC 5737 TEST-NET-3 Range (203.0.113.0/24) - Controlled Application Test Only
    const val RFC_5737_PREFIX = "203.0.113."
    const val DEFAULT_SYNTHETIC_IP = "203.0.113.42"
    const val DEFAULT_MAC = "02:00:11:22:33:44"
    const val DEFAULT_SSID = "\"LabTest_WiFi\""
    const val DEFAULT_BSSID = "02:00:11:22:33:44"

    // IP Classification Boundaries
    const val IP_TYPE_LOCAL_LOOPBACK = "LOCAL_LOOPBACK"
    const val IP_TYPE_PRIVATE_LAN = "PRIVATE_LAN"
    const val IP_TYPE_SYNTHETIC_TEST_IP = "SYNTHETIC_TEST_IP"
    const val IP_TYPE_ACTUAL_PUBLIC_EGRESS = "ACTUAL_PUBLIC_EGRESS_IP"
    const val STATUS_UNSUPPORTED_AT_CURRENT_LAYER = "UNSUPPORTED_AT_CURRENT_LAYER"

    // 5-Stage Verification Lifecycle
    const val STAGE_HOOK_REGISTERED = "HOOK_REGISTERED"
    const val STAGE_HOOK_INVOKED = "HOOK_INVOKED"
    const val STAGE_VALUE_GENERATED = "VALUE_GENERATED"
    const val STAGE_VALUE_RETURNED = "VALUE_RETURNED"
    const val STAGE_TARGET_OBSERVED = "TARGET_OBSERVED"

    /**
     * Derives a deterministic RFC 5737 (203.0.113.0/24) test IP from an identity pool index.
     * Guarantees RFC 5737 TEST-NET-3 compliance and no stale IP carryover between profiles.
     */
    fun deriveSyntheticIp(identityNumber: Long): String {
        val lastOctet = (Math.abs(identityNumber) % 240) + 10
        return "$RFC_5737_PREFIX$lastOctet"
    }

    data class WorldwideLocation(
        val city: String,
        val country: String,
        val latitude: Double,
        val longitude: Double,
        val timezone: String
    )

    val WORLDWIDE_LOCATIONS = listOf(
        WorldwideLocation("Tokyo", "JP", 35.6762, 139.6503, "Asia/Tokyo"),
        WorldwideLocation("London", "GB", 51.5074, -0.1278, "Europe/London"),
        WorldwideLocation("New York", "US", 40.7128, -74.0060, "America/New_York"),
        WorldwideLocation("Paris", "FR", 48.8566, 2.3522, "Europe/Paris"),
        WorldwideLocation("Sydney", "AU", -33.8688, 151.2093, "Australia/Sydney"),
        WorldwideLocation("Singapore", "SG", 1.3521, 103.8198, "Asia/Singapore"),
        WorldwideLocation("Berlin", "DE", 52.5200, 13.4050, "Europe/Berlin"),
        WorldwideLocation("Toronto", "CA", 43.6532, -79.3832, "America/Toronto"),
        WorldwideLocation("Seoul", "KR", 37.5665, 126.9780, "Asia/Seoul"),
        WorldwideLocation("Amsterdam", "NL", 52.3676, 4.9041, "Europe/Amsterdam"),
        WorldwideLocation("Zurich", "CH", 47.3769, 8.5417, "Europe/Zurich"),
        WorldwideLocation("São Paulo", "BR", -23.5505, -46.6333, "America/Sao_Paulo"),
        WorldwideLocation("Stockholm", "SE", 59.3293, 18.0686, "Europe/Stockholm"),
        WorldwideLocation("San Francisco", "US", 37.7749, -122.4194, "America/Los_Angeles"),
        WorldwideLocation("Dubai", "AE", 25.2048, 55.2708, "Asia/Dubai"),
        WorldwideLocation("Dublin", "IE", 53.3498, -6.2603, "Europe/Dublin")
    )

    fun deriveLocation(identityNumber: Long): WorldwideLocation {
        val idx = (Math.abs(identityNumber) % WORLDWIDE_LOCATIONS.size).toInt()
        return WORLDWIDE_LOCATIONS[idx]
    }

    fun deriveMac(identityNumber: Long): String {
        val b1 = "%02X".format(Math.abs(identityNumber * 3) % 256)
        val b2 = "%02X".format(Math.abs(identityNumber * 7) % 256)
        return "02:00:11:22:$b1:$b2"
    }

    fun deriveWifiSsid(city: String): String {
        return "\"LabTest_WiFi_$city\""
    }

    /**
     * Categorizes an IP address into architectural test boundaries.
     */
    fun classifyIp(ip: String?): String {
        if (ip.isNullOrBlank()) return "UNKNOWN / NULL"
        val trimmed = ip.trim().removePrefix("/")
        return when {
            trimmed.startsWith("127.") || trimmed == "::1" -> IP_TYPE_LOCAL_LOOPBACK
            trimmed.startsWith("10.") || trimmed.startsWith("192.168.") ||
                    (trimmed.startsWith("172.") && isPrivate172(trimmed)) -> IP_TYPE_PRIVATE_LAN
            trimmed.startsWith(RFC_5737_PREFIX) -> IP_TYPE_SYNTHETIC_TEST_IP
            else -> IP_TYPE_ACTUAL_PUBLIC_EGRESS
        }
    }

    private fun isPrivate172(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size >= 2) {
                val second = parts[1].toIntOrNull() ?: 0
                second in 16..31
            } else false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Packs an IPv4 string into a 32-bit little-endian integer for WifiInfo.getIpAddress().
     */
    fun ipToIntLittleEndian(ip: String): Int {
        return try {
            val parts = ip.split(".")
            if (parts.size == 4) {
                val b0 = parts[0].toInt() and 0xFF
                val b1 = parts[1].toInt() and 0xFF
                val b2 = parts[2].toInt() and 0xFF
                val b3 = parts[3].toInt() and 0xFF
                (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
            } else 0
        } catch (_: Throwable) {
            0
        }
    }

    /**
     * Unpacks a 32-bit little-endian integer into an IPv4 address string.
     */
    fun intToIpLittleEndian(ipInt: Int): String {
        return "${ipInt and 0xFF}.${(ipInt shr 8) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 24) and 0xFF}"
    }

    /**
     * Converts a colon-delimited MAC string to a byte array.
     */
    fun macToByteArray(mac: String): ByteArray {
        return try {
            val parts = mac.split(":", "-")
            if (parts.size == 6) {
                ByteArray(6) { i -> parts[i].toInt(16).toByte() }
            } else {
                byteArrayOf(0x02, 0x00, 0x11, 0x22, 0x33, 0x44)
            }
        } catch (_: Throwable) {
            byteArrayOf(0x02, 0x00, 0x11, 0x22, 0x33, 0x44)
        }
    }

    /**
     * Converts a byte array to colon-delimited MAC string.
     */
    fun byteArrayToMac(bytes: ByteArray?): String {
        if (bytes == null || bytes.size != 6) return "02:00:00:00:00:00"
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Set dynamically when the NPatch/Xposed runtime environment initializes in a target process.
     */
    @Volatile
    var isXposedEnvironmentActive: Boolean = false
}
