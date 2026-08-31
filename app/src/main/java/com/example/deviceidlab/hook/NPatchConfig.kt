package com.example.deviceidlab.hook

/**
 * Shared configuration constants for NPatch 1.0.7 runtime communication.
 *
 * Isolated from Xposed framework interfaces so standard Android controller components
 * (MainActivity, DeviceIdProvider, DeviceIdReader, NPatchAuditManager) can access configuration
 * without triggering classloading of Xposed runtime interfaces (which are compileOnly).
 */
object NPatchConfig {
    const val PREF_FILE = "npatch_config"
    const val KEY_ACTIVE_ANDROID_ID = "active_android_id"
    const val KEY_ACTIVE_TELEPHONY_ID = "active_telephony_id"
    const val KEY_INTERCEPTION_ENABLED = "interception_enabled"
    const val KEY_TARGET_PACKAGE_FILTER = "target_package_filter"

    const val PROVIDER_AUTHORITY = "com.example.deviceidlab.provider"
    const val PROVIDER_URI_STRING = "content://$PROVIDER_AUTHORITY"

    /**
     * Set dynamically when the NPatch/Xposed runtime environment initializes in a target process.
     */
    @Volatile
    var isXposedEnvironmentActive: Boolean = false
}
