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
}
