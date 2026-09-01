package com.example.deviceidlab.hook

import android.app.Application
import android.content.ContentResolver
import android.content.Context
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
 * Implements strict event logging, ThreadLocal recursion prevention,
 * masked diagnostics, and non-crash error handling.
 */
class NPatchHookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "NPatch"
        private const val CONTROLLER_PACKAGE = "com.example.deviceidlab"
        private val isHookExecuting = ThreadLocal.withInitial { false }
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Exclude the controller module itself from being hooked
        if (lpparam.packageName == CONTROLLER_PACKAGE) {
            return
        }

        log("EVENT: TARGET_PROCESS_STARTED | Package: ${lpparam.packageName} | Process: ${lpparam.processName}")

        installApplicationStartupHooks(lpparam)
        installSettingsSecureHooks(lpparam)
        installBuildHooks(lpparam)
        installTelephonyHooks(lpparam)
        installWifiHooks(lpparam)
    }

    // -------------------------------------------------------------------------
    // 0. Application Startup Hook (Ensures Context-backed static field setup)
    // -------------------------------------------------------------------------
    private fun installApplicationStartupHooks(lpparam: LoadPackageParam) {
        try {
            val appClass = XposedHelpers.findClass("android.app.Application", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                appClass,
                "attachBaseContext",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val ctx = param.args[0] as? Context ?: return
                        try {
                            val buildClass = XposedHelpers.findClass("android.os.Build", lpparam.classLoader)
                            applyStaticBuildFields(buildClass, ctx.contentResolver, lpparam.packageName)
                        } catch (e: Throwable) {
                            log("Startup static field override skipped: ${e.message}")
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: Application.attachBaseContext (Static Field Initializer)")
        } catch (e: Throwable) {
            log("Application startup hook skipped: ${e.message}")
        }
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
                        if (isHookExecuting.get()) return
                        val key = param.args[1] as? String ?: return
                        if (Settings.Secure.ANDROID_ID == key) {
                            try {
                                isHookExecuting.set(true)
                                log("EVENT: API_INVOCATION_INTERCEPTED | API: Settings.Secure.getString(ANDROID_ID) | Target: ${lpparam.packageName}")
                                val originalVal = param.result as? String
                                val cr = param.args[0] as? ContentResolver
                                val spoofedId = queryIpcValue(cr, NPatchConfig.KEY_ANDROID_ID)
                                if (!spoofedId.isNullOrEmpty()) {
                                    log("EVENT: PROFILE_LOOKUP_SUCCESS | Key: androidId | Val: ${TestApiCatalog.maskValue(spoofedId)}")
                                    param.result = spoofedId
                                    log("EVENT: VALUE_REPLACED | API: ANDROID_ID | Target: ${lpparam.packageName} | Orig: ${TestApiCatalog.maskValue(originalVal)} | Replaced: ${TestApiCatalog.maskValue(spoofedId)}")
                                } else {
                                    log("EVENT: PROFILE_LOOKUP_FAILED | Key: androidId | Falling back to unmodified result")
                                }
                            } finally {
                                isHookExecuting.set(false)
                            }
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: Settings.Secure.getString(ContentResolver, String)")

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
                            if (isHookExecuting.get()) return
                            val key = param.args[1] as? String ?: return
                            if (Settings.Secure.ANDROID_ID == key) {
                                try {
                                    isHookExecuting.set(true)
                                    log("EVENT: API_INVOCATION_INTERCEPTED | API: Settings.Secure.getStringForUser(ANDROID_ID) | Target: ${lpparam.packageName}")
                                    val originalVal = param.result as? String
                                    val cr = param.args[0] as? ContentResolver
                                    val spoofedId = queryIpcValue(cr, NPatchConfig.KEY_ANDROID_ID)
                                    if (!spoofedId.isNullOrEmpty()) {
                                        log("EVENT: PROFILE_LOOKUP_SUCCESS | Key: androidId | Val: ${TestApiCatalog.maskValue(spoofedId)}")
                                        param.result = spoofedId
                                        log("EVENT: VALUE_REPLACED | API: ANDROID_ID (forUser) | Target: ${lpparam.packageName} | Orig: ${TestApiCatalog.maskValue(originalVal)} | Replaced: ${TestApiCatalog.maskValue(spoofedId)}")
                                    } else {
                                        log("EVENT: PROFILE_LOOKUP_FAILED | Key: androidId | Falling back to unmodified result")
                                    }
                                } finally {
                                    isHookExecuting.set(false)
                                }
                            }
                        }
                    }
                )
                log("EVENT: HOOK_REGISTERED | Hook: Settings.Secure.getStringForUser(ContentResolver, String, int)")
            } catch (e: Throwable) {
                log("getStringForUser hook skipped: ${e.message}")
            }
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

            // Initial early static override attempt
            applyStaticBuildFields(buildClass, null, lpparam.packageName)

            // Hook Build.getSerial() (API 26+)
            try {
                XposedHelpers.findAndHookMethod(
                    buildClass,
                    "getSerial",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (isHookExecuting.get()) return
                            try {
                                isHookExecuting.set(true)
                                log("EVENT: API_INVOCATION_INTERCEPTED | API: Build.getSerial() | Target: ${lpparam.packageName}")
                                val originalVal = param.result as? String
                                val spoofedSerial = queryIpcValue(null, NPatchConfig.KEY_SERIAL)
                                if (!spoofedSerial.isNullOrEmpty()) {
                                    log("EVENT: PROFILE_LOOKUP_SUCCESS | Key: serialNumber | Val: ${TestApiCatalog.maskValue(spoofedSerial)}")
                                    param.result = spoofedSerial
                                    log("EVENT: VALUE_REPLACED | API: Build.getSerial() | Target: ${lpparam.packageName} | Orig: ${TestApiCatalog.maskValue(originalVal)} | Replaced: ${TestApiCatalog.maskValue(spoofedSerial)}")
                                } else {
                                    log("EVENT: PROFILE_LOOKUP_FAILED | Key: serialNumber")
                                }
                            } finally {
                                isHookExecuting.set(false)
                            }
                        }
                    }
                )
                log("EVENT: HOOK_REGISTERED | Hook: Build.getSerial()")
            } catch (e: Throwable) {
                log("Build.getSerial hook skipped: ${e.message}")
            }
        } catch (e: Throwable) {
            log("Error installing Build hooks: ${e.message}")
        }
    }

    private fun applyStaticBuildFields(buildClass: Class<*>, cr: ContentResolver?, packageName: String) {
        try {
            val model = queryIpcValue(cr, NPatchConfig.KEY_BUILD_MODEL)
            if (!model.isNullOrEmpty()) {
                XposedHelpers.setStaticObjectField(buildClass, "MODEL", model)
                log("EVENT: VALUE_REPLACED | API: Build.MODEL (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(model)}")
            }

            val manufacturer = queryIpcValue(cr, NPatchConfig.KEY_BUILD_MANUFACTURER)
            if (!manufacturer.isNullOrEmpty()) {
                XposedHelpers.setStaticObjectField(buildClass, "MANUFACTURER", manufacturer)
                log("EVENT: VALUE_REPLACED | API: Build.MANUFACTURER (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(manufacturer)}")
            }

            val brand = queryIpcValue(cr, NPatchConfig.KEY_BUILD_BRAND)
            if (!brand.isNullOrEmpty()) {
                XposedHelpers.setStaticObjectField(buildClass, "BRAND", brand)
                log("EVENT: VALUE_REPLACED | API: Build.BRAND (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(brand)}")
            }

            val product = queryIpcValue(cr, NPatchConfig.KEY_BUILD_PRODUCT)
            if (!product.isNullOrEmpty()) {
                XposedHelpers.setStaticObjectField(buildClass, "PRODUCT", product)
                log("EVENT: VALUE_REPLACED | API: Build.PRODUCT (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(product)}")
            }

            val device = queryIpcValue(cr, NPatchConfig.KEY_BUILD_DEVICE)
            if (!device.isNullOrEmpty()) {
                XposedHelpers.setStaticObjectField(buildClass, "DEVICE", device)
                log("EVENT: VALUE_REPLACED | API: Build.DEVICE (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(device)}")
            }

            val fingerprint = queryIpcValue(cr, NPatchConfig.KEY_BUILD_FINGERPRINT)
            if (!fingerprint.isNullOrEmpty()) {
                XposedHelpers.setStaticObjectField(buildClass, "FINGERPRINT", fingerprint)
                log("EVENT: VALUE_REPLACED | API: Build.FINGERPRINT (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(fingerprint)}")
            }

            val serial = queryIpcValue(cr, NPatchConfig.KEY_SERIAL)
            if (!serial.isNullOrEmpty()) {
                @Suppress("DEPRECATION")
                XposedHelpers.setStaticObjectField(buildClass, "SERIAL", serial)
                log("EVENT: VALUE_REPLACED | API: Build.SERIAL (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(serial)}")
            }
        } catch (e: Throwable) {
            log("Static Build fields override exception: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 3. TelephonyManager Hooks (Including Overloads)
    // -------------------------------------------------------------------------
    private fun installTelephonyHooks(lpparam: LoadPackageParam) {
        try {
            val telephonyManagerClass = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader)

            // 1. getDeviceId()
            hookTelephonyMethod(telephonyManagerClass, "getDeviceId", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 2. getDeviceId(int)
            hookTelephonySlotMethod(telephonyManagerClass, "getDeviceId", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 3. getImei()
            hookTelephonyMethod(telephonyManagerClass, "getImei", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 4. getImei(int)
            hookTelephonySlotMethod(telephonyManagerClass, "getImei", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 5. getMeid()
            hookTelephonyMethod(telephonyManagerClass, "getMeid", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 6. getMeid(int)
            hookTelephonySlotMethod(telephonyManagerClass, "getMeid", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 7. getSimSerialNumber()
            hookTelephonyMethod(telephonyManagerClass, "getSimSerialNumber", lpparam.packageName, NPatchConfig.KEY_SERIAL)

            // 8. getSubscriberId()
            hookTelephonyMethod(telephonyManagerClass, "getSubscriberId", lpparam.packageName, NPatchConfig.KEY_IMEI)

            log("EVENT: HOOK_REGISTERED | Hook: TelephonyManager (8 methods & slot overloads)")
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
                        if (isHookExecuting.get()) return
                        try {
                            isHookExecuting.set(true)
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: TelephonyManager.$methodName() | Target: $targetPackage")
                            val originalVal = param.result as? String
                            val spoofedVal = queryIpcValue(null, configKey)
                            if (!spoofedVal.isNullOrEmpty()) {
                                log("EVENT: PROFILE_LOOKUP_SUCCESS | Key: $configKey | Val: ${TestApiCatalog.maskValue(spoofedVal)}")
                                param.result = spoofedVal
                                log("EVENT: VALUE_REPLACED | API: TelephonyManager.$methodName() | Target: $targetPackage | Orig: ${TestApiCatalog.maskValue(originalVal)} | Replaced: ${TestApiCatalog.maskValue(spoofedVal)}")
                            } else {
                                log("EVENT: PROFILE_LOOKUP_FAILED | Key: $configKey")
                            }
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    private fun hookTelephonySlotMethod(clazz: Class<*>, methodName: String, targetPackage: String, configKey: String) {
        try {
            XposedHelpers.findAndHookMethod(
                clazz,
                methodName,
                java.lang.Integer.TYPE,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get()) return
                        try {
                            isHookExecuting.set(true)
                            val slot = param.args[0] as? Int ?: 0
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: TelephonyManager.$methodName($slot) | Target: $targetPackage")
                            val originalVal = param.result as? String
                            val spoofedVal = queryIpcValue(null, configKey)
                            if (!spoofedVal.isNullOrEmpty()) {
                                log("EVENT: PROFILE_LOOKUP_SUCCESS | Key: $configKey | Val: ${TestApiCatalog.maskValue(spoofedVal)}")
                                param.result = spoofedVal
                                log("EVENT: VALUE_REPLACED | API: TelephonyManager.$methodName($slot) | Target: $targetPackage | Orig: ${TestApiCatalog.maskValue(originalVal)} | Replaced: ${TestApiCatalog.maskValue(spoofedVal)}")
                            } else {
                                log("EVENT: PROFILE_LOOKUP_FAILED | Key: $configKey")
                            }
                        } finally {
                            isHookExecuting.set(false)
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
                        if (isHookExecuting.get()) return
                        try {
                            isHookExecuting.set(true)
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: WifiInfo.getMacAddress() | Target: ${lpparam.packageName}")
                            val originalVal = param.result as? String
                            val spoofedMac = queryIpcValue(null, NPatchConfig.KEY_MAC)
                            if (!spoofedMac.isNullOrEmpty()) {
                                log("EVENT: PROFILE_LOOKUP_SUCCESS | Key: macAddress | Val: ${TestApiCatalog.maskValue(spoofedMac)}")
                                param.result = spoofedMac
                                log("EVENT: VALUE_REPLACED | API: WifiInfo.getMacAddress() | Target: ${lpparam.packageName} | Orig: ${TestApiCatalog.maskValue(originalVal)} | Replaced: ${TestApiCatalog.maskValue(spoofedMac)}")
                            } else {
                                log("EVENT: PROFILE_LOOKUP_FAILED | Key: macAddress")
                            }
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: WifiInfo.getMacAddress()")
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
            log("EVENT: IPC_RESOLVER_UNAVAILABLE | Cannot resolve ContentResolver for IPC query of $key")
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
            log("EVENT: IPC_QUERY_EXCEPTION | Key: $key | Error: ${e.message}")
            null
        }
    }

    private fun resolveContentResolver(): ContentResolver? {
        return try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null)
            val currentApp = XposedHelpers.callStaticMethod(activityThreadClass, "currentApplication") as? Application
            currentApp?.contentResolver
        } catch (e: Throwable) {
            null
        }
    }

    private fun log(message: String) {
        XposedBridge.log("[$TAG] $message")
        Log.d(TAG, message)
    }
}
