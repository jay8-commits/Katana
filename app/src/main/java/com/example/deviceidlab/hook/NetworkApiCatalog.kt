package com.example.deviceidlab.hook

/**
 * Dedicated Network Information Subsystem API Catalog.
 *
 * Separated into its own catalog to preserve the original 21-API Device Identity
 * baseline and 14-API Location Subsystem baseline untouched.
 *
 * Provides a structured inventory of audited network-layer APIs
 * (WifiInfo, WifiManager, DhcpInfo, NetworkInterface, ConnectivityManager)
 * and distinguishes cataloged, hook-registered, observable, and physical states.
 */
object NetworkApiCatalog {

    enum class NetworkInterceptionTier {
        FRAMEWORK_WIFI_HOOKABLE,
        FRAMEWORK_DHCP_HOOKABLE,
        JAVA_CORE_NET_HOOKABLE,
        CONNECTIVITY_MANAGER_AUDITED
    }

    data class NetworkApiEntry(
        val id: String,
        val displayName: String,
        val frameworkClass: String,
        val methodSignature: String,
        val returnType: String,
        val minSdk: Int,
        val targetSdk: Int,
        val tier: NetworkInterceptionTier,
        val isHookImplemented: Boolean,
        val isDynamic: Boolean,
        val description: String,
        val runtimeStatus: String = "NOT_PERFORMED"
    )

    val SUPPORTED_NETWORK_APIS: List<NetworkApiEntry> = listOf(
        NetworkApiEntry(
            id = "NET_01",
            displayName = "WifiInfo.getIpAddress()",
            frameworkClass = "android.net.wifi.WifiInfo",
            methodSignature = "getIpAddress()",
            returnType = "int",
            minSdk = 1,
            targetSdk = 34,
            tier = NetworkInterceptionTier.FRAMEWORK_WIFI_HOOKABLE,
            isHookImplemented = true,
            isDynamic = true,
            description = "Returns synthetic IPv4 address as a little-endian 32-bit integer matching active test IP profile."
        ),
        NetworkApiEntry(
            id = "NET_02",
            displayName = "WifiInfo.getSSID()",
            frameworkClass = "android.net.wifi.WifiInfo",
            methodSignature = "getSSID()",
            returnType = "java.lang.String",
            minSdk = 1,
            targetSdk = 34,
            tier = NetworkInterceptionTier.FRAMEWORK_WIFI_HOOKABLE,
            isHookImplemented = true,
            isDynamic = true,
            description = "Returns synthetic test SSID (e.g., \"TestLab-WiFi\") instead of hardware/unknown SSID."
        ),
        NetworkApiEntry(
            id = "NET_03",
            displayName = "WifiInfo.getBSSID()",
            frameworkClass = "android.net.wifi.WifiInfo",
            methodSignature = "getBSSID()",
            returnType = "java.lang.String",
            minSdk = 1,
            targetSdk = 34,
            tier = NetworkInterceptionTier.FRAMEWORK_WIFI_HOOKABLE,
            isHookImplemented = true,
            isDynamic = true,
            description = "Returns controlled test BSSID (e.g., \"02:00:00:00:00:00\") matching test network configuration."
        ),
        NetworkApiEntry(
            id = "NET_04",
            displayName = "WifiManager.getDhcpInfo()",
            frameworkClass = "android.net.wifi.WifiManager",
            methodSignature = "getDhcpInfo()",
            returnType = "android.net.DhcpInfo",
            minSdk = 1,
            targetSdk = 34,
            tier = NetworkInterceptionTier.FRAMEWORK_DHCP_HOOKABLE,
            isHookImplemented = true,
            isDynamic = true,
            description = "Returns synthetic DhcpInfo instance with ipAddress, gateway, netmask, and DNS matching active test profile."
        ),
        NetworkApiEntry(
            id = "NET_05",
            displayName = "NetworkInterface.getHardwareAddress()",
            frameworkClass = "java.net.NetworkInterface",
            methodSignature = "getHardwareAddress()",
            returnType = "byte[]",
            minSdk = 9,
            targetSdk = 34,
            tier = NetworkInterceptionTier.JAVA_CORE_NET_HOOKABLE,
            isHookImplemented = true,
            isDynamic = true,
            description = "Returns synthetic 6-byte hardware MAC address byte array corresponding to active profile macAddress."
        ),
        NetworkApiEntry(
            id = "NET_06",
            displayName = "WifiManager.getConnectionInfo()",
            frameworkClass = "android.net.wifi.WifiManager",
            methodSignature = "getConnectionInfo()",
            returnType = "android.net.wifi.WifiInfo",
            minSdk = 1,
            targetSdk = 34,
            tier = NetworkInterceptionTier.FRAMEWORK_WIFI_HOOKABLE,
            isHookImplemented = true,
            isDynamic = true,
            description = "WifiManager connection query; underlying WifiInfo accessor methods (MAC, IP, SSID, BSSID) are hooked."
        ),
        NetworkApiEntry(
            id = "NET_07",
            displayName = "ConnectivityManager.getActiveNetworkInfo()",
            frameworkClass = "android.net.ConnectivityManager",
            methodSignature = "getActiveNetworkInfo()",
            returnType = "android.net.NetworkInfo",
            minSdk = 1,
            targetSdk = 34,
            tier = NetworkInterceptionTier.CONNECTIVITY_MANAGER_AUDITED,
            isHookImplemented = false,
            isDynamic = true,
            description = "Legacy network info query (deprecated in API 29). Audited for platform status without modifying public internet routing."
        ),
        NetworkApiEntry(
            id = "NET_08",
            displayName = "ConnectivityManager.getLinkProperties(Network)",
            frameworkClass = "android.net.ConnectivityManager",
            methodSignature = "getLinkProperties(android.net.Network)",
            returnType = "android.net.LinkProperties",
            minSdk = 21,
            targetSdk = 34,
            tier = NetworkInterceptionTier.CONNECTIVITY_MANAGER_AUDITED,
            isHookImplemented = false,
            isDynamic = true,
            description = "Queries system link properties for active network. Audited at target call site without routing interception."
        )
    )

    fun getApiById(id: String): NetworkApiEntry? {
        return SUPPORTED_NETWORK_APIS.firstOrNull { it.id == id }
    }
}
