package com.example.deviceidlab.hook

import android.net.Uri

object NPatchConfig {
    const val PROVIDER_AUTHORITY = "com.example.deviceidlab.provider.deviceid"
    val PROVIDER_URI: Uri = Uri.parse("content://$PROVIDER_AUTHORITY/profile")

    const val KEY_ANDROID_ID = "androidId"
    const val KEY_IMEI = "imei"
    const val KEY_SERIAL = "serialNumber"
    const val KEY_MAC = "macAddress"
    const val KEY_BUILD_MODEL = "buildModel"
    const val KEY_BUILD_MANUFACTURER = "buildManufacturer"
    const val KEY_BUILD_BRAND = "buildBrand"
    const val KEY_BUILD_PRODUCT = "buildProduct"
    const val KEY_BUILD_DEVICE = "buildDevice"
    const val KEY_BUILD_FINGERPRINT = "buildFingerprint"

    // Location Subsystem IPC Keys
    val LOCATION_PROVIDER_URI: Uri = Uri.parse("content://$PROVIDER_AUTHORITY/location")
    const val KEY_LOC_LATITUDE = "loc_latitude"
    const val KEY_LOC_LONGITUDE = "loc_longitude"
    const val KEY_LOC_ALTITUDE = "loc_altitude"
    const val KEY_LOC_ACCURACY = "loc_accuracy"
    const val KEY_LOC_SPEED = "loc_speed"
    const val KEY_LOC_BEARING = "loc_bearing"
    const val KEY_LOC_TIMESTAMP = "loc_timestamp"
    const val KEY_LOC_ELAPSED_NANOS = "loc_elapsed_nanos"
    const val KEY_LOC_PROVIDER = "loc_provider"
    const val KEY_LOC_PROFILE_ID = "loc_profile_id"
    const val KEY_LOC_JSON = "loc_json"
    const val KEY_LOC_CITY = "loc_city"
    const val KEY_LOC_COUNTRY = "loc_country"
    const val KEY_LOC_COUNTRY_CODE = "loc_country_code"
    const val KEY_LOC_TIMEZONE = "loc_timezone"
    const val KEY_LOC_SYNTHETIC_IP = "loc_synthetic_ip"
    const val KEY_LOC_STATE = "loc_state"

    // Network Subsystem IPC Keys
    const val KEY_TEST_IPV4 = "testIpv4"
    const val KEY_WIFI_SSID = "wifi_ssid"
    const val KEY_WIFI_BSSID = "wifi_bssid"
}
