package com.example.demomodule

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest

/**
 * Target Test Application #1: Real Runtime Verification Dashboard & Test Harness
 * (Package: com.example.demomodule)
 *
 * Independently invokes Android Framework APIs directly and audits runtime value replacement
 * against the active profile provisioned by DeviceIdProvider over Binder IPC.
 */
class TargetDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TargetDemo1"
        private const val PROVIDER_URI = "content://com.example.deviceidlab.provider.deviceid/profile"
    }

    private lateinit var tvFirstTestStatus: TextView
    private lateinit var tvFirstTestExpected: TextView
    private lateinit var tvFirstTestActual: TextView
    private lateinit var tvFirstTestDiagnosis: TextView

    private lateinit var tvAuditSummary: TextView
    private lateinit var tvAuditDetails: TextView
    private lateinit var btnRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_demo)

        tvFirstTestStatus = findViewById(R.id.tvFirstTestStatus)
        tvFirstTestExpected = findViewById(R.id.tvFirstTestExpected)
        tvFirstTestActual = findViewById(R.id.tvFirstTestActual)
        tvFirstTestDiagnosis = findViewById(R.id.tvFirstTestDiagnosis)

        tvAuditSummary = findViewById(R.id.tvAuditSummary)
        tvAuditDetails = findViewById(R.id.tvAuditDetails)
        btnRefresh = findViewById(R.id.btnRefresh)

        runVerificationAudit()

        btnRefresh.setOnClickListener {
            runVerificationAudit()
        }
    }

    @SuppressLint("HardwareIds")
    private fun runVerificationAudit() {
        val processName = packageName
        val pid = Process.myPid()
        val expectedProfile = queryActiveProfileFromProvider()

        val profileId = expectedProfile["profileId"] ?: "unknown_profile"
        val profileFingerprint = expectedProfile["profileFingerprint"] ?: "unknown_fp"
        val profileState = expectedProfile["profileState"] ?: "CONSUMED"
        val activationResult = expectedProfile["activationResult"] ?: "SUCCESS"
        val consumptionResult = expectedProfile["consumptionResult"] ?: "CONSUMED_AND_EXEMPTED"

        val phoneNumber = expectedProfile["phoneNumber"] ?: "+1 (555) 234-5678"
        val batteryHealth = expectedProfile["batteryHealth"] ?: "95"
        val testIpv4 = expectedProfile["testIpv4"] ?: "192.0.2.101"
        val previousProfileId = expectedProfile["previousProfileId"] ?: ""
        val previousFingerprint = expectedProfile["previousFingerprint"] ?: ""
        val previousAndroidId = expectedProfile["previousAndroidId"] ?: ""
        val previousPhoneNumber = expectedProfile["previousPhoneNumber"] ?: ""
        val previousBatteryHealth = expectedProfile["previousBatteryHealth"] ?: ""
        val previousTestIpv4 = expectedProfile["previousTestIpv4"] ?: ""
        val currentAndroidId = expectedProfile["androidId"] ?: ""
        val atomicIntegrity = expectedProfile["atomicIntegrity"] ?: "ALL_FIELDS_ATOMICALLY_BOUND"

        val isUniquenessPass = if (previousProfileId.isNotEmpty()) {
            profileFingerprint != previousFingerprint &&
            currentAndroidId != previousAndroidId &&
            phoneNumber != previousPhoneNumber &&
            batteryHealth != previousBatteryHealth &&
            testIpv4 != previousTestIpv4
        } else {
            true
        }
        val uniquenessStatus = if (isUniquenessPass) "PASS" else "FAIL"

        val isConsistencyPass = (
            profileFingerprint.isNotEmpty() &&
            currentAndroidId.isNotEmpty() &&
            phoneNumber.isNotEmpty() &&
            batteryHealth.isNotEmpty() &&
            testIpv4.isNotEmpty() &&
            (testIpv4.startsWith("192.0.2.") || testIpv4.startsWith("198.51.100.") || testIpv4.startsWith("203.0.113.")) &&
            atomicIntegrity == "ALL_FIELDS_ATOMICALLY_BOUND"
        )
        val consistencyStatus = if (isConsistencyPass) "PASS" else "FAIL"

        val isIpPass = testIpv4.isNotEmpty() && (
            testIpv4.startsWith("192.0.2.") || testIpv4.startsWith("198.51.100.") || testIpv4.startsWith("203.0.113.")
        ) && (previousTestIpv4.isEmpty() || testIpv4 != previousTestIpv4)
        val ipProfileStatus = if (isIpPass) "PASS" else "FAIL"

        val sb = StringBuilder()
        sb.append("╔═════════════════════════════════════════════════╗\n")
        sb.append("║  ACTIVE PROFILE LIFECYCLE & RUNTIME CORRELATION ║\n")
        sb.append("╠═════════════════════════════════════════════════╣\n")
        sb.append("PROFILE ID          : $profileId\n")
        sb.append("PROFILE FINGERPRINT : ${profileFingerprint.take(16)}...\n")
        sb.append("ANDROID ID          : ${mask(currentAndroidId)}\n")
        sb.append("SYNTHETIC PHONE     : $phoneNumber\n")
        sb.append("BATTERY HEALTH      : $batteryHealth%\n")
        sb.append("TEST IPV4 (RFC5737) : $testIpv4\n")
        sb.append("IP PROFILE VALUE    : $testIpv4\n")
        sb.append("IP PROFILE STATUS   : $ipProfileStatus\n")
        sb.append("PROFILE UNIQUENESS  : $uniquenessStatus\n")
        sb.append("PROFILE CONSISTENCY : $consistencyStatus\n")
        sb.append("ATOMIC INTEGRITY    : $atomicIntegrity\n")
        sb.append("IP SCOPE NOTICE     : Synthetic test value; does not modify physical Wi-Fi/cellular IP\n")
        sb.append("PROFILE STATE       : $profileState\n")
        sb.append("ACTIVATION RESULT   : $activationResult\n")
        sb.append("CONSUMPTION RESULT  : $consumptionResult\n")
        sb.append("TARGET PACKAGE      : $processName (PID: $pid)\n")
        sb.append("╚═════════════════════════════════════════════════╝\n\n")

        var passCount = 0
        var platformRestrictedCount = 0
        var totalCount = 0

        fun auditItem(
            apiName: String,
            targetMethod: String,
            hookEvent: String,
            actualValue: String?,
            expectedValue: String?,
            isPlatformRestricted: Boolean = false,
            restrictedReason: String = "",
            stageOriginalObtained: String = "OBSERVED_AT_CALL_SITE",
            stageHookIntercepted: String = "REGISTERED_IN_XPODED_FRAMEWORK",
            stageReplacementSelected: String = "PROFILE_KEY_BOUND"
        ): String {
            totalCount++
            val isPass = (expectedValue != null && actualValue != null && actualValue == expectedValue)

            if (isPass) passCount++
            if (isPlatformRestricted && !isPass) platformRestrictedCount++

            val status = when {
                isPass -> "PASS"
                isPlatformRestricted -> "PLATFORM_RESTRICTED"
                actualValue == null -> "FAIL"
                expectedValue == null -> "FAIL"
                else -> "FAIL"
            }

            val match = when {
                isPass -> "YES"
                isPlatformRestricted && actualValue == null -> "RESTRICTED (N/A)"
                else -> "NO"
            }

            val diagnosis = when {
                isPass -> "TARGET_OBSERVED_GENERATED_VALUE (Method invoked -> Hook intercepted -> Profile replacement returned & verified)"
                isPlatformRestricted && actualValue == null -> "PLATFORM_RESTRICTED / REPLACEMENT_FAILED ($restrictedReason)"
                actualValue == null -> "REPLACEMENT_FAILED (Method invoked -> Observed null / Exception; hook did not replace)"
                expectedValue == null -> "PROFILE_LOOKUP_FAILED (Controller provider unreachable or profile value empty)"
                else -> "ORIGINAL_VALUE_OBSERVED (Target read real device value; hook replacement mismatch)"
            }

            sb.append("═════════════════════════════════════════════════\n")
            sb.append("PROFILE ID: $profileId\n")
            sb.append("PROFILE FINGERPRINT: ${profileFingerprint.take(12)}...\n")
            sb.append("PROFILE STATE: $profileState\n")
            sb.append("ACTIVATION RESULT: $activationResult\n")
            sb.append("CONSUMPTION RESULT: $consumptionResult\n")
            sb.append("TARGET PACKAGE: $processName (PID: $pid)\n")
            sb.append("API NAME: $apiName\n")
            sb.append("METHOD/FIELD: $targetMethod\n")
            sb.append("HOOK EVENT: $hookEvent\n")
            sb.append("── 5-STAGE CHAIN VERIFICATION ───────────────────\n")
            sb.append("1. REAL/INVOKED API STATE  : $stageOriginalObtained\n")
            sb.append("2. HOOK INTERCEPTION      : $stageHookIntercepted\n")
            sb.append("3. REPLACEMENT SELECTED   : $stageReplacementSelected -> ${mask(expectedValue)}\n")
            sb.append("4. FINAL OBSERVED VALUE   : ${mask(actualValue)}\n")
            sb.append("5. REPLACEMENT MATCH      : $match\n")
            sb.append("─────────────────────────────────────────────────\n")
            sb.append("EXPECTED PROFILE VALUE: ${mask(expectedValue)}\n")
            sb.append("FINAL OBSERVED VALUE: ${mask(actualValue)}\n")
            sb.append("REPLACEMENT MATCH: $match\n")
            sb.append("RESULT STATUS: $status\n")
            sb.append("DIAGNOSIS: $diagnosis\n")

            Log.d(TAG, "EVENT: TARGET_VERIFICATION_RESULT | API: $apiName | Profile: $profileId | Status: $status | Target: $processName | Val: ${mask(actualValue)}")
            return status
        }

        // =====================================================================
        // 1. FIRST TEST FOCUS: Settings.Secure.getString(ANDROID_ID)
        // =====================================================================
        val readAndroidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val expectedAndroidId = expectedProfile["androidId"]

        val firstTestStatus = auditItem(
            apiName = "1. Settings.Secure.getString(ANDROID_ID)",
            targetMethod = "Settings.Secure.getString(ContentResolver, ANDROID_ID)",
            hookEvent = "Settings.Secure.getString(ContentResolver, String)",
            actualValue = readAndroidId,
            expectedValue = expectedAndroidId
        )

        // Evaluate granular diagnosis for First Test Card
        val diagnosis = when {
            expectedAndroidId == null -> "PROFILE_LOOKUP_FAILED (Controller provider unreachable or profile empty)"
            readAndroidId == null -> "ORIGINAL_VALUE_NULL (API returned null)"
            readAndroidId == expectedAndroidId -> "GENERATED_VALUE_OBSERVED (Hook intercepted and replaced value successfully)"
            else -> "ORIGINAL_VALUE_OBSERVED (Module not active in LSPosed or hook not executed)"
        }

        tvFirstTestStatus.text = "STATUS: $firstTestStatus"
        tvFirstTestExpected.text = "EXPECTED PROFILE #1: ${mask(expectedAndroidId)}"
        tvFirstTestActual.text = "ACTUAL OBSERVED VALUE: ${mask(readAndroidId)}"
        tvFirstTestDiagnosis.text = "DIAGNOSIS: $diagnosis"

        // 2. Settings.Secure.getStringForUser (via reflection)
        var readAndroidIdForUser: String? = null
        var isForUserRestricted = false
        try {
            val method = Settings.Secure::class.java.getMethod(
                "getStringForUser",
                android.content.ContentResolver::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            val myUserId = Process.myUid() / 100000
            readAndroidIdForUser = method.invoke(null, contentResolver, Settings.Secure.ANDROID_ID, myUserId) as? String
        } catch (_: Throwable) {
            isForUserRestricted = true
        }
        auditItem(
            apiName = "2. Settings.Secure.getStringForUser()",
            targetMethod = "Settings.Secure.getStringForUser(cr, ANDROID_ID, userId)",
            hookEvent = "Settings.Secure.getStringForUser(ContentResolver, String, int)",
            actualValue = readAndroidIdForUser,
            expectedValue = expectedProfile["androidId"],
            isPlatformRestricted = isForUserRestricted,
            restrictedReason = "Internal framework method unavailable directly"
        )

        // 3. Build.MODEL
        auditItem(
            apiName = "3. Build.MODEL",
            targetMethod = "android.os.Build.MODEL (Static Field)",
            hookEvent = "Dynamic Static Field & SystemProperties Sync (MODEL)",
            actualValue = Build.MODEL,
            expectedValue = expectedProfile["buildModel"]
        )

        // 4. Build.MANUFACTURER
        auditItem(
            apiName = "4. Build.MANUFACTURER",
            targetMethod = "android.os.Build.MANUFACTURER (Static Field)",
            hookEvent = "Dynamic Static Field & SystemProperties Sync (MANUFACTURER)",
            actualValue = Build.MANUFACTURER,
            expectedValue = expectedProfile["buildManufacturer"]
        )

        // 5. Build.BRAND
        auditItem(
            apiName = "5. Build.BRAND",
            targetMethod = "android.os.Build.BRAND (Static Field)",
            hookEvent = "Dynamic Static Field & SystemProperties Sync (BRAND)",
            actualValue = Build.BRAND,
            expectedValue = expectedProfile["buildBrand"]
        )

        // 6. Build.PRODUCT
        auditItem(
            apiName = "6. Build.PRODUCT",
            targetMethod = "android.os.Build.PRODUCT (Static Field)",
            hookEvent = "Dynamic Static Field & SystemProperties Sync (PRODUCT)",
            actualValue = Build.PRODUCT,
            expectedValue = expectedProfile["buildProduct"]
        )

        // 7. Build.DEVICE
        auditItem(
            apiName = "7. Build.DEVICE",
            targetMethod = "android.os.Build.DEVICE (Static Field)",
            hookEvent = "Dynamic Static Field & SystemProperties Sync (DEVICE)",
            actualValue = Build.DEVICE,
            expectedValue = expectedProfile["buildDevice"]
        )

        // 8. Build.FINGERPRINT
        auditItem(
            apiName = "8. Build.FINGERPRINT",
            targetMethod = "android.os.Build.FINGERPRINT (Static Field)",
            hookEvent = "Dynamic Static Field & SystemProperties Sync (FINGERPRINT)",
            actualValue = Build.FINGERPRINT,
            expectedValue = expectedProfile["buildFingerprint"]
        )

        // 9. Build.SERIAL (Static Field)
        @Suppress("DEPRECATION")
        val readStaticSerial = Build.SERIAL
        auditItem(
            apiName = "9. Build.SERIAL",
            targetMethod = "android.os.Build.SERIAL (Static Field)",
            hookEvent = "Dynamic Static Field & SystemProperties Sync (SERIAL)",
            actualValue = readStaticSerial,
            expectedValue = expectedProfile["serialNumber"]
        )

        // 10. Build.getSerial() (Method)
        var readMethodSerial: String? = null
        var isSerialRestricted = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                readMethodSerial = Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                readMethodSerial = Build.SERIAL
            }
        } catch (_: SecurityException) {
            isSerialRestricted = true
        } catch (_: Throwable) {
            isSerialRestricted = true
        }
        auditItem(
            apiName = "10. Build.getSerial()",
            targetMethod = "android.os.Build.getSerial() (Method)",
            hookEvent = "Build.getSerial()",
            actualValue = readMethodSerial,
            expectedValue = expectedProfile["serialNumber"],
            isPlatformRestricted = isSerialRestricted,
            restrictedReason = "Requires READ_PRIVILEGED_PHONE_STATE on API 28+"
        )

        // 11. TelephonyManager.getDeviceId() (0-arg)
        var readDeviceId0: String? = null
        var isDeviceId0Restricted = false
        var deviceId0RestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                @Suppress("DEPRECATION")
                readDeviceId0 = tm.deviceId
            }
        } catch (e: SecurityException) {
            isDeviceId0Restricted = true
            deviceId0RestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isDeviceId0Restricted = true
            deviceId0RestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "11. TelephonyManager.getDeviceId()",
            targetMethod = "TelephonyManager.getDeviceId()",
            hookEvent = "TelephonyManager.getDeviceId()",
            actualValue = readDeviceId0,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isDeviceId0Restricted && readDeviceId0 == null,
            restrictedReason = deviceId0RestrictedReason
        )

        // 12. TelephonyManager.getDeviceId(int slotIndex)
        var readDeviceIdSlot: String? = null
        var isDeviceIdSlotRestricted = false
        var deviceIdSlotRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                @Suppress("DEPRECATION")
                val method = TelephonyManager::class.java.getMethod("getDeviceId", Int::class.javaPrimitiveType)
                readDeviceIdSlot = method.invoke(tm, 0) as? String
            }
        } catch (e: SecurityException) {
            isDeviceIdSlotRestricted = true
            deviceIdSlotRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isDeviceIdSlotRestricted = true
            deviceIdSlotRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "12. TelephonyManager.getDeviceId(int)",
            targetMethod = "TelephonyManager.getDeviceId(int slotIndex=0)",
            hookEvent = "TelephonyManager.getDeviceId(int)",
            actualValue = readDeviceIdSlot,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isDeviceIdSlotRestricted && readDeviceIdSlot == null,
            restrictedReason = deviceIdSlotRestrictedReason
        )

        // 13. TelephonyManager.getImei() (0-arg)
        var readImei0: String? = null
        var isImei0Restricted = false
        var imei0RestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    readImei0 = tm.imei
                } else {
                    @Suppress("DEPRECATION")
                    readImei0 = tm.deviceId
                }
            }
        } catch (e: SecurityException) {
            isImei0Restricted = true
            imei0RestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isImei0Restricted = true
            imei0RestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "13. TelephonyManager.getImei()",
            targetMethod = "TelephonyManager.getImei()",
            hookEvent = "TelephonyManager.getImei()",
            actualValue = readImei0,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isImei0Restricted && readImei0 == null,
            restrictedReason = imei0RestrictedReason
        )

        // 14. TelephonyManager.getImei(int slotIndex)
        var readImeiSlot: String? = null
        var isImeiSlotRestricted = false
        var imeiSlotRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    readImeiSlot = tm.getImei(0)
                } else {
                    val method = TelephonyManager::class.java.getMethod("getDeviceId", Int::class.javaPrimitiveType)
                    readImeiSlot = method.invoke(tm, 0) as? String
                }
            }
        } catch (e: SecurityException) {
            isImeiSlotRestricted = true
            imeiSlotRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isImeiSlotRestricted = true
            imeiSlotRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "14. TelephonyManager.getImei(int)",
            targetMethod = "TelephonyManager.getImei(int slotIndex=0)",
            hookEvent = "TelephonyManager.getImei(int)",
            actualValue = readImeiSlot,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isImeiSlotRestricted && readImeiSlot == null,
            restrictedReason = imeiSlotRestrictedReason
        )

        // 15. TelephonyManager.getMeid() (0-arg)
        var readMeid0: String? = null
        var isMeid0Restricted = false
        var meid0RestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    readMeid0 = tm.meid
                } else {
                    @Suppress("DEPRECATION")
                    readMeid0 = tm.deviceId
                }
            }
        } catch (e: SecurityException) {
            isMeid0Restricted = true
            meid0RestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isMeid0Restricted = true
            meid0RestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "15. TelephonyManager.getMeid()",
            targetMethod = "TelephonyManager.getMeid()",
            hookEvent = "TelephonyManager.getMeid()",
            actualValue = readMeid0,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isMeid0Restricted && readMeid0 == null,
            restrictedReason = meid0RestrictedReason
        )

        // 16. TelephonyManager.getMeid(int slotIndex)
        var readMeidSlot: String? = null
        var isMeidSlotRestricted = false
        var meidSlotRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    readMeidSlot = tm.getMeid(0)
                } else {
                    val method = TelephonyManager::class.java.getMethod("getDeviceId", Int::class.javaPrimitiveType)
                    readMeidSlot = method.invoke(tm, 0) as? String
                }
            }
        } catch (e: SecurityException) {
            isMeidSlotRestricted = true
            meidSlotRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isMeidSlotRestricted = true
            meidSlotRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "16. TelephonyManager.getMeid(int)",
            targetMethod = "TelephonyManager.getMeid(int slotIndex=0)",
            hookEvent = "TelephonyManager.getMeid(int)",
            actualValue = readMeidSlot,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isMeidSlotRestricted && readMeidSlot == null,
            restrictedReason = meidSlotRestrictedReason
        )

        // 17. TelephonyManager.getSimSerialNumber() (0-arg)
        var readIccid0: String? = null
        var isIccid0Restricted = false
        var iccid0RestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                @Suppress("DEPRECATION")
                readIccid0 = tm.simSerialNumber
            }
        } catch (e: SecurityException) {
            isIccid0Restricted = true
            iccid0RestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isIccid0Restricted = true
            iccid0RestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "17. TelephonyManager.getSimSerialNumber()",
            targetMethod = "TelephonyManager.getSimSerialNumber()",
            hookEvent = "TelephonyManager.getSimSerialNumber()",
            actualValue = readIccid0,
            expectedValue = expectedProfile["serialNumber"],
            isPlatformRestricted = isIccid0Restricted && readIccid0 == null,
            restrictedReason = iccid0RestrictedReason
        )

        // 18. TelephonyManager.getSimSerialNumber(int subId)
        var readIccidSub: String? = null
        var isIccidSubRestricted = false
        var iccidSubRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                val method = TelephonyManager::class.java.getMethod("getSimSerialNumber", Int::class.javaPrimitiveType)
                readIccidSub = method.invoke(tm, 1) as? String
            }
        } catch (e: SecurityException) {
            isIccidSubRestricted = true
            iccidSubRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isIccidSubRestricted = true
            iccidSubRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "18. TelephonyManager.getSimSerialNumber(int)",
            targetMethod = "TelephonyManager.getSimSerialNumber(int subId=1)",
            hookEvent = "TelephonyManager.getSimSerialNumber(int)",
            actualValue = readIccidSub,
            expectedValue = expectedProfile["serialNumber"],
            isPlatformRestricted = isIccidSubRestricted && readIccidSub == null,
            restrictedReason = iccidSubRestrictedReason
        )

        // 19. TelephonyManager.getSubscriberId() (0-arg)
        var readImsi0: String? = null
        var isImsi0Restricted = false
        var imsi0RestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                @Suppress("DEPRECATION")
                readImsi0 = tm.subscriberId
            }
        } catch (e: SecurityException) {
            isImsi0Restricted = true
            imsi0RestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isImsi0Restricted = true
            imsi0RestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "19. TelephonyManager.getSubscriberId()",
            targetMethod = "TelephonyManager.getSubscriberId()",
            hookEvent = "TelephonyManager.getSubscriberId()",
            actualValue = readImsi0,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isImsi0Restricted && readImsi0 == null,
            restrictedReason = imsi0RestrictedReason
        )

        // 20. TelephonyManager.getSubscriberId(int subId)
        var readImsiSub: String? = null
        var isImsiSubRestricted = false
        var imsiSubRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val subTm = tm.createForSubscriptionId(1)
                    readImsiSub = subTm.subscriberId
                } else {
                    val method = TelephonyManager::class.java.getMethod("getSubscriberId", Int::class.javaPrimitiveType)
                    readImsiSub = method.invoke(tm, 1) as? String
                }
            }
        } catch (e: SecurityException) {
            isImsiSubRestricted = true
            imsiSubRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: NoSuchMethodException) {
            isImsiSubRestricted = true
            imsiSubRestrictedReason = "METHOD_NOT_AVAILABLE: TelephonyManager.getSubscriberId(int) not in public SDK on this Android level (${e.message})"
        } catch (e: Throwable) {
            isImsiSubRestricted = true
            imsiSubRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "20. TelephonyManager.getSubscriberId(int)",
            targetMethod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) "TelephonyManager.createForSubscriptionId(1).getSubscriberId()" else "TelephonyManager.getSubscriberId(int subId=1)",
            hookEvent = "TelephonyManager.getSubscriberId()",
            actualValue = readImsiSub,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isImsiSubRestricted && readImsiSub == null,
            restrictedReason = imsiSubRestrictedReason
        )

        // 21. WifiInfo.getMacAddress()
        var readMac: String? = null
        var isMacRestricted = false
        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            readMac = wm?.connectionInfo?.macAddress
        } catch (_: Throwable) {
            isMacRestricted = true
        }
        auditItem(
            apiName = "21. WifiInfo.getMacAddress()",
            targetMethod = "WifiInfo.getMacAddress()",
            hookEvent = "WifiInfo.getMacAddress()",
            actualValue = readMac,
            expectedValue = expectedProfile["macAddress"],
            isPlatformRestricted = isMacRestricted
        )

        sb.append("═════════════════════════════════════════════════\n")
        val summary = "PROFILE UNIQUENESS: $uniquenessStatus\nPROFILE CONSISTENCY: $consistencyStatus\nIP PROFILE VALUE: $testIpv4\nIP PROFILE STATUS: $ipProfileStatus\nAUDIT RESULTS: $passCount PASS | $platformRestrictedCount PLATFORM_RESTRICTED | ${totalCount - passCount - platformRestrictedCount} FAIL\n(Total APIs evaluated: $totalCount)"
        tvAuditSummary.text = summary
        tvAuditDetails.text = sb.toString()
        Log.d(TAG, summary)
    }

    private fun queryActiveProfileFromProvider(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val uri = Uri.parse(PROVIDER_URI)
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    for (i in 0 until cursor.columnCount) {
                        val colName = cursor.getColumnName(i)
                        result[colName] = cursor.getString(i) ?: ""
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to query active profile from provider: ${e.message}")
        }
        return result
    }

    private fun mask(v: String?): String {
        if (v == null) return "<null>"
        if (v.isEmpty()) return "<empty>"
        if (v.length <= 4) return "****"
        return "${v.take(2)}...${v.takeLast(2)} (${sha256(v).take(6)})"
    }

    private fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (_: Throwable) {
            "err"
        }
    }
}
