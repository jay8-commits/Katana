package com.example.deviceidlab.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.example.deviceidlab.hook.NPatchConfig
import com.example.deviceidlab.manager.DeviceIdentityManager
import com.example.deviceidlab.manager.LocationJsonSerializer
import com.example.deviceidlab.manager.LocationProfileManager

class DeviceIdProvider : ContentProvider() {

    companion object {
        private const val TAG = "DeviceIdProvider"
        const val AUTHORITY = "com.example.deviceidlab.provider.deviceid"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/profile")
        val LOCATION_URI: Uri = Uri.parse("content://$AUTHORITY/location")
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "DeviceIdProvider initialized")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val ctx = context ?: return MatrixCursor(emptyArray())
        val locManager = LocationProfileManager(ctx)
        val worldwideLoc = locManager.getActiveWorldwideProfile()
        val locProfile = worldwideLoc.toLocationProfile()

        // Handle dedicated /location query
        if (uri.path?.contains("location") == true) {
            val locColumns = arrayOf(
                NPatchConfig.KEY_LOC_PROFILE_ID,
                NPatchConfig.KEY_LOC_CITY,
                NPatchConfig.KEY_LOC_COUNTRY,
                NPatchConfig.KEY_LOC_COUNTRY_CODE,
                NPatchConfig.KEY_LOC_LATITUDE,
                NPatchConfig.KEY_LOC_LONGITUDE,
                NPatchConfig.KEY_LOC_TIMEZONE,
                NPatchConfig.KEY_LOC_SYNTHETIC_IP,
                NPatchConfig.KEY_LOC_ALTITUDE,
                NPatchConfig.KEY_LOC_ACCURACY,
                NPatchConfig.KEY_LOC_SPEED,
                NPatchConfig.KEY_LOC_BEARING,
                NPatchConfig.KEY_LOC_TIMESTAMP,
                NPatchConfig.KEY_LOC_ELAPSED_NANOS,
                NPatchConfig.KEY_LOC_PROVIDER,
                NPatchConfig.KEY_LOC_STATE,
                NPatchConfig.KEY_LOC_JSON
            )
            val cursor = MatrixCursor(locColumns)
            cursor.addRow(
                arrayOf(
                    worldwideLoc.profileId,
                    worldwideLoc.city,
                    worldwideLoc.country,
                    worldwideLoc.countryCode,
                    worldwideLoc.latitude.toString(),
                    worldwideLoc.longitude.toString(),
                    worldwideLoc.timezone,
                    worldwideLoc.syntheticIp,
                    worldwideLoc.altitude.toString(),
                    worldwideLoc.accuracy.toString(),
                    worldwideLoc.speed.toString(),
                    worldwideLoc.bearing.toString(),
                    worldwideLoc.timestamp.toString(),
                    worldwideLoc.elapsedRealtimeNanos.toString(),
                    worldwideLoc.provider,
                    worldwideLoc.state,
                    LocationJsonSerializer.serializeWorldwide(worldwideLoc)
                )
            )
            Log.d(TAG, "IPC Query received for location URI: $uri, returning ${worldwideLoc.city}, ${worldwideLoc.country} (${worldwideLoc.latitude}, ${worldwideLoc.longitude})")
            return cursor
        }

        val manager = DeviceIdentityManager(ctx)
        val profile = manager.getActiveProfile()

        Log.d(TAG, "IPC Query received for URI: $uri, returning profile: ${profile.name}, Android ID: ${profile.androidId}")

        val previousProfile = manager.getPreviousProfile()
        val uniquenessStatus = manager.getProfileUniquenessStatus()
        val consistencyStatus = manager.getProfileConsistencyStatus()
        val ipStatus = manager.getIpProfileStatus()

        val columns = arrayOf(
            "androidId", "imei", "serialNumber", "macAddress",
            "buildModel", "buildManufacturer", "buildBrand",
            "buildProduct", "buildDevice", "buildFingerprint",
            "phoneNumber", "batteryHealth", "testIpv4",
            "profileId", "profileName", "profileFingerprint", "profileState",
            "profileUniqueness", "profileConsistency", "ipProfileStatus", "ipProfileValue",
            "previousProfileId", "previousFingerprint",
            "previousAndroidId", "previousPhoneNumber", "previousBatteryHealth", "previousTestIpv4",
            "atomicIntegrity",
            "createdAt", "consumedAt", "activationResult", "consumptionResult",
            // Location Columns for consolidated IPC access
            NPatchConfig.KEY_LOC_LATITUDE,
            NPatchConfig.KEY_LOC_LONGITUDE,
            NPatchConfig.KEY_LOC_ALTITUDE,
            NPatchConfig.KEY_LOC_ACCURACY,
            NPatchConfig.KEY_LOC_SPEED,
            NPatchConfig.KEY_LOC_BEARING,
            NPatchConfig.KEY_LOC_TIMESTAMP,
            NPatchConfig.KEY_LOC_ELAPSED_NANOS,
            NPatchConfig.KEY_LOC_PROVIDER,
            NPatchConfig.KEY_LOC_PROFILE_ID,
            NPatchConfig.KEY_LOC_CITY,
            NPatchConfig.KEY_LOC_COUNTRY,
            NPatchConfig.KEY_LOC_COUNTRY_CODE,
            NPatchConfig.KEY_LOC_TIMEZONE,
            NPatchConfig.KEY_LOC_SYNTHETIC_IP,
            NPatchConfig.KEY_LOC_STATE,
            NPatchConfig.KEY_LOC_JSON
        )
        val cursor = MatrixCursor(columns)
        cursor.addRow(
            arrayOf(
                profile.androidId,
                profile.imei,
                profile.serialNumber,
                profile.macAddress,
                profile.buildModel,
                profile.buildManufacturer,
                profile.buildBrand,
                profile.buildProduct,
                profile.buildDevice,
                profile.buildFingerprint,
                profile.phoneNumber,
                profile.batteryHealth.toString(),
                profile.testIpv4,
                profile.id,
                profile.name,
                profile.computeFingerprint(),
                profile.state.name,
                uniquenessStatus,
                consistencyStatus,
                ipStatus,
                profile.testIpv4,
                previousProfile?.id ?: "",
                previousProfile?.computeFingerprint() ?: "",
                previousProfile?.androidId ?: "",
                previousProfile?.phoneNumber ?: "",
                previousProfile?.batteryHealth?.toString() ?: "",
                previousProfile?.testIpv4 ?: "",
                "ALL_FIELDS_ATOMICALLY_BOUND",
                profile.createdAt.toString(),
                (profile.consumedAt ?: 0L).toString(),
                "SUCCESS",
                if (profile.state.name == "CONSUMED") "CONSUMED_AND_EXEMPTED" else "AVAILABLE",
                // Location values
                worldwideLoc.latitude.toString(),
                worldwideLoc.longitude.toString(),
                worldwideLoc.altitude.toString(),
                worldwideLoc.accuracy.toString(),
                worldwideLoc.speed.toString(),
                worldwideLoc.bearing.toString(),
                worldwideLoc.timestamp.toString(),
                worldwideLoc.elapsedRealtimeNanos.toString(),
                worldwideLoc.provider,
                worldwideLoc.profileId,
                worldwideLoc.city,
                worldwideLoc.country,
                worldwideLoc.countryCode,
                worldwideLoc.timezone,
                worldwideLoc.syntheticIp,
                worldwideLoc.state,
                LocationJsonSerializer.serializeWorldwide(worldwideLoc)
            )
        )
        return cursor
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/vnd.com.example.deviceidlab.profile"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
