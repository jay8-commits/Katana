package com.example.deviceidlab.hook

import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.wifi.WifiInfo
import android.os.Build
import android.os.Bundle
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
        installActivityLifecycleHooks(lpparam)
        installSystemPropertiesHooks(lpparam)
        installSettingsSecureHooks(lpparam)
        installBuildHooks(lpparam)
        installTelephonyHooks(lpparam)
        installWifiHooks(lpparam)
    }

    // -------------------------------------------------------------------------
    // 0. Application Startup Hooks (Ensures Context-backed static field setup)
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
                            syncStaticBuildFields(ctx.contentResolver, lpparam.packageName)
                        } catch (e: Throwable) {
                            log("attachBaseContext static field override skipped: ${e.message}")
                        }
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                appClass,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        try {
                            syncStaticBuildFields(app.contentResolver, lpparam.packageName)
                        } catch (e: Throwable) {
                            log("Application.onCreate static field override skipped: ${e.message}")
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: Application lifecycle (Static Field Initializer)")
        } catch (e: Throwable) {
            log("Application startup hook skipped: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 0.1 Activity Lifecycle Hooks (Ensures UI views always observe active profile)
    // -------------------------------------------------------------------------
    private fun installActivityLifecycleHooks(lpparam: LoadPackageParam) {
        try {
            val activityClass = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                activityClass,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        try {
                            syncStaticBuildFields(activity.contentResolver, lpparam.packageName)
                        } catch (_: Throwable) {}
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                activityClass,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        try {
                            syncStaticBuildFields(activity.contentResolver, lpparam.packageName)
                        } catch (_: Throwable) {}
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: Activity lifecycle (Dynamic Static Sync)")
        } catch (e: Throwable) {
            log("Activity lifecycle hook skipped: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 0.2 SystemProperties Hooks (Intercepts framework & native property queries)
    // -------------------------------------------------------------------------
    private fun installSystemPropertiesHooks(lpparam: LoadPackageParam) {
        try {
            val sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                sysPropClass,
                "get",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        val key = param.args[0] as? String ?: return
                        val replacement = getSystemPropertyReplacement(key)
                        if (replacement != null) {
                            param.result = replacement
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                sysPropClass,
                "get",
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        val key = param.args[0] as? String ?: return
                        val replacement = getSystemPropertyReplacement(key)
                        if (replacement != null) {
                            param.result = replacement
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: SystemProperties.get(String, [String])")
        } catch (e: Throwable) {
            log("SystemProperties hook skipped: ${e.message}")
        }
    }

    private fun getSystemPropertyReplacement(key: String): String? {
        return when {
            key == "ro.product.model" || key.endsWith(".model") -> queryIpcValue(null, NPatchConfig.KEY_BUILD_MODEL)
            key == "ro.product.manufacturer" || key.endsWith(".manufacturer") -> queryIpcValue(null, NPatchConfig.KEY_BUILD_MANUFACTURER)
            key == "ro.product.brand" || key.endsWith(".brand") -> queryIpcValue(null, NPatchConfig.KEY_BUILD_BRAND)
            key == "ro.product.name" || key.endsWith(".name") || key == "ro.product.product.name" -> queryIpcValue(null, NPatchConfig.KEY_BUILD_PRODUCT)
            key == "ro.product.device" || key.endsWith(".device") -> queryIpcValue(null, NPatchConfig.KEY_BUILD_DEVICE)
            key == "ro.build.fingerprint" || key.endsWith(".fingerprint") || key == "ro.bootimage.build.fingerprint" -> queryIpcValue(null, NPatchConfig.KEY_BUILD_FINGERPRINT)
            key == "ro.serialno" || key == "ro.boot.serialno" || key == "no.such.thing" -> queryIpcValue(null, NPatchConfig.KEY_SERIAL)
            else -> null
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
                        if (isHookExecuting.get() == true) return
                        val key = param.args[1] as? String ?: return
                        if (Settings.Secure.ANDROID_ID == key) {
                            val cr = param.args[0] as? ContentResolver
                            try {
                                isHookExecuting.set(true)
                                log("EVENT: API_INVOCATION_INTERCEPTED | API: Settings.Secure.getString(ANDROID_ID) | Target: ${lpparam.packageName}")
                                val originalVal = param.result as? String
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
                            // Proactively synchronize Build static fields with active ContentResolver
                            syncStaticBuildFields(cr, lpparam.packageName)
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
                            if (isHookExecuting.get() == true) return
                            val key = param.args[1] as? String ?: return
                            if (Settings.Secure.ANDROID_ID == key) {
                                val cr = param.args[0] as? ContentResolver
                                try {
                                    isHookExecuting.set(true)
                                    log("EVENT: API_INVOCATION_INTERCEPTED | API: Settings.Secure.getStringForUser(ANDROID_ID) | Target: ${lpparam.packageName}")
                                    val originalVal = param.result as? String
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
                                syncStaticBuildFields(cr, lpparam.packageName)
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

            // Initial static override attempt
            syncStaticBuildFields(null, lpparam.packageName)

            // Hook Build.getSerial() (API 26+)
            try {
                XposedHelpers.findAndHookMethod(
                    buildClass,
                    "getSerial",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (isHookExecuting.get() == true) return
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

    private fun setStaticFieldReliably(clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(null, value)
        } catch (_: Throwable) {
            try {
                XposedHelpers.setStaticObjectField(clazz, fieldName, value)
            } catch (_: Throwable) {}
        }
    }

    private fun syncStaticBuildFields(cr: ContentResolver?, packageName: String) {
        if (isHookExecuting.get() == true) return
        try {
            isHookExecuting.set(true)
            val buildClass = Build::class.java

            val model = queryIpcValue(cr, NPatchConfig.KEY_BUILD_MODEL)
            if (!model.isNullOrEmpty()) {
                setStaticFieldReliably(buildClass, "MODEL", model)
                log("EVENT: VALUE_REPLACED | API: Build.MODEL (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(model)}")
            }

            val manufacturer = queryIpcValue(cr, NPatchConfig.KEY_BUILD_MANUFACTURER)
            if (!manufacturer.isNullOrEmpty()) {
                setStaticFieldReliably(buildClass, "MANUFACTURER", manufacturer)
                log("EVENT: VALUE_REPLACED | API: Build.MANUFACTURER (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(manufacturer)}")
            }

            val brand = queryIpcValue(cr, NPatchConfig.KEY_BUILD_BRAND)
            if (!brand.isNullOrEmpty()) {
                setStaticFieldReliably(buildClass, "BRAND", brand)
                log("EVENT: VALUE_REPLACED | API: Build.BRAND (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(brand)}")
            }

            val product = queryIpcValue(cr, NPatchConfig.KEY_BUILD_PRODUCT)
            if (!product.isNullOrEmpty()) {
                setStaticFieldReliably(buildClass, "PRODUCT", product)
                log("EVENT: VALUE_REPLACED | API: Build.PRODUCT (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(product)}")
            }

            val device = queryIpcValue(cr, NPatchConfig.KEY_BUILD_DEVICE)
            if (!device.isNullOrEmpty()) {
                setStaticFieldReliably(buildClass, "DEVICE", device)
                log("EVENT: VALUE_REPLACED | API: Build.DEVICE (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(device)}")
            }

            val fingerprint = queryIpcValue(cr, NPatchConfig.KEY_BUILD_FINGERPRINT)
            if (!fingerprint.isNullOrEmpty()) {
                setStaticFieldReliably(buildClass, "FINGERPRINT", fingerprint)
                log("EVENT: VALUE_REPLACED | API: Build.FINGERPRINT (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(fingerprint)}")
            }

            val serial = queryIpcValue(cr, NPatchConfig.KEY_SERIAL)
            if (!serial.isNullOrEmpty()) {
                @Suppress("DEPRECATION")
                setStaticFieldReliably(buildClass, "SERIAL", serial)
                log("EVENT: VALUE_REPLACED | API: Build.SERIAL (Static) | Target: $packageName | Replaced: ${TestApiCatalog.maskValue(serial)}")
            }
        } catch (e: Throwable) {
            log("Static Build fields override exception: ${e.message}")
        } finally {
            isHookExecuting.set(false)
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
                        if (isHookExecuting.get() == true) return
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
                        if (isHookExecuting.get() == true) return
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
                        if (isHookExecuting.get() == true) return
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

