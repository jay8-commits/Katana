package com.example.deviceidlab.hook

import android.content.ContentResolver
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/**
 * Generalized NPatch / LSPosed Hook Entry Point.
 *
 * Intercepts supported Android Device Information and Identity APIs
 * dynamically for any authorized target application across processes.
 *
 * Supported API inventory:
 * 1. Settings.Secure.getString(ContentResolver, String) -> ANDROID_ID
 * 2. Settings.Secure.getStringForUser(ContentResolver, String, int) -> ANDROID_ID
 * 3. Build fields (MODEL, MANUFACTURER, BRAND, PRODUCT, DEVICE, FINGERPRINT, SERIAL)
 * 4. Build.getSerial()
 * 5. TelephonyManager (getDeviceId, getDeviceId(int), getImei, getImei(int), getMeid, getMeid(int), getSimSerialNumber, getSubscriberId)
 * 6. WifiInfo.getMacAddress()
 */
class NPatchHookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "NPatch"
        private const val CONTROLLER_PACKAGE = "com.example.deviceidlab"
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Exclude the controller module itself from being hooked
        if (lpparam.packageName == CONTROLLER_PACKAGE) {
            return
        }

        log("Target package detected: ${lpparam.packageName}, process: ${lpparam.processName}")
        log("NPatchHookEntry loaded successfully in target process ${lpparam.processName}")

        installSettingsSecureHooks(lpparam)
        installBuildHooks(lpparam)
        installTelephonyHooks(lpparam)
        installWifiHooks(lpparam)
    }

    // -------------------------------------------------------------------------
    // 1. Settings.Secure Hooks (ANDROID_ID)
    // -------------------------------------------------------------------------
    private fun installSettingsSecureHooks(lpparam: LoadPackageParam) {
        try {
            val settingsSecureClass = XposedHelpers.findClass("android.provider.Settings\$Secure", lpparam.classLoader)

            // Settings.Secure.getString(ContentResolver, String)
            XposedHelpers.findAndHookMethod(
                settingsSecureClass,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[1] as? String ?: return
                        if (Settings.Secure.ANDROID_ID == key) {
                            val originalVal = param.result as? String
                            val cr = param.args[0] as? ContentResolver
                            val spoofedId = queryIpcValue(cr, NPatchConfig.KEY_ANDROID_ID)
                            log("API: Settings.Secure.getString(ANDROID_ID) | Target: ${lpparam.packageName} | Original: $originalVal | Replaced: $spoofedId")
                            if (!spoofedId.isNullOrEmpty()) {
                                param.result = spoofedId
                            }
                        }
                    }
                }
            )

            // Settings.Secure.getStringForUser(ContentResolver, String, int)
            try {
                XposedHelpers.findAndHookMethod(
                    settingsSecureClass,
                    "getStringForUser",
                    ContentResolver::class.java,
                    String::class.java,
                    java.lang.Integer.TYPE,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val key = param.args[1] as? String ?: return
                            if (Settings.Secure.ANDROID_ID == key) {
                                val originalVal = param.result as? String
                                val cr = param.args[0] as? ContentResolver
                                val spoofedId = queryIpcValue(cr, NPatchConfig.KEY_ANDROID_ID)
                                log("API: Settings.Secure.getStringForUser(ANDROID_ID) | Target: ${lpparam.packageName} | Original: $originalVal | Replaced: $spoofedId")
                                if (!spoofedId.isNullOrEmpty()) {
                                    param.result = spoofedId
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                log("getStringForUser hook skipped: ${e.message}")
            }

            log("Hook installed successfully: Settings.Secure.getString & getStringForUser")
        } catch (e: Throwable) {
            log("Error installing Settings.Secure hooks: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 2. Build Information Hooks
    // -------------------------------------------------------------------------
    private fun installBuildHooks(lpparam: LoadPackageParam) {
        try {
            val buildClass = XposedHelpers.findClass("android.os.Build", lpparam.classLoader)

            // Apply static field modifications if profile values are available
            applyStaticBuildFields(buildClass)

            // Hook Build.getSerial() (API 26+)
            try {
                XposedHelpers.findAndHookMethod(
                    buildClass,
                    "getSerial",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val originalVal = param.result as? String
                            val spoofedSerial = queryIpcValue(null, NPatchConfig.KEY_SERIAL)
                            log("API: Build.getSerial() | Target: ${lpparam.packageName} | Original: $originalVal | Replaced: $spoofedSerial")
                            if (!spoofedSerial.isNullOrEmpty()) {
                                param.result = spoofedSerial
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                log("Build.getSerial hook skipped: ${e.message}")
            }

            log("Hook installed successfully: android.os.Build")
        } catch (e: Throwable) {
            log("Error installing Build hooks: ${e.message}")
        }
    }

    private fun applyStaticBuildFields(buildClass: Class<*>) {
        try {
            val model = queryIpcValue(null, NPatchConfig.KEY_BUILD_MODEL)
            if (!model.isNullOrEmpty()) XposedHelpers.setStaticObjectField(buildClass, "MODEL", model)

            val manufacturer = queryIpcValue(null, NPatchConfig.KEY_BUILD_MANUFACTURER)
            if (!manufacturer.isNullOrEmpty()) XposedHelpers.setStaticObjectField(buildClass, "MANUFACTURER", manufacturer)

            val brand = queryIpcValue(null, NPatchConfig.KEY_BUILD_BRAND)
            if (!brand.isNullOrEmpty()) XposedHelpers.setStaticObjectField(buildClass, "BRAND", brand)

            val product = queryIpcValue(null, NPatchConfig.KEY_BUILD_PRODUCT)
            if (!product.isNullOrEmpty()) XposedHelpers.setStaticObjectField(buildClass, "PRODUCT", product)

            val device = queryIpcValue(null, NPatchConfig.KEY_BUILD_DEVICE)
            if (!device.isNullOrEmpty()) XposedHelpers.setStaticObjectField(buildClass, "DEVICE", device)

            val fingerprint = queryIpcValue(null, NPatchConfig.KEY_BUILD_FINGERPRINT)
            if (!fingerprint.isNullOrEmpty()) XposedHelpers.setStaticObjectField(buildClass, "FINGERPRINT", fingerprint)

            val serial = queryIpcValue(null, NPatchConfig.KEY_SERIAL)
            if (!serial.isNullOrEmpty()) {
                @Suppress("DEPRECATION")
                XposedHelpers.setStaticObjectField(buildClass, "SERIAL", serial)
            }
        } catch (e: Throwable) {
            log("Static Build fields override skipped: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 3. TelephonyManager Hooks
    // -------------------------------------------------------------------------
    private fun installTelephonyHooks(lpparam: LoadPackageParam) {
        try {
            val telephonyManagerClass = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader)

            // getDeviceId()
            hookTelephonyMethod(telephonyManagerClass, "getDeviceId", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // getDeviceId(int)
            try {
                XposedHelpers.findAndHookMethod(
                    telephonyManagerClass,
                    "getDeviceId",
                    java.lang.Integer.TYPE,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val originalVal = param.result as? String
                            val spoofedImei = queryIpcValue(null, NPatchConfig.KEY_IMEI)
                            log("API: TelephonyManager.getDeviceId(slot) | Target: ${lpparam.packageName} | Original: $originalVal | Replaced: $spoofedImei")
                            if (!spoofedImei.isNullOrEmpty()) {
                                param.result = spoofedImei
                            }
                        }
                    }
                )
            } catch (_: Throwable) {}

            // getImei()
            hookTelephonyMethod(telephonyManagerClass, "getImei", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // getImei(int)
            try {
                XposedHelpers.findAndHookMethod(
                    telephonyManagerClass,
                    "getImei",
                    java.lang.Integer.TYPE,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val originalVal = param.result as? String
                            val spoofedImei = queryIpcValue(null, NPatchConfig.KEY_IMEI)
                            log("API: TelephonyManager.getImei(slot) | Target: ${lpparam.packageName} | Original: $originalVal | Replaced: $spoofedImei")
                            if (!spoofedImei.isNullOrEmpty()) {
                                param.result = spoofedImei
                            }
                        }
                    }
                )
            } catch (_: Throwable) {}

            // getMeid()
            hookTelephonyMethod(telephonyManagerClass, "getMeid", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // getSimSerialNumber()
            hookTelephonyMethod(telephonyManagerClass, "getSimSerialNumber", lpparam.packageName, NPatchConfig.KEY_SERIAL)

            // getSubscriberId()
            hookTelephonyMethod(telephonyManagerClass, "getSubscriberId", lpparam.packageName, NPatchConfig.KEY_IMEI)

            log("Hook installed successfully: android.telephony.TelephonyManager")
        } catch (e: Throwable) {
            log("TelephonyManager hooks skipped: ${e.message}")
        }
    }

    private fun hookTelephonyMethod(clazz: Class<*>, methodName: String, targetPackage: String, configKey: String) {
        try {
            XposedHelpers.findAndHookMethod(
                clazz,
                methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalVal = param.result as? String
                        val spoofedVal = queryIpcValue(null, configKey)
                        log("API: TelephonyManager.$methodName() | Target: $targetPackage | Original: $originalVal | Replaced: $spoofedVal")
                        if (!spoofedVal.isNullOrEmpty()) {
                            param.result = spoofedVal
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    // -------------------------------------------------------------------------
    // 4. WifiInfo Hooks
    // -------------------------------------------------------------------------
    private fun installWifiHooks(lpparam: LoadPackageParam) {
        try {
            val wifiInfoClass = XposedHelpers.findClass("android.net.wifi.WifiInfo", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                wifiInfoClass,
                "getMacAddress",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalVal = param.result as? String
                        val spoofedMac = queryIpcValue(null, NPatchConfig.KEY_MAC)
                        log("API: WifiInfo.getMacAddress() | Target: ${lpparam.packageName} | Original: $originalVal | Replaced: $spoofedMac")
                        if (!spoofedMac.isNullOrEmpty()) {
                            param.result = spoofedMac
                        }
                    }
                }
            )
            log("Hook installed successfully: android.net.wifi.WifiInfo")
        } catch (e: Throwable) {
            log("WifiInfo hook skipped: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // IPC Query Helper
    // -------------------------------------------------------------------------
    private fun queryIpcValue(cr: ContentResolver?, key: String): String? {
        val resolver = cr ?: resolveContentResolver()
        if (resolver == null) {
            log("Cannot resolve ContentResolver for IPC query of $key")
            return null
        }
        return try {
            val uri = NPatchConfig.PROVIDER_URI
            resolver.query(uri, arrayOf(key), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(key)
                    if (idx >= 0) {
                        cursor.getString(idx)
                    } else null
                } else null
            }
        } catch (e: Throwable) {
            log("IPC Query exception for key $key: ${e.message}")
            null
        }
    }

    private fun resolveContentResolver(): ContentResolver? {
        return try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null)
            val currentApp = XposedHelpers.callStaticMethod(activityThreadClass, "currentApplication") as? android.app.Application
            currentApp?.contentResolver
        } catch (e: Throwable) {
            log("Failed to get ActivityThread currentApplication: ${e.message}")
            null
        }
    }

    private fun log(message: String) {
        XposedBridge.log("[$TAG] $message")
        Log.d(TAG, message)
    }
}
