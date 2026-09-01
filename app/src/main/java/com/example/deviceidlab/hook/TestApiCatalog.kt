package com.example.deviceidlab.hook

import java.security.MessageDigest

/**
 * Centralized, auditable catalog of explicitly approved test APIs in the lab.
 */
data class SupportedApiDefinition(
    val id: String,
    val name: String,
    val frameworkClass: String,
    val targetMethodOrField: String,
    val configKey: String,
    val isDynamic: Boolean,
    val requiresProcessRestart: Boolean,
    val description: String
)

object TestApiCatalog {

    fun maskValue(raw: String?): String {
        if (raw == null) return "<null>"
        if (raw.isEmpty()) return "<empty>"
        if (raw.length <= 4) return "****"
        return "${raw.take(2)}...${raw.takeLast(2)} (${sha256(raw).take(6)})"
    }

    fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (_: Exception) {
            "hash_err"
        }
    }

    val SUPPORTED_APIS = listOf(
        SupportedApiDefinition(
            id = "android_id_get_string",
            name = "Settings.Secure.getString(ANDROID_ID)",
            frameworkClass = "android.provider.Settings\$Secure",
            targetMethodOrField = "getString(ContentResolver, String)",
            configKey = NPatchConfig.KEY_ANDROID_ID,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts ContentResolver lookups for Settings.Secure.ANDROID_ID."
        ),
        SupportedApiDefinition(
            id = "android_id_get_string_for_user",
            name = "Settings.Secure.getStringForUser(ANDROID_ID)",
            frameworkClass = "android.provider.Settings\$Secure",
            targetMethodOrField = "getStringForUser(ContentResolver, String, int)",
            configKey = NPatchConfig.KEY_ANDROID_ID,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts internal user-scoped Settings.Secure queries."
        ),
        SupportedApiDefinition(
            id = "build_model",
            name = "Build.MODEL",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "MODEL (Field)",
            configKey = NPatchConfig.KEY_BUILD_MODEL,
            isDynamic = false,
            requiresProcessRestart = true,
            description = "Reflectively updates static Build.MODEL on target class initialization."
        ),
        SupportedApiDefinition(
            id = "build_manufacturer",
            name = "Build.MANUFACTURER",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "MANUFACTURER (Field)",
            configKey = NPatchConfig.KEY_BUILD_MANUFACTURER,
            isDynamic = false,
            requiresProcessRestart = true,
            description = "Reflectively updates static Build.MANUFACTURER."
        ),
        SupportedApiDefinition(
            id = "build_brand",
            name = "Build.BRAND",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "BRAND (Field)",
            configKey = NPatchConfig.KEY_BUILD_BRAND,
            isDynamic = false,
            requiresProcessRestart = true,
            description = "Reflectively updates static Build.BRAND."
        ),
        SupportedApiDefinition(
            id = "build_product",
            name = "Build.PRODUCT",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "PRODUCT (Field)",
            configKey = NPatchConfig.KEY_BUILD_PRODUCT,
            isDynamic = false,
            requiresProcessRestart = true,
            description = "Reflectively updates static Build.PRODUCT."
        ),
        SupportedApiDefinition(
            id = "build_device",
            name = "Build.DEVICE",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "DEVICE (Field)",
            configKey = NPatchConfig.KEY_BUILD_DEVICE,
            isDynamic = false,
            requiresProcessRestart = true,
            description = "Reflectively updates static Build.DEVICE."
        ),
        SupportedApiDefinition(
            id = "build_fingerprint",
            name = "Build.FINGERPRINT",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "FINGERPRINT (Field)",
            configKey = NPatchConfig.KEY_BUILD_FINGERPRINT,
            isDynamic = false,
            requiresProcessRestart = true,
            description = "Reflectively updates static Build.FINGERPRINT."
        ),
        SupportedApiDefinition(
            id = "build_serial_field",
            name = "Build.SERIAL (Field)",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "SERIAL (Field)",
            configKey = NPatchConfig.KEY_SERIAL,
            isDynamic = false,
            requiresProcessRestart = true,
            description = "Reflectively updates deprecated static Build.SERIAL field."
        ),
        SupportedApiDefinition(
            id = "build_get_serial",
            name = "Build.getSerial()",
            frameworkClass = "android.os.Build",
            targetMethodOrField = "getSerial()",
            configKey = NPatchConfig.KEY_SERIAL,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts Build.getSerial() method calls dynamically."
        ),
        SupportedApiDefinition(
            id = "telephony_device_id",
            name = "TelephonyManager.getDeviceId()",
            frameworkClass = "android.telephony.TelephonyManager",
            targetMethodOrField = "getDeviceId() / getDeviceId(int)",
            configKey = NPatchConfig.KEY_IMEI,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts TelephonyManager device ID queries (pre-Android 10 / permitted testing)."
        ),
        SupportedApiDefinition(
            id = "telephony_imei",
            name = "TelephonyManager.getImei()",
            frameworkClass = "android.telephony.TelephonyManager",
            targetMethodOrField = "getImei() / getImei(int)",
            configKey = NPatchConfig.KEY_IMEI,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts TelephonyManager IMEI queries."
        ),
        SupportedApiDefinition(
            id = "telephony_meid",
            name = "TelephonyManager.getMeid()",
            frameworkClass = "android.telephony.TelephonyManager",
            targetMethodOrField = "getMeid() / getMeid(int)",
            configKey = NPatchConfig.KEY_IMEI,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts TelephonyManager MEID queries."
        ),
        SupportedApiDefinition(
            id = "telephony_sim_serial",
            name = "TelephonyManager.getSimSerialNumber()",
            frameworkClass = "android.telephony.TelephonyManager",
            targetMethodOrField = "getSimSerialNumber()",
            configKey = NPatchConfig.KEY_SERIAL,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts SIM Serial Number queries."
        ),
        SupportedApiDefinition(
            id = "telephony_subscriber_id",
            name = "TelephonyManager.getSubscriberId()",
            frameworkClass = "android.telephony.TelephonyManager",
            targetMethodOrField = "getSubscriberId()",
            configKey = NPatchConfig.KEY_IMEI,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts Subscriber ID (IMSI) queries."
        ),
        SupportedApiDefinition(
            id = "wifi_mac_address",
            name = "WifiInfo.getMacAddress()",
            frameworkClass = "android.net.wifi.WifiInfo",
            targetMethodOrField = "getMacAddress()",
            configKey = NPatchConfig.KEY_MAC,
            isDynamic = true,
            requiresProcessRestart = false,
            description = "Intercepts WifiInfo MAC address queries."
        )
    )
}
