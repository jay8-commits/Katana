package com.example.deviceidlab.hook

/**
 * Dedicated Location Subsystem API Catalog.
 *
 * Kept completely separate from TestApiCatalog.kt to preserve the original
 * 21-API Device Identity baseline untouched.
 *
 * All runtime statuses remain labeled "NOT_PERFORMED" until physically
 * verified on hardware with an active LSPosed/NPatch framework.
 */
object LocationApiCatalog {

    enum class InterceptionTier {
        FRAMEWORK_HOOKABLE,
        DYNAMIC_OBJECT_ACCESSOR,
        PLATFORM_RESTRICTED,
        ASYNC_CALLBACK_LIMITED
    }

    data class LocationApiEntry(
        val id: String,
        val displayName: String,
        val frameworkClass: String,
        val methodSignature: String,
        val returnType: String,
        val minSdk: Int,
        val targetSdk: Int,
        val tier: InterceptionTier,
        val isDynamic: Boolean,
        val description: String,
        val runtimeStatus: String = "NOT_PERFORMED"
    )

    val SUPPORTED_LOCATION_APIS: List<LocationApiEntry> = listOf(
        LocationApiEntry(
            id = "LOC_01",
            displayName = "LocationManager.getLastKnownLocation(provider)",
            frameworkClass = "android.location.LocationManager",
            methodSignature = "getLastKnownLocation(java.lang.String)",
            returnType = "android.location.Location",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.FRAMEWORK_HOOKABLE,
            isDynamic = true,
            description = "Primary location query API. Intercepted by NPatch to return synthetic Location object."
        ),
        LocationApiEntry(
            id = "LOC_02",
            displayName = "LocationManager.isProviderEnabled(provider)",
            frameworkClass = "android.location.LocationManager",
            methodSignature = "isProviderEnabled(java.lang.String)",
            returnType = "boolean",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.FRAMEWORK_HOOKABLE,
            isDynamic = true,
            description = "Checks if provider (gps, network) is enabled. Intercepted to ensure provider appears active."
        ),
        LocationApiEntry(
            id = "LOC_03",
            displayName = "LocationManager.getProviders(enabledOnly)",
            frameworkClass = "android.location.LocationManager",
            methodSignature = "getProviders(boolean)",
            returnType = "java.util.List<java.lang.String>",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.FRAMEWORK_HOOKABLE,
            isDynamic = true,
            description = "Enumerates location providers. Intercepted to include spoofed provider in list."
        ),
        LocationApiEntry(
            id = "LOC_04",
            displayName = "LocationManager.getBestProvider(criteria, enabledOnly)",
            frameworkClass = "android.location.LocationManager",
            methodSignature = "getBestProvider(android.location.Criteria, boolean)",
            returnType = "java.lang.String",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.FRAMEWORK_HOOKABLE,
            isDynamic = true,
            description = "Resolves optimal provider name matching criteria. Intercepted to return active provider."
        ),
        LocationApiEntry(
            id = "LOC_05",
            displayName = "Location.getLatitude()",
            frameworkClass = "android.location.Location",
            methodSignature = "getLatitude()",
            returnType = "double",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic latitude (-90.0 to 90.0) from the synthetic Location instance."
        ),
        LocationApiEntry(
            id = "LOC_06",
            displayName = "Location.getLongitude()",
            frameworkClass = "android.location.Location",
            methodSignature = "getLongitude()",
            returnType = "double",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic longitude (-180.0 to 180.0) from the synthetic Location instance."
        ),
        LocationApiEntry(
            id = "LOC_07",
            displayName = "Location.getAltitude()",
            frameworkClass = "android.location.Location",
            methodSignature = "getAltitude()",
            returnType = "double",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic altitude (WGS 84 meters) from the synthetic Location instance."
        ),
        LocationApiEntry(
            id = "LOC_08",
            displayName = "Location.getAccuracy()",
            frameworkClass = "android.location.Location",
            methodSignature = "getAccuracy()",
            returnType = "float",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic horizontal accuracy radius in meters (68% confidence)."
        ),
        LocationApiEntry(
            id = "LOC_09",
            displayName = "Location.getSpeed()",
            frameworkClass = "android.location.Location",
            methodSignature = "getSpeed()",
            returnType = "float",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic ground speed in meters/second."
        ),
        LocationApiEntry(
            id = "LOC_10",
            displayName = "Location.getBearing()",
            frameworkClass = "android.location.Location",
            methodSignature = "getBearing()",
            returnType = "float",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic horizontal direction of travel in degrees (0.0 to 360.0)."
        ),
        LocationApiEntry(
            id = "LOC_11",
            displayName = "Location.getTime()",
            frameworkClass = "android.location.Location",
            methodSignature = "getTime()",
            returnType = "long",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic UTC epoch timestamp in milliseconds."
        ),
        LocationApiEntry(
            id = "LOC_12",
            displayName = "Location.getElapsedRealtimeNanos()",
            frameworkClass = "android.location.Location",
            methodSignature = "getElapsedRealtimeNanos()",
            returnType = "long",
            minSdk = 17,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic boot-relative elapsed nanoseconds for freshness validation."
        ),
        LocationApiEntry(
            id = "LOC_13",
            displayName = "Location.getProvider()",
            frameworkClass = "android.location.Location",
            methodSignature = "getProvider()",
            returnType = "java.lang.String",
            minSdk = 1,
            targetSdk = 34,
            tier = InterceptionTier.DYNAMIC_OBJECT_ACCESSOR,
            isDynamic = true,
            description = "Returns synthetic provider identifier (e.g., gps, network, fused)."
        ),
        LocationApiEntry(
            id = "LOC_14",
            displayName = "LocationManager.getCurrentLocation(provider, ...)",
            frameworkClass = "android.location.LocationManager",
            methodSignature = "getCurrentLocation(java.lang.String, android.os.CancellationSignal, java.util.concurrent.Executor, java.util.function.Consumer)",
            returnType = "void",
            minSdk = 30,
            targetSdk = 34,
            tier = InterceptionTier.ASYNC_CALLBACK_LIMITED,
            isDynamic = true,
            description = "Modern API 30+ async single-shot query. Requires asynchronous Consumer callback interception; marked PLATFORM_RESTRICTED for synchronous hooks."
        )
    )

    fun getApiById(id: String): LocationApiEntry? {
        return SUPPORTED_LOCATION_APIS.firstOrNull { it.id == id }
    }
}
