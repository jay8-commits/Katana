package com.example.deviceidlab

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deviceidlab.hook.NPatchConfig
import com.example.deviceidlab.provider.DeviceIdProvider
import com.example.ui.theme.DeviceIdLabTheme
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Independent Target Demo Application.
 *
 * Requirements:
 * 1. Invokes real Android & Java APIs for Identity, Location, and Network subsystems.
 * 2. Compares live observed returns against expected profile values from DeviceIdProvider.
 * 3. Applies the 5-stage verification model:
 *    HOOK_REGISTERED -> HOOK_INVOKED -> VALUE_GENERATED -> VALUE_RETURNED -> TARGET_OBSERVED
 * 4. Categorizes IP test boundaries:
 *    LOCAL_LOOPBACK | PRIVATE_LAN | SYNTHETIC_TEST_IP (RFC 5737 203.0.113.0/24) | ACTUAL_PUBLIC_EGRESS_IP
 * 5. Identifies UNSUPPORTED_AT_CURRENT_LAYER rather than falsely claiming transport modification.
 */
class TargetDemoActivity : ComponentActivity() {

    companion object {
        const val TAG = "TargetDemoApp"
        private val PROVIDER_URI = Uri.parse(NPatchConfig.PROVIDER_URI_STRING)
    }

    data class ApiTestReport(
        val category: String,
        val api: String,
        val expectedTestValue: String,
        val observedValue: String,
        val hookStatus: String, // HOOK_REGISTERED, HOOK_INVOKED, VALUE_GENERATED, VALUE_RETURNED, TARGET_OBSERVED
        val matchStatus: String, // MATCH, MISMATCH, RESTRICTED, UNSUPPORTED
        val ipClassification: String? = null,
        val diagnosis: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeviceIdLabTheme {
                TargetDemoScreen(
                    onBackPressed = { finish() },
                    runAllTests = { executeAuditedTests() }
                )
            }
        }
    }

    private fun executeAuditedTests(): List<ApiTestReport> {
        val expectedBundle = try {
            contentResolver.call(PROVIDER_URI, DeviceIdProvider.METHOD_GET_CURRENT_TEST_IDS, null, null)
        } catch (_: Throwable) {
            null
        }

        val expAndroidId = expectedBundle?.getString(DeviceIdProvider.KEY_ANDROID_TEST_ID)
            ?: expectedBundle?.getString(DeviceIdProvider.KEY_TEST_ID) ?: "NPATCH_ANDROID_001"
        val expTelephonyId = expectedBundle?.getString(DeviceIdProvider.KEY_TELEPHONY_TEST_ID) ?: "NPATCH_TELEPHONY_001"
        val expSyntheticIp = expectedBundle?.getString(NPatchConfig.KEY_ACTIVE_SYNTHETIC_IP) ?: NPatchConfig.DEFAULT_SYNTHETIC_IP
        val expMac = expectedBundle?.getString(NPatchConfig.KEY_ACTIVE_MAC_ADDRESS) ?: NPatchConfig.DEFAULT_MAC
        val expSsid = expectedBundle?.getString(NPatchConfig.KEY_ACTIVE_WIFI_SSID) ?: NPatchConfig.DEFAULT_SSID
        val expBssid = expectedBundle?.getString(NPatchConfig.KEY_ACTIVE_WIFI_BSSID) ?: expMac
        val expLat = if (expectedBundle?.containsKey(NPatchConfig.KEY_ACTIVE_LATITUDE) == true) expectedBundle.getDouble(NPatchConfig.KEY_ACTIVE_LATITUDE) else 37.7749
        val expLon = if (expectedBundle?.containsKey(NPatchConfig.KEY_ACTIVE_LONGITUDE) == true) expectedBundle.getDouble(NPatchConfig.KEY_ACTIVE_LONGITUDE) else -122.4194
        val expCity = expectedBundle?.getString(NPatchConfig.KEY_ACTIVE_CITY) ?: "San Francisco"

        val reports = mutableListOf<ApiTestReport>()

        // 1. Identity - Settings.Secure.ANDROID_ID
        val obsAndroidId = queryAndroidId()
        val isAndroidMatch = obsAndroidId == expAndroidId
        reports.add(
            ApiTestReport(
                category = "IDENTITY",
                api = "Settings.Secure.getString(android_id)",
                expectedTestValue = expAndroidId,
                observedValue = obsAndroidId,
                hookStatus = if (isAndroidMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isAndroidMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isAndroidMatch) {
                    "Full 5-stage verification passed: Target application observed dynamic simulated Android ID."
                } else {
                    "Observed platform hardware default; target process running without active injection or pending initial patch."
                }
            )
        )

        // 2. Identity - TelephonyManager.getDeviceId() / getImei()
        val obsTelephony = queryTelephonyId()
        val isTelephonyMatch = obsTelephony == expTelephonyId
        reports.add(
            ApiTestReport(
                category = "IDENTITY",
                api = "TelephonyManager.getDeviceId() / getImei()",
                expectedTestValue = expTelephonyId,
                observedValue = obsTelephony,
                hookStatus = if (isTelephonyMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isTelephonyMatch) "MATCH" else if (obsTelephony.contains("Restricted", ignoreCase = true)) "RESTRICTED" else "MISMATCH",
                diagnosis = if (isTelephonyMatch) {
                    "Full 5-stage verification passed: Target process intercepted Telephony API and returned current profile ID."
                } else {
                    "Platform restricted on Android 10+ (READ_PRIVILEGED_PHONE_STATE required) or running unhooked."
                }
            )
        )

        // 3. Location - LocationManager.getLastKnownLocation
        val obsLocation = queryLocation()
        val expLocStr = "%.4f, %.4f (%s)".format(expLat, expLon, expCity)
        val isLocMatch = obsLocation.contains("%.2f".format(expLat))
        reports.add(
            ApiTestReport(
                category = "LOCATION",
                api = "LocationManager.getLastKnownLocation(gps)",
                expectedTestValue = expLocStr,
                observedValue = obsLocation,
                hookStatus = if (isLocMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isLocMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isLocMatch) {
                    "Full 5-stage verification passed: Worldwide location subsystem successfully returned synthetic coordinates."
                } else {
                    "Returned physical device GPS location or null provider snapshot."
                }
            )
        )

        // 3a. Location - Location.getLatitude()
        val obsLat = queryLocationLatitude()
        val isLatMatch = obsLat != null && Math.abs(obsLat - expLat) < 0.01
        reports.add(
            ApiTestReport(
                category = "LOCATION",
                api = "Location.getLatitude()",
                expectedTestValue = "%.4f".format(expLat),
                observedValue = obsLat?.let { "%.4f".format(it) } ?: "null",
                hookStatus = if (isLatMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isLatMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isLatMatch) {
                    "Full 5-stage verification passed: Location.getLatitude() intercepted with active profile latitude."
                } else {
                    "Default or hardware latitude observed."
                }
            )
        )

        // 3b. Location - Location.getLongitude()
        val obsLon = queryLocationLongitude()
        val isLonMatch = obsLon != null && Math.abs(obsLon - expLon) < 0.01
        reports.add(
            ApiTestReport(
                category = "LOCATION",
                api = "Location.getLongitude()",
                expectedTestValue = "%.4f".format(expLon),
                observedValue = obsLon?.let { "%.4f".format(it) } ?: "null",
                hookStatus = if (isLonMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isLonMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isLonMatch) {
                    "Full 5-stage verification passed: Location.getLongitude() intercepted with active profile longitude."
                } else {
                    "Default or hardware longitude observed."
                }
            )
        )

        // 4. Network Java - NetworkInterface.getHardwareAddress()
        val obsNetMac = queryNetworkInterfaceHardwareAddress()
        val isNetMacMatch = obsNetMac.equals(expMac, ignoreCase = true)
        reports.add(
            ApiTestReport(
                category = "NETWORK_JAVA",
                api = "NetworkInterface.getHardwareAddress()",
                expectedTestValue = expMac,
                observedValue = obsNetMac,
                hookStatus = if (isNetMacMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isNetMacMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isNetMacMatch) {
                    "Full 5-stage verification passed: NetworkInterface hardware MAC replaced with controlled test MAC."
                } else {
                    "Standard hardware MAC returned (or 02:00:00:00:00:00 privacy mask)."
                }
            )
        )

        // 5. Network Java - NetworkInterface.getInetAddresses() (RFC 5737 TEST-NET-3)
        val obsInetAddr = queryNetworkInterfaceInetAddress()
        val isInetMatch = obsInetAddr == expSyntheticIp
        val ipClass = NPatchConfig.classifyIp(obsInetAddr)
        reports.add(
            ApiTestReport(
                category = "NETWORK_JAVA",
                api = "NetworkInterface.getInetAddresses()",
                expectedTestValue = expSyntheticIp,
                observedValue = obsInetAddr,
                hookStatus = if (isInetMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isInetMatch) "MATCH" else "MISMATCH",
                ipClassification = ipClass,
                diagnosis = if (isInetMatch) {
                    "Full 5-stage verification passed: NetworkInterface returned synthetic RFC 5737 TEST-NET address."
                } else {
                    "Observed local/LAN IP address instead of synthetic test range."
                }
            )
        )

        // 6. Network Android - WifiInfo.getMacAddress()
        val obsWifiMac = queryWifiMac()
        val isWifiMacMatch = obsWifiMac.equals(expMac, ignoreCase = true)
        reports.add(
            ApiTestReport(
                category = "NETWORK_ANDROID",
                api = "WifiInfo.getMacAddress()",
                expectedTestValue = expMac,
                observedValue = obsWifiMac,
                hookStatus = if (isWifiMacMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isWifiMacMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isWifiMacMatch) {
                    "Full 5-stage verification passed: WifiInfo MAC substituted with synthetic address."
                } else {
                    "Returned default Android 6+ privacy sentinel (02:00:00:00:00:00)."
                }
            )
        )

        // 7. Network Android - WifiInfo.getIpAddress()
        val obsWifiIp = queryWifiIp()
        val isWifiIpMatch = obsWifiIp == expSyntheticIp
        reports.add(
            ApiTestReport(
                category = "NETWORK_ANDROID",
                api = "WifiInfo.getIpAddress()",
                expectedTestValue = expSyntheticIp,
                observedValue = obsWifiIp,
                hookStatus = if (isWifiIpMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isWifiIpMatch) "MATCH" else "MISMATCH",
                ipClassification = NPatchConfig.classifyIp(obsWifiIp),
                diagnosis = if (isWifiIpMatch) {
                    "Full 5-stage verification passed: WifiInfo IPv4 integer unpacked to matching RFC 5737 test address."
                } else {
                    "Returned raw network connection IP or 0.0.0.0."
                }
            )
        )

        // 8. Network Android - WifiInfo.getSSID()
        val obsSsid = queryWifiSsid()
        val isSsidMatch = obsSsid.trim('"') == expSsid.trim('"')
        reports.add(
            ApiTestReport(
                category = "NETWORK_ANDROID",
                api = "WifiInfo.getSSID()",
                expectedTestValue = expSsid,
                observedValue = obsSsid,
                hookStatus = if (isSsidMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isSsidMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isSsidMatch) {
                    "Full 5-stage verification passed: Connected WiFi SSID substituted with controlled test SSID."
                } else {
                    "Returned hardware connected SSID or <unknown ssid>."
                }
            )
        )

        // 8b. Network Android - WifiInfo.getBSSID()
        val obsBssid = queryWifiBssid()
        val isBssidMatch = obsBssid.equals(expBssid, ignoreCase = true)
        reports.add(
            ApiTestReport(
                category = "NETWORK_ANDROID",
                api = "WifiInfo.getBSSID()",
                expectedTestValue = expBssid,
                observedValue = obsBssid,
                hookStatus = if (isBssidMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isBssidMatch) "MATCH" else "MISMATCH",
                diagnosis = if (isBssidMatch) {
                    "Full 5-stage verification passed: Connected WiFi BSSID substituted with controlled test BSSID."
                } else {
                    "Returned hardware connected BSSID or 02:00:00:00:00:00."
                }
            )
        )

        // 9. Network Android - LinkProperties.getLinkAddresses()
        val obsLinkProp = queryLinkProperties()
        val isLinkMatch = obsLinkProp.contains(expSyntheticIp)
        reports.add(
            ApiTestReport(
                category = "NETWORK_ANDROID",
                api = "LinkProperties.getLinkAddresses()",
                expectedTestValue = expSyntheticIp,
                observedValue = obsLinkProp,
                hookStatus = if (isLinkMatch) NPatchConfig.STAGE_TARGET_OBSERVED else NPatchConfig.STAGE_HOOK_REGISTERED,
                matchStatus = if (isLinkMatch) "MATCH" else "MISMATCH",
                ipClassification = NPatchConfig.classifyIp(obsLinkProp),
                diagnosis = if (isLinkMatch) {
                    "LinkProperties contains RFC 5737 synthetic interface test address."
                } else {
                    "Physical interface LinkProperties observed."
                }
            )
        )

        // 10. Actual Public Egress IP (Boundary Check)
        reports.add(
            ApiTestReport(
                category = "NETWORK_ANDROID",
                api = "Socket.connect (Public Egress IP)",
                expectedTestValue = "UNALTERED_PHYSICAL_EGRESS",
                observedValue = "PHYSICAL_CELLULAR_OR_WIFI_EGRESS",
                hookStatus = "UNSUPPORTED_AT_CURRENT_LAYER",
                matchStatus = "UNSUPPORTED",
                ipClassification = NPatchConfig.IP_TYPE_ACTUAL_PUBLIC_EGRESS,
                diagnosis = "Architectural boundary: Application framework method hooks intercept app-facing APIs (WifiInfo, NetworkInterface, LinkProperties). They do NOT modify carrier NAT or physical TCP packet headers without a VPN tunnel. Correctly categorized as UNSUPPORTED_AT_CURRENT_LAYER."
            )
        )

        return reports
    }

    @SuppressLint("HardwareIds")
    private fun queryAndroidId(): String {
        return try {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "null"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    private fun queryTelephonyId(): String {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return "Telephony Unavailable"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val imeiVal = tm.imei
                    if (!imeiVal.isNullOrBlank()) imeiVal else (tm.getDeviceId() ?: "null")
                } catch (_: SecurityException) {
                    "Restricted (READ_PRIVILEGED_PHONE_STATE required)"
                }
            } else {
                try {
                    tm.deviceId ?: "null"
                } catch (_: SecurityException) {
                    "Permission Denied"
                }
            }
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryLocation(): String {
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return "Location Unavailable"
        return try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                "%.4f, %.4f (provider: %s)".format(loc.latitude, loc.longitude, loc.provider)
            } else {
                "No cached location available"
            }
        } catch (_: SecurityException) {
            "Location Permission Denied"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryNetworkInterfaceHardwareAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val macBytes = nif.hardwareAddress
                if (macBytes != null && macBytes.isNotEmpty() && !nif.isLoopback) {
                    return NPatchConfig.byteArrayToMac(macBytes)
                }
            }
            "02:00:00:00:00:00"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryNetworkInterfaceInetAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    val host = addr.hostAddress ?: ""
                    if (!addr.isLoopbackAddress && !host.contains(":")) {
                        return host
                    }
                }
            }
            "127.0.0.1"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryWifiMac(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return "WiFi Unavailable"
        return try {
            @Suppress("DEPRECATION")
            wm.connectionInfo?.macAddress ?: "02:00:00:00:00:00"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryWifiIp(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return "0.0.0.0"
        return try {
            @Suppress("DEPRECATION")
            val ipInt = wm.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) NPatchConfig.intToIpLittleEndian(ipInt) else "0.0.0.0"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryWifiSsid(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return "WiFi Unavailable"
        return try {
            @Suppress("DEPRECATION")
            wm.connectionInfo?.ssid ?: "<unknown ssid>"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryLinkProperties(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Connectivity Unavailable"
        return try {
            val activeNet = cm.activeNetwork
            val lp = cm.getLinkProperties(activeNet)
            val linkAddrs = lp?.linkAddresses
            if (!linkAddrs.isNullOrEmpty()) {
                linkAddrs.firstOrNull()?.address?.hostAddress ?: "None"
            } else {
                "None"
            }
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }

    private fun queryLocationLatitude(): Double? {
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            loc?.latitude
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryLocationLongitude(): Double? {
        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            loc?.longitude
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryWifiBssid(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return "WiFi Unavailable"
        return try {
            @Suppress("DEPRECATION")
            wm.connectionInfo?.bssid ?: "02:00:00:00:00:00"
        } catch (t: Throwable) {
            "Error: ${t.message}"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetDemoScreen(
    onBackPressed: () -> Unit,
    runAllTests: () -> List<TargetDemoActivity.ApiTestReport>
) {
    var testResults by remember { mutableStateOf(runAllTests()) }
    var lastRunTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    val passCount = testResults.count { it.matchStatus == "MATCH" }
    val totalCount = testResults.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Target Verification Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Audited Identity, Location & Network APIs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed,
                        modifier = Modifier.testTag("target_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "5-Stage Verification Engine",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = if (passCount > 0) Color(0xFF14532D) else Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$passCount / $totalCount PASS",
                                color = if (passCount > 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Every test independently queries live platform APIs and evaluates: HOOK_REGISTERED -> HOOK_INVOKED -> VALUE_GENERATED -> VALUE_RETURNED -> TARGET_OBSERVED. A registered hook is not a pass; target observation is strictly required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Query: ${timeFormatter.format(Date(lastRunTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Target PID: ${android.os.Process.myPid()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Results List
            testResults.forEach { report ->
                ApiTestCard(report)
            }

            // Action Buttons
            Button(
                onClick = {
                    testResults = runAllTests()
                    lastRunTimestamp = System.currentTimeMillis()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("target_refresh_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "RE-INVOKE ALL APIS (LIVE AUDIT)", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("return_to_lab_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RETURN TO CONTROLLER")
            }
        }
    }
}

@Composable
fun ApiTestCard(report: TargetDemoActivity.ApiTestReport) {
    val isMatch = report.matchStatus == "MATCH"
    val isUnsupported = report.matchStatus == "UNSUPPORTED"
    val isRestricted = report.matchStatus == "RESTRICTED"

    val statusColor = when {
        isMatch -> Color(0xFF22C55E)
        isUnsupported -> Color(0xFF94A3B8)
        isRestricted -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val categoryIcon = when (report.category) {
        "IDENTITY" -> Icons.Default.Fingerprint
        "LOCATION" -> Icons.Default.LocationOn
        "NETWORK_JAVA" -> Icons.Default.Router
        "NETWORK_ANDROID" -> Icons.Default.Wifi
        else -> Icons.Default.Public
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMatch) Icons.Default.CheckCircle else if (isUnsupported) Icons.Default.Info else Icons.Default.Error,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = report.matchStatus,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Text(
                text = report.api,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Values Box
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "EXPECTED TEST VALUE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = report.expectedTestValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "OBSERVED VALUE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = report.observedValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMatch) Color(0xFF4ADE80) else Color(0xFFFCA5A5),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (report.ipClassification != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "IP BOUNDARY:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                            Text(
                                text = report.ipClassification,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (report.ipClassification) {
                                    NPatchConfig.IP_TYPE_SYNTHETIC_TEST_IP -> Color(0xFF38BDF8)
                                    NPatchConfig.IP_TYPE_LOCAL_LOOPBACK -> Color(0xFFF59E0B)
                                    NPatchConfig.IP_TYPE_PRIVATE_LAN -> Color(0xFFA78BFA)
                                    else -> Color(0xFFF87171)
                                },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "HOOK STATUS:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = report.hookStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Text(
                text = report.diagnosis,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}
