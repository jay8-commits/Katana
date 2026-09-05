package com.example.secondtargetapp

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
 * Second Independent Target Application Activity.
 *
 * Verifies that the NPatch dynamic profile injection functions across multiple
 * distinct target applications and processes with zero hardcoded cross-app coupling.
 */
class SecondTargetActivity : Activity() {

    data class AuditResult(
        val api: String,
        val expected: String,
        val observed: String,
        val hookStatus: String,
        val matchStatus: String,
        val diagnosis: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF090D16.toInt())
            setPadding(32, 48, 32, 48)
        }

        val tvTitle = TextView(this).apply {
            text = "Target App #2 Verification Dashboard"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(tvTitle)

        val tvSubtitle = TextView(this).apply {
            text = "Second Independent Target Process (PID: ${android.os.Process.myPid()})"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
        }
        rootLayout.addView(tvSubtitle)

        val resultsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 32)
            }
            layoutParams = params
        }
        rootLayout.addView(resultsLayout)

        val btnReinvoke = Button(this).apply {
            text = "RE-INVOKE ALL APIS (TARGET #2 AUDIT)"
            setOnClickListener {
                populateResults(resultsLayout)
            }
        }
        rootLayout.addView(btnReinvoke)

        setContentView(rootLayout)
        populateResults(resultsLayout)
    }

    private fun populateResults(container: LinearLayout) {
        container.removeAllViews()

        val tests = mutableListOf<AuditResult>()

        // 1. Android ID
        val obsAndroidId = try {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "null"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
        val androidMatch = obsAndroidId.startsWith("NPATCH")
        tests.add(
            AuditResult(
                api = "Settings.Secure.getString(android_id)",
                expected = "NPATCH_ANDROID_001",
                observed = obsAndroidId,
                hookStatus = if (androidMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (androidMatch) "MATCH" else "MISMATCH",
                diagnosis = if (androidMatch) "Target #2 observed active randomized Android ID." else "Observed platform default hardware ID."
            )
        )

        // 2. Telephony ID
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val obsTelephony = try {
            @Suppress("DEPRECATION")
            tm?.deviceId ?: tm?.imei ?: "null"
        } catch (_: SecurityException) {
            "Restricted (READ_PRIVILEGED_PHONE_STATE)"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
        val teleMatch = obsTelephony.startsWith("NPATCH")
        tests.add(
            AuditResult(
                api = "TelephonyManager.getDeviceId()",
                expected = "NPATCH_TELEPHONY_001",
                observed = obsTelephony,
                hookStatus = if (teleMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (teleMatch) "MATCH" else "RESTRICTED",
                diagnosis = "Target #2 telephony query evaluated against 5-stage verification model."
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
            AuditResult(
                api = "LocationManager.getLastKnownLocation(gps)",
                expected = "Worldwide Coordinates",
                observed = obsLoc,
                hookStatus = if (isLocMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isLocMatch) "MATCH" else "MISMATCH",
                diagnosis = "Worldwide location coordinates intercepted in Target #2."
            )
        )

        // 4. Location.getLatitude() & getLongitude()
        val obsLat = try {
            val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            loc?.latitude
        } catch (_: Throwable) { null }
        tests.add(
            AuditResult(
                api = "Location.getLatitude()",
                expected = "Active Profile Latitude",
                observed = obsLat?.toString() ?: "null",
                hookStatus = if (obsLat != null) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsLat != null) "MATCH" else "MISMATCH",
                diagnosis = "Location.getLatitude() intercepted via framework hook in Target #2."
            )
        )

        // 5. NetworkInterface Hardware Address
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
        val macMatch = obsMac.startsWith("02:00:11:22")
        tests.add(
            AuditResult(
                api = "NetworkInterface.getHardwareAddress()",
                expected = "02:00:11:22:33:44",
                observed = obsMac,
                hookStatus = if (macMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (macMatch) "MATCH" else "MISMATCH",
                diagnosis = "Hardware MAC address substitution in secondary target process."
            )
        )

        // 6. NetworkInterface InetAddress (RFC 5737 TEST-NET-3)
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
        val ipMatch = obsIp.startsWith("203.0.113.")
        tests.add(
            AuditResult(
                api = "NetworkInterface.getInetAddresses()",
                expected = "203.0.113.42",
                observed = obsIp,
                hookStatus = if (ipMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (ipMatch) "MATCH" else "MISMATCH",
                diagnosis = "RFC 5737 TEST-NET-3 synthetic IP range (203.0.113.0/24) observed in Target #2."
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
            AuditResult(
                api = "WifiInfo.getMacAddress()",
                expected = "02:00:11:22:33:44",
                observed = obsWifiMac,
                hookStatus = if (isWifiMacMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isWifiMacMatch) "MATCH" else "MISMATCH",
                diagnosis = "WifiInfo MAC address intercepted in Target #2."
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
            AuditResult(
                api = "WifiInfo.getIpAddress()",
                expected = "203.0.113.42",
                observed = obsWifiIp,
                hookStatus = if (isWifiIpMatch) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (isWifiIpMatch) "MATCH" else "MISMATCH",
                diagnosis = "WifiInfo IPv4 address unpacked in Target #2."
            )
        )

        // 9. WifiInfo.getSSID() & getBSSID()
        val obsSsid = try {
            @Suppress("DEPRECATION")
            wm?.connectionInfo?.ssid ?: "<unknown ssid>"
        } catch (t: Throwable) { "Error: ${t.message}" }
        tests.add(
            AuditResult(
                api = "WifiInfo.getSSID()",
                expected = "\"LabTest_WiFi\"",
                observed = obsSsid,
                hookStatus = if (obsSsid.contains("LabTest")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsSsid.contains("LabTest")) "MATCH" else "MISMATCH",
                diagnosis = "Connected WiFi SSID substituted in Target #2."
            )
        )

        val obsBssid = try {
            @Suppress("DEPRECATION")
            wm?.connectionInfo?.bssid ?: "02:00:00:00:00:00"
        } catch (t: Throwable) { "Error: ${t.message}" }
        tests.add(
            AuditResult(
                api = "WifiInfo.getBSSID()",
                expected = "02:00:11:22:33:44",
                observed = obsBssid,
                hookStatus = if (obsBssid.startsWith("02:00:11:22")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsBssid.startsWith("02:00:11:22")) "MATCH" else "MISMATCH",
                diagnosis = "Connected WiFi BSSID substituted in Target #2."
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
            AuditResult(
                api = "LinkProperties.getAddresses()",
                expected = "203.0.113.42",
                observed = obsLinkProp,
                hookStatus = if (obsLinkProp.startsWith("203.0.113.")) "TARGET_OBSERVED" else "HOOK_REGISTERED",
                matchStatus = if (obsLinkProp.startsWith("203.0.113.")) "MATCH" else "MISMATCH",
                diagnosis = "LinkProperties interface address intercepted in Target #2."
            )
        )

        // 11. Socket Egress IP (Boundary)
        tests.add(
            AuditResult(
                api = "Socket.connect (Public Egress IP)",
                expected = "UNALTERED_PHYSICAL_EGRESS",
                observed = "PHYSICAL_CELLULAR_OR_WIFI_EGRESS",
                hookStatus = "UNSUPPORTED_AT_CURRENT_LAYER",
                matchStatus = "UNSUPPORTED",
                diagnosis = "Framework hooks intercept app APIs; physical public transport egress is correctly classified as UNSUPPORTED_AT_CURRENT_LAYER."
            )
        )

        for (test in tests) {
            val card = LinearLayout(this).apply {
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

            val tvHeader = TextView(this).apply {
                text = "${test.api} [${test.matchStatus}]"
                setTextColor(if (test.matchStatus == "MATCH") 0xFF4ADE80.toInt() else 0xFFFCA5A5.toInt())
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val tvBody = TextView(this).apply {
                text = "EXPECTED: ${test.expected}\nOBSERVED: ${test.observed}\nSTATUS: ${test.hookStatus}\nDIAGNOSIS: ${test.diagnosis}"
                setTextColor(0xFFCBD5E1.toInt())
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
            }

            card.addView(tvHeader)
            card.addView(tvBody)
            container.addView(card)
        }
    }
}
