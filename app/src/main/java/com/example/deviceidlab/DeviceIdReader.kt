package com.example.deviceidlab

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import com.example.deviceidlab.demo.DeviceIdHookDemo
import com.example.deviceidlab.demo.InterceptionBridge
import com.example.deviceidlab.hook.NPatchAuditManager
import com.example.deviceidlab.hook.NPatchConfig

/**
 * Result model representing device identifier queries from official Android APIs.
 */
data class RealIdResult(
    val value: String,
    val isRestricted: Boolean = false,
    val statusDetail: String = ""
)

/**
 * Result model representing real-time ID injection testing verification.
 */
data class InjectionTestResult(
    val originalId: String,
    val currentId: String,
    val injectedId: String,
    val isSuccess: Boolean,
    val hookStatus: String,
    val targetPackage: String = "com.example.deviceidlab",
    val failureReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Comprehensive NPatch 1.0.7 runtime injection verification model.
 */
data class NpatchVerificationDetails(
    val moduleDetected: Boolean,
    val targetPackage: String,
    val targetProcess: String,
    val hookEntryStatus: String,
    val hookInstallationStatus: String,
    val canaryStatus: String,
    val lastHookTimestamp: Long,
    val finalResult: String,
    val isVerified: Boolean
)

/**
 * Reads device identifiers from official Android platform APIs.
 *
 * Demonstrates:
 * 1. Settings.Secure.ANDROID_ID (Standard per-app-signing-key ID since Android 8.0 Oreo)
 * 2. TelephonyManager.getDeviceId() / getImei() (Strictly restricted on modern Android)
 */
object DeviceIdReader {

    private const val TAG = "DeviceIdReader"

    /**
     * Diagnostic canary method hooked by [NPatchHookEntry] when active under NPatch 1.0.7 / LSPatch / Xposed.
     * Unhooked default returns false. When running inside a patched process with the NPatch module loaded,
     * the framework method replacement dynamically returns true, proving real framework bytecode interception.
     */
    @JvmStatic
    fun isNpatchHookActive(): Boolean {
        return false
    }

    /**
     * Performs a comprehensive runtime verification check for NPatch 1.0.7 module injection.
     *
     * Validates:
     * - Module presence & initialization
     * - Target package & process observation
     * - Framework method replacement on [isNpatchHookActive] (canary)
     * - Actual hook installation and invocation timestamps
     */
    fun verifyNpatchInjection(context: Context, targetPackage: String): NpatchVerificationDetails {
        val canaryActive = isNpatchHookActive()
        val auditRecord = NPatchAuditManager.getAuditRecord(context, targetPackage, canaryActive)

        val isVerified = auditRecord.isInjectionVerified
        val finalResultStr = if (isVerified) "INJECTION VERIFIED" else "INJECTION NOT DETECTED"

        val logMsg = if (isVerified) {
            "[$TAG] [NPATCH VERIFIED] Target process: '${auditRecord.targetProcess}', pkg: '${auditRecord.targetPackage}', canary: ${auditRecord.canaryStatus}, result: $finalResultStr"
        } else {
            "[$TAG] [NPATCH FAILURE] No framework hook callback or canary bytecode replacement detected for target pkg '$targetPackage'"
        }

        Log.i(TAG, logMsg)
        safeXposedLog(logMsg)

        return NpatchVerificationDetails(
            moduleDetected = auditRecord.moduleDetected,
            targetPackage = if (auditRecord.targetPackage != "None") auditRecord.targetPackage else targetPackage,
            targetProcess = auditRecord.targetProcess,
            hookEntryStatus = auditRecord.hookEntryStatus,
            hookInstallationStatus = auditRecord.hookInstallationStatus,
            canaryStatus = auditRecord.canaryStatus,
            lastHookTimestamp = auditRecord.lastHookTimestamp,
            finalResult = finalResultStr,
            isVerified = isVerified
        )
    }

    /**
     * Reads the real Android ID using Settings.Secure.
     *
     * Format: 64-bit number (as a 16-character hex string) unique to each
     * combination of app-signing key, user, and device.
     */
    fun readAndroidId(context: Context): RealIdResult {
        return try {
            val androidId = DeviceIdHookDemo.interceptSettingsSecureGetString(
                callerPackage = context.packageName,
                settingName = Settings.Secure.ANDROID_ID,
                originalProvider = {
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                }
            ) ?: Settings.Secure.getString(
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
     * Retrieves the currently configured Injected Android ID from persistent SharedPreferences or DeviceIdProvider.
     */
    fun getSavedInjectedAndroidId(context: Context): String {
        val providerId = com.example.deviceidlab.provider.DeviceIdProvider.activeAndroidTestId
        if (providerId.isNotBlank() && providerId != "NPATCH_ANDROID_001" && providerId != "NPATCH_TEST_001") return providerId

        return try {
            val prefs = context.getSharedPreferences(NPatchConfig.PREF_FILE, Context.MODE_PRIVATE)
            val saved = prefs.getString(NPatchConfig.KEY_ACTIVE_ANDROID_ID, null)
            if (!saved.isNullOrEmpty()) saved else (InterceptionBridge.activeSimulatedAndroidId.value ?: providerId)
        } catch (_: Throwable) {
            InterceptionBridge.activeSimulatedAndroidId.value ?: providerId
        }
    }

    /**
     * Retrieves the currently configured Injected Telephony ID from persistent SharedPreferences or DeviceIdProvider.
     */
    fun getSavedInjectedTelephonyId(context: Context): String {
        val providerId = com.example.deviceidlab.provider.DeviceIdProvider.activeTelephonyTestId
        if (providerId.isNotBlank() && providerId != "NPATCH_TELEPHONY_001") return providerId

        return try {
            val prefs = context.getSharedPreferences(NPatchConfig.PREF_FILE, Context.MODE_PRIVATE)
            val saved = prefs.getString(NPatchConfig.KEY_ACTIVE_TELEPHONY_ID, null)
            if (!saved.isNullOrEmpty()) saved else (InterceptionBridge.activeSimulatedTelephonyId.value ?: providerId)
        } catch (_: Throwable) {
            InterceptionBridge.activeSimulatedTelephonyId.value ?: providerId
        }
    }

    /**
     * Persists the Injected IDs to SharedPreferences and DeviceIdProvider so NPatch
     * hooks in all target processes dynamically read it via ContentResolver.
     */
    fun saveInjectedIds(context: Context, androidId: String, telephonyId: String) {
        com.example.deviceidlab.provider.DeviceIdProvider.updateTestIds(context, androidId, telephonyId)
        try {
            val prefs = context.getSharedPreferences(NPatchConfig.PREF_FILE, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(NPatchConfig.KEY_ACTIVE_ANDROID_ID, androidId)
                .putString(NPatchConfig.KEY_ACTIVE_TELEPHONY_ID, telephonyId)
                .putBoolean(NPatchConfig.KEY_INTERCEPTION_ENABLED, true)
                .commit()
        } catch (_: Throwable) {}
        InterceptionBridge.updateActiveSimulatedIds(androidId, telephonyId)
    }

    fun saveInjectedAndroidId(context: Context, id: String) {
        saveInjectedIds(context, id, getSavedInjectedTelephonyId(context))
    }

    fun saveInjectedTelephonyId(context: Context, id: String) {
        saveInjectedIds(context, getSavedInjectedAndroidId(context), id)
    }

    /**
     * Performs a live test of ID injection / hook substitution:
     * 1. Captures baseline ID directly from Settings.Secure
     * 2. Sets new randomized identity in interception configuration & SharedPreferences
     * 3. Queries Settings.Secure / target package
     * 4. Verifies whether the returned ANDROID_ID matches the injected value
     * 5. If hook is not active, provides the exact failure reason
     */
    fun performInjectionTest(
        context: Context,
        targetRandomId: String,
        targetPackage: String = context.packageName
    ): InjectionTestResult {
        if (targetRandomId.isBlank()) {
            return InjectionTestResult(
                originalId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "",
                currentId = "",
                injectedId = "",
                isSuccess = false,
                hookStatus = "INVALID_INPUT",
                targetPackage = targetPackage,
                failureReason = "Injected ID is empty. Please enter or generate a valid 16-character hex ID."
            )
        }

        // 1. Persist to SharedPreferences and InterceptionBridge
        saveInjectedAndroidId(context, targetRandomId)

        val isSelf = (targetPackage == context.packageName) || (targetPackage == "com.example.deviceidlab")

        if (isSelf) {
            val baseline = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_baseline"
            val isRealHook = isNpatchHookActive()
            val isSimBridge = InterceptionBridge.isInterceptionActive.value

            // Real query directly from Settings.Secure
            val currentId = if (isRealHook) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: baseline
            } else if (isSimBridge) {
                readAndroidId(context).value
            } else {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: baseline
            }

            val isSuccess = if (isRealHook) {
                (currentId == targetRandomId)
            } else if (isSimBridge) {
                (currentId == targetRandomId)
            } else {
                false
            }

            val hookStatus = if (isRealHook && isSuccess) {
                "REAL_HOOK_SUCCESS"
            } else if (isSimBridge && isSuccess) {
                "SIMULATION_ONLY"
            } else if (!isRealHook && !isSimBridge) {
                "TARGET_NOT_PATCHED"
            } else if (currentId == baseline) {
                "API_NOT_INTERCEPTED"
            } else {
                "RETURN_MISMATCH"
            }

            val failureReason = if (!isSuccess) {
                if (!isRealHook && !isSimBridge) {
                    "Returned Android ID matches baseline '$baseline'. NPatch bytecode hook is not active in process '$targetPackage'. Ensure APK is patched with NPatch 1.0.7 and module is enabled."
                } else if (currentId == baseline) {
                    "Returned Android ID matches baseline '$baseline'. Method was not intercepted."
                } else {
                    "Returned Android ID '$currentId' does not match the target injected ID '$targetRandomId'."
                }
            } else null

            Log.i(TAG, "[$TAG] [NPATCH STAGE: RETURN_VERIFY] Verification in process '$targetPackage': status=$hookStatus, realHook=$isRealHook, baseline='$baseline', current='$currentId', injected='$targetRandomId'")

            return InjectionTestResult(
                originalId = baseline,
                currentId = currentId,
                injectedId = targetRandomId,
                isSuccess = isSuccess,
                hookStatus = hookStatus,
                targetPackage = targetPackage,
                failureReason = failureReason
            )
        } else {
            // Target is an external application
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent == null) {
                Log.w(TAG, "[$TAG] [NPATCH STAGE: PROCESS_LOAD] Target package '$targetPackage' not installed")
                return InjectionTestResult(
                    originalId = "External Hardware Baseline",
                    currentId = "Unreachable (App not found)",
                    injectedId = targetRandomId,
                    isSuccess = false,
                    hookStatus = "NOT_INSTALLED",
                    targetPackage = targetPackage,
                    failureReason = "Target package '$targetPackage' is not installed on this device."
                )
            }

            // Launch target application to execute NPatch hook lifecycle in target process
            try {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to launch $targetPackage: ${t.message}")
            }

            val audit = NPatchAuditManager.getAuditRecord(context, targetPackage)
            val isSuccess = audit.isInjectionVerified

            val failureReason = if (!isSuccess) {
                "Target app '$targetPackage' was launched, but no NPatch 1.0.7 hook execution was detected in its process. Verify that the target APK was patched with NPatch 1.0.7 and this module is selected."
            } else null

            val hookStatus = if (isSuccess) "REAL_HOOK_SUCCESS" else "TARGET_NOT_PATCHED"
            Log.i(TAG, "[$TAG] [NPATCH STAGE: RETURN_VERIFY] External verification for '$targetPackage' (PID: ${audit.targetPid}): status=$hookStatus, isSuccess=$isSuccess")

            return InjectionTestResult(
                originalId = "Target Hardware Baseline",
                currentId = if (isSuccess) targetRandomId else "Baseline (Unhooked)",
                injectedId = targetRandomId,
                isSuccess = isSuccess,
                hookStatus = hookStatus,
                targetPackage = targetPackage,
                failureReason = failureReason
            )
        }
    }

    /**
     * Attempts to read the Telephony Device ID (IMEI/MEID) via [TelephonyManager].
     */
    @SuppressLint("HardwareIds")
    fun readTelephonyDeviceId(context: Context): RealIdResult {
        return try {
            val intercepted = DeviceIdHookDemo.interceptTelephonyGetDeviceId(
                callerPackage = context.packageName,
                originalProvider = { null }
            )
            if (!intercepted.isNullOrBlank()) {
                return RealIdResult(
                    value = intercepted,
                    isRestricted = false,
                    statusDetail = "Retrieved via TelephonyManager (NPatch Intercepted)"
                )
            }

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

    /**
     * Safely logs messages to XposedBridge without throwing ClassNotFoundException
     * when executing on unpatched host processes or local JVM test environments.
     */
    private fun safeXposedLog(message: String) {
        try {
            val clazz = Class.forName("de.robv.android.xposed.XposedBridge")
            val method = clazz.getMethod("log", String::class.java)
            method.invoke(null, message)
        } catch (_: Throwable) {
            // XposedBridge not present in current runtime classloader
        }
    }
}
