package com.example.deviceidlab

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

/**
 * Result model representing device identifier queries from official Android APIs.
 */
data class RealIdResult(
    val value: String,
    val isRestricted: Boolean = false,
    val statusDetail: String = ""
)

/**
 * Reads device identifiers from official Android platform APIs.
 *
 * Demonstrates:
 * 1. Settings.Secure.ANDROID_ID (Standard per-app-signing-key ID since Android 8.0 Oreo)
 * 2. TelephonyManager.getDeviceId() / getImei() (Strictly restricted on modern Android)
 */
object DeviceIdReader {

    /**
     * Reads the real Android ID using Settings.Secure.
     *
     * Format: 64-bit number (as a 16-character hex string) unique to each
     * combination of app-signing key, user, and device.
     */
    fun readAndroidId(context: Context): RealIdResult {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrEmpty()) {
                RealIdResult(
                    value = androidId,
                    isRestricted = false,
                    statusDetail = "Retrieved via Settings.Secure.ANDROID_ID"
                )
            } else {
                RealIdResult(
                    value = "Unavailable (null)",
                    isRestricted = true,
                    statusDetail = "Settings.Secure returned null"
                )
            }
        } catch (e: Exception) {
            RealIdResult(
                value = "Error: ${e.message ?: "Unknown"}",
                isRestricted = true,
                statusDetail = "Exception querying Settings.Secure"
            )
        }
    }

    /**
     * Attempts to read the Telephony Device ID (IMEI/MEID) via [TelephonyManager].
     *
     * Handles platform restrictions gracefully:
     * - Android 10+ (API 29+): Non-resettable hardware identifiers (IMEI/Serial) require
     *   the privileged permission `READ_PRIVILEGED_PHONE_STATE`, which is restricted to
     *   system apps and carrier apps. Third-party apps receive a [SecurityException].
     * - Safely catches all [SecurityException], [NoSuchMethodError], and [NullPointerException].
     */
    @SuppressLint("HardwareIds")
    fun readTelephonyDeviceId(context: Context): RealIdResult {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val mgr = telephonyManager ?: return RealIdResult(
                value = "Unavailable (No cellular hardware)",
                isRestricted = true,
                statusDetail = "TelephonyManager system service not present on this device"
            )

            // Modern Android (API 29+) explicitly restricts getDeviceId() / getImei()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val imei = mgr.imei
                    if (!imei.isNullOrBlank()) {
                        RealIdResult(
                            value = imei,
                            isRestricted = false,
                            statusDetail = "Retrieved via TelephonyManager.getImei()"
                        )
                    } else {
                        RealIdResult(
                            value = "Restricted (Android 10+)",
                            isRestricted = true,
                            statusDetail = "Requires READ_PRIVILEGED_PHONE_STATE (restricted to system/carrier apps)"
                        )
                    }
                } catch (se: SecurityException) {
                    RealIdResult(
                        value = "Restricted (SecurityException)",
                        isRestricted = true,
                        statusDetail = "Android 10+ blocked IMEI access: READ_PRIVILEGED_PHONE_STATE is restricted"
                    )
                } catch (e: Exception) {
                    RealIdResult(
                        value = "Restricted / Unavailable",
                        isRestricted = true,
                        statusDetail = "Hardware identifier access blocked: ${e.javaClass.simpleName}"
                    )
                }
            } else {
                // Legacy Android API < 29
                try {
                    @Suppress("DEPRECATION")
                    val deviceId = telephonyManager.deviceId
                    if (!deviceId.isNullOrBlank()) {
                        RealIdResult(
                            value = deviceId,
                            isRestricted = false,
                            statusDetail = "Retrieved via TelephonyManager.getDeviceId()"
                        )
                    } else {
                        RealIdResult(
                            value = "Unavailable",
                            isRestricted = true,
                            statusDetail = "TelephonyManager returned null or empty deviceId"
                        )
                    }
                } catch (se: SecurityException) {
                    RealIdResult(
                        value = "Permission denied",
                        isRestricted = true,
                        statusDetail = "READ_PHONE_STATE permission required for legacy API"
                    )
                }
            }
        } catch (e: Throwable) {
            RealIdResult(
                value = "Unavailable (${e.javaClass.simpleName})",
                isRestricted = true,
                statusDetail = e.message ?: "Failed to query TelephonyManager"
            )
        }
    }
}
