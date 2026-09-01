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

        val sb = StringBuilder()
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
            restrictedReason: String = ""
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
                isPass -> "TARGET_OBSERVED_GENERATED_VALUE (Method invoked -> Hook intercepted -> Profile value returned & verified)"
                isPlatformRestricted && actualValue == null -> "PLATFORM_RESTRICTED / REPLACEMENT_FAILED ($restrictedReason)"
                actualValue == null -> "REPLACEMENT_FAILED (Method invoked -> Observed null / Exception; hook did not replace)"
                expectedValue == null -> "PROFILE_LOOKUP_FAILED (Controller provider unreachable or profile value empty)"
                else -> "ORIGINAL_VALUE_OBSERVED (Target read real device value; hook replacement mismatch)"
            }

            sb.append("═════════════════════════════════════════════════\n")
            sb.append("API: $apiName\n")
            sb.append("METHOD/FIELD: $targetMethod\n")
            sb.append("HOOK EVENT: $hookEvent\n")
            sb.append("TARGET PROCESS: $processName (PID: $pid)\n")
            sb.append("EXPECTED PROFILE VALUE (MASKED): ${mask(expectedValue)}\n")
            sb.append("ACTUAL OBSERVED VALUE  (MASKED): ${mask(actualValue)}\n")
            sb.append("VALUE MATCH: $match\n")
            sb.append("RESULT STATUS: $status\n")
            sb.append("DIAGNOSIS: $diagnosis\n")

            Log.d(TAG, "EVENT: TARGET_VERIFICATION_RESULT | API: $apiName | Status: $status | Target: $processName | Val: ${mask(actualValue)}")
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

        // 11. TelephonyManager.getDeviceId() & getDeviceId(0)
        var readDeviceId: String? = null
        var isDeviceIdRestricted = false
        var deviceIdRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                @Suppress("DEPRECATION")
                readDeviceId = tm.deviceId
            }
        } catch (e: SecurityException) {
            isDeviceIdRestricted = true
            deviceIdRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isDeviceIdRestricted = true
            deviceIdRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "11. TelephonyManager.getDeviceId()",
            targetMethod = "TelephonyManager.getDeviceId()",
            hookEvent = "TelephonyManager.getDeviceId() / (int)",
            actualValue = readDeviceId,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isDeviceIdRestricted && readDeviceId == null,
            restrictedReason = deviceIdRestrictedReason
        )

        // 12. TelephonyManager.getImei() & getImei(0)
        var readImei: String? = null
        var isImeiRestricted = false
        var imeiRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    readImei = tm.imei
                } else {
                    @Suppress("DEPRECATION")
                    readImei = tm.deviceId
                }
            }
        } catch (e: SecurityException) {
            isImeiRestricted = true
            imeiRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isImeiRestricted = true
            imeiRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "12. TelephonyManager.getImei()",
            targetMethod = "TelephonyManager.getImei()",
            hookEvent = "TelephonyManager.getImei() / (int)",
            actualValue = readImei,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isImeiRestricted && readImei == null,
            restrictedReason = imeiRestrictedReason
        )

        // 13. TelephonyManager.getMeid()
        var readMeid: String? = null
        var isMeidRestricted = false
        var meidRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    readMeid = tm.meid
                } else {
                    @Suppress("DEPRECATION")
                    readMeid = tm.deviceId
                }
            }
        } catch (e: SecurityException) {
            isMeidRestricted = true
            meidRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isMeidRestricted = true
            meidRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "13. TelephonyManager.getMeid()",
            targetMethod = "TelephonyManager.getMeid()",
            hookEvent = "TelephonyManager.getMeid() / (int)",
            actualValue = readMeid,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isMeidRestricted && readMeid == null,
            restrictedReason = meidRestrictedReason
        )

        // 14. TelephonyManager.getSimSerialNumber()
        var readIccid: String? = null
        var isIccidRestricted = false
        var iccidRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                @Suppress("DEPRECATION")
                readIccid = tm.simSerialNumber
            }
        } catch (e: SecurityException) {
            isIccidRestricted = true
            iccidRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isIccidRestricted = true
            iccidRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "14. TelephonyManager.getSimSerialNumber()",
            targetMethod = "TelephonyManager.getSimSerialNumber()",
            hookEvent = "TelephonyManager.getSimSerialNumber() / (int)",
            actualValue = readIccid,
            expectedValue = expectedProfile["serialNumber"],
            isPlatformRestricted = isIccidRestricted && readIccid == null,
            restrictedReason = iccidRestrictedReason
        )

        // 15. TelephonyManager.getSubscriberId()
        var readImsi: String? = null
        var isImsiRestricted = false
        var imsiRestrictedReason = ""
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                @Suppress("DEPRECATION")
                readImsi = tm.subscriberId
            }
        } catch (e: SecurityException) {
            isImsiRestricted = true
            imsiRestrictedReason = "SecurityException: ${e.message}"
        } catch (e: Throwable) {
            isImsiRestricted = true
            imsiRestrictedReason = "${e.javaClass.simpleName}: ${e.message}"
        }
        auditItem(
            apiName = "15. TelephonyManager.getSubscriberId()",
            targetMethod = "TelephonyManager.getSubscriberId()",
            hookEvent = "TelephonyManager.getSubscriberId() / (int)",
            actualValue = readImsi,
            expectedValue = expectedProfile["imei"],
            isPlatformRestricted = isImsiRestricted && readImsi == null,
            restrictedReason = imsiRestrictedReason
        )

        // 16. WifiInfo.getMacAddress()
        var readMac: String? = null
        var isMacRestricted = false
        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            readMac = wm?.connectionInfo?.macAddress
        } catch (_: Throwable) {
            isMacRestricted = true
        }
        auditItem(
            apiName = "16. WifiInfo.getMacAddress()",
            targetMethod = "WifiInfo.getMacAddress()",
            hookEvent = "WifiInfo.getMacAddress()",
            actualValue = readMac,
            expectedValue = expectedProfile["macAddress"],
            isPlatformRestricted = isMacRestricted
        )

        sb.append("═════════════════════════════════════════════════\n")
        val summary = "AUDIT RESULTS: $passCount PASS | $platformRestrictedCount PLATFORM_RESTRICTED | ${totalCount - passCount - platformRestrictedCount} FAIL\n(Total APIs evaluated: $totalCount)"
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
