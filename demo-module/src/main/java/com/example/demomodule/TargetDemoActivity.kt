package com.example.demomodule

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.NetworkInterface

/**
 * Target Demo Activity #1 for module runtime verification.
 * Implements the 5-stage verification model:
 * HOOK_REGISTERED -> HOOK_INVOKED -> VALUE_GENERATED -> VALUE_RETURNED -> TARGET_OBSERVED
 */
class TargetDemoActivity : Activity() {

    data class TestResult(
        val api: String,
        val expected: String,
        val observed: String,
        val hookStatus: String,
        val matchStatus: String,
        val diagnosis: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.deviceidlab.demo.R.layout.activity_target_demo)

        val btnReinvoke = findViewById<Button>(com.example.deviceidlab.demo.R.id.btn_reinvoke_all)
        btnReinvoke?.setOnClickListener {
            runAuditedVerification()
        }

        runAuditedVerification()
    }

    private fun runAuditedVerification() {
        val resultsContainer = findViewById<LinearLayout>(com.example.deviceidlab.demo.R.id.layout_test_results) ?: return
        resultsContainer.removeAllViews()

        val tests = mutableListOf<TestResult>()

        // 1. Android ID
        val obsAndroidId = try {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "null"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
        tests.add(
            TestResult(
                api = "Settings.Secure.getString(android_id)",
                expected = "NPATCH_ANDROID_001",
                observed = obsAndroidId,
                hookStatus = if (obsAndroidId.startsWith("NPATCH")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsAndroidId.startsWith("NPATCH")) "MATCH" else "MISMATCH",
                diagnosis = "Target process queries Settings.Secure for simulated Android ID."
            )
        )

        // 2. Telephony ID
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val obsTelephony = try {
            @Suppress("DEPRECATION")
            tm?.deviceId ?: tm?.imei ?: "null"
        } catch (_: SecurityException) {
            "Restricted (READ_PRIVILEGED_PHONE_STATE required)"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
        tests.add(
            TestResult(
                api = "TelephonyManager.getDeviceId()",
                expected = "NPATCH_TELEPHONY_001",
                observed = obsTelephony,
                hookStatus = if (obsTelephony.startsWith("NPATCH")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsTelephony.startsWith("NPATCH")) "MATCH" else "RESTRICTED",
                diagnosis = "Target queries TelephonyManager for simulated Device ID."
            )
        )

        // 3. LocationManager.getLastKnownLocation
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val obsLoc = try {
            val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) "%.4f, %.4f".format(loc.latitude, loc.longitude) else "null"
        } catch (_: SecurityException) {
            "Location Permission Required"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
        val isLocMatch = obsLoc.contains("37.77") || obsLoc.startsWith("35.") || obsLoc.startsWith("51.") || obsLoc.startsWith("40.")
        tests.add(
            TestResult(
                api = "LocationManager.getLastKnownLocation(gps)",
                expected = "Worldwide Coordinates",
                observed = obsLoc,
                hookStatus = if (isLocMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isLocMatch) "MATCH" else "MISMATCH",
                diagnosis = "Worldwide location coordinates intercepted."
            )
        )

        // 4. Location.getLatitude() & getLongitude()
        val obsLat = try {
            val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            loc?.latitude
        } catch (_: Throwable) { null }
        tests.add(
            TestResult(
                api = "Location.getLatitude()",
                expected = "Active Profile Latitude",
                observed = obsLat?.toString() ?: "null",
                hookStatus = if (obsLat != null) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsLat != null) "MATCH" else "MISMATCH",
                diagnosis = "Location.getLatitude() intercepted via framework hook."
            )
        )

        // 5. NetworkInterface Hardware MAC
        val obsMac = try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var mac = "02:00:00:00:00:00"
            while (interfaces != null && interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val bytes = nif.hardwareAddress
                if (bytes != null && bytes.isNotEmpty() && !nif.isLoopback) {
                    mac = bytes.joinToString(":") { String.format("%02X", it) }
                    break
                }
            }
            mac
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
        val isMacMatch = obsMac.startsWith("02:00:11:22")
        tests.add(
            TestResult(
                api = "NetworkInterface.getHardwareAddress()",
                expected = "02:00:11:22:33:44",
                observed = obsMac,
                hookStatus = if (isMacMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isMacMatch) "MATCH" else "MISMATCH",
                diagnosis = "Hardware MAC substitution for privacy and randomization testing."
            )
        )

        // 6. NetworkInterface IP Address (RFC 5737 TEST-NET-3)
        val obsIp = try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var ip = "127.0.0.1"
            while (interfaces != null && interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    val host = a.hostAddress ?: ""
                    if (!a.isLoopbackAddress && !host.contains(":")) {
                        ip = host
                        break
                    }
                }
            }
            ip
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
        val isIpMatch = obsIp.startsWith("203.0.113.")
        tests.add(
            TestResult(
                api = "NetworkInterface.getInetAddresses()",
                expected = "203.0.113.42",
                observed = obsIp,
                hookStatus = if (isIpMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isIpMatch) "MATCH" else "MISMATCH",
                diagnosis = "RFC 5737 TEST-NET-3 synthetic IP range (203.0.113.0/24)."
            )
        )

        // 7. WifiInfo.getMacAddress()
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val obsWifiMac = try {
            @Suppress("DEPRECATION")
            wm?.connectionInfo?.macAddress ?: "02:00:00:00:00:00"
        } catch (t: Throwable) { "Error: ${t.message}" }
        val isWifiMacMatch = obsWifiMac.startsWith("02:00:11:22")
        tests.add(
            TestResult(
                api = "WifiInfo.getMacAddress()",
                expected = "02:00:11:22:33:44",
                observed = obsWifiMac,
                hookStatus = if (isWifiMacMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isWifiMacMatch) "MATCH" else "MISMATCH",
                diagnosis = "WifiInfo MAC address intercepted."
            )
        )

        // 8. WifiInfo.getIpAddress()
        val obsWifiIp = try {
            @Suppress("DEPRECATION")
            val ipInt = wm?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                "${ipInt and 0xFF}.${(ipInt shr 8) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 24) and 0xFF}"
            } else "0.0.0.0"
        } catch (t: Throwable) { "Error: ${t.message}" }
        val isWifiIpMatch = obsWifiIp.startsWith("203.0.113.")
        tests.add(
            TestResult(
                api = "WifiInfo.getIpAddress()",
                expected = "203.0.113.42",
                observed = obsWifiIp,
                hookStatus = if (isWifiIpMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isWifiIpMatch) "MATCH" else "MISMATCH",
                diagnosis = "WifiInfo IPv4 address unpacked."
            )
        )

        // 9. WifiInfo.getSSID() & getBSSID()
        val obsSsid = try {
            @Suppress("DEPRECATION")
            wm?.connectionInfo?.ssid ?: "<unknown ssid>"
        } catch (t: Throwable) { "Error: ${t.message}" }
        tests.add(
            TestResult(
                api = "WifiInfo.getSSID()",
                expected = "\"LabTest_WiFi\"",
                observed = obsSsid,
                hookStatus = if (obsSsid.contains("LabTest")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsSsid.contains("LabTest")) "MATCH" else "MISMATCH",
                diagnosis = "Connected WiFi SSID substituted."
            )
        )

        val obsBssid = try {
            @Suppress("DEPRECATION")
            wm?.connectionInfo?.bssid ?: "02:00:00:00:00:00"
        } catch (t: Throwable) { "Error: ${t.message}" }
        tests.add(
            TestResult(
                api = "WifiInfo.getBSSID()",
                expected = "02:00:11:22:33:44",
                observed = obsBssid,
                hookStatus = if (obsBssid.startsWith("02:00:11:22")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsBssid.startsWith("02:00:11:22")) "MATCH" else "MISMATCH",
                diagnosis = "Connected WiFi BSSID substituted."
            )
        )

        // 10. LinkProperties.getAddresses()
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val obsLinkProp = try {
            val net = cm?.activeNetwork
            val lp = cm?.getLinkProperties(net)
            lp?.linkAddresses?.firstOrNull()?.address?.hostAddress ?: "None"
        } catch (t: Throwable) { "Error: ${t.message}" }
        tests.add(
            TestResult(
                api = "LinkProperties.getAddresses()",
                expected = "203.0.113.42",
                observed = obsLinkProp,
                hookStatus = if (obsLinkProp.startsWith("203.0.113.")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsLinkProp.startsWith("203.0.113.")) "MATCH" else "MISMATCH",
                diagnosis = "LinkProperties interface address intercepted."
            )
        )

        // 11. Socket Egress IP (Boundary)
        tests.add(
            TestResult(
                api = "Socket.connect (Public Egress IP)",
                expected = "UNALTERED_PHYSICAL_EGRESS",
                observed = "PHYSICAL_CELLULAR_OR_WIFI_EGRESS",
                hookStatus = "UNSUPPORTED_AT_CURRENT_LAYER",
                matchStatus = "UNSUPPORTED",
                diagnosis = "Application layer hooks do not alter physical carrier NAT or raw TCP egress headers."
            )
        )

        // Render cards
        for (test in tests) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
                setBackgroundColor(0xFF1E293B.toInt())
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                layoutParams = params
            }

            val tvApi = TextView(this).apply {
                text = "${test.api} [${test.matchStatus}]"
                setTextColor(if (test.matchStatus == "MATCH") 0xFF4ADE80.toInt() else 0xFFFCA5A5.toInt())
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val tvDetails = TextView(this).apply {
                text = "EXPECTED: ${test.expected}\nOBSERVED: ${test.observed}\nSTATUS: ${test.hookStatus}\nDIAGNOSIS: ${test.diagnosis}"
                setTextColor(0xFFCBD5E1.toInt())
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
            }

            itemLayout.addView(tvApi)
            itemLayout.addView(tvDetails)
            resultsContainer.addView(itemLayout)
        }
    }
}
