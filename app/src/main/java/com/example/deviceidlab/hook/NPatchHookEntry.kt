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
        installNetworkInterfaceHooks(lpparam)
        installLocationHooks(lpparam)
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

            // 1. getDeviceId() & getDeviceId(int)
            hookTelephonyMethod(telephonyManagerClass, "getDeviceId", lpparam.packageName, NPatchConfig.KEY_IMEI)
            hookTelephonySlotMethod(telephonyManagerClass, "getDeviceId", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 2. getImei() & getImei(int)
            hookTelephonyMethod(telephonyManagerClass, "getImei", lpparam.packageName, NPatchConfig.KEY_IMEI)
            hookTelephonySlotMethod(telephonyManagerClass, "getImei", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 3. getMeid() & getMeid(int)
            hookTelephonyMethod(telephonyManagerClass, "getMeid", lpparam.packageName, NPatchConfig.KEY_IMEI)
            hookTelephonySlotMethod(telephonyManagerClass, "getMeid", lpparam.packageName, NPatchConfig.KEY_IMEI)

            // 4. getSimSerialNumber() & getSimSerialNumber(int)
            hookTelephonyMethod(telephonyManagerClass, "getSimSerialNumber", lpparam.packageName, NPatchConfig.KEY_SERIAL)
            hookTelephonySlotMethod(telephonyManagerClass, "getSimSerialNumber", lpparam.packageName, NPatchConfig.KEY_SERIAL)

            // 5. getSubscriberId() & getSubscriberId(int)
            hookTelephonyMethod(telephonyManagerClass, "getSubscriberId", lpparam.packageName, NPatchConfig.KEY_IMEI)
            hookTelephonySlotMethod(telephonyManagerClass, "getSubscriberId", lpparam.packageName, NPatchConfig.KEY_IMEI)

            log("EVENT: HOOK_REGISTERED | Hook: TelephonyManager (10 methods & slot overloads)")
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
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: TelephonyManager.$methodName() | Target: $targetPackage")
                            val spoofedVal = queryIpcValue(null, configKey)
                            if (!spoofedVal.isNullOrEmpty()) {
                                param.result = spoofedVal
                                log("EVENT: VALUE_REPLACED | API: TelephonyManager.$methodName() | Target: $targetPackage | Replaced: ${TestApiCatalog.maskValue(spoofedVal)}")
                            } else {
                                log("EVENT: PROFILE_LOOKUP_FAILED | Key: $configKey")
                            }
                        } catch (e: Throwable) {
                            log("EVENT: HOOK_EXECUTION_EXCEPTION | Method: $methodName | Error: ${e.message}")
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            val spoofedVal = queryIpcValue(null, configKey)
                            if (!spoofedVal.isNullOrEmpty()) {
                                param.throwable = null
                                param.result = spoofedVal
                            }
                        } catch (_: Throwable) {
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: TelephonyManager.$methodName()")
        } catch (e: Throwable) {
            log("Hook TelephonyManager.$methodName skipped: ${e.message}")
        }
    }

    private fun hookTelephonySlotMethod(clazz: Class<*>, methodName: String, targetPackage: String, configKey: String) {
        try {
            XposedHelpers.findAndHookMethod(
                clazz,
                methodName,
                java.lang.Integer.TYPE,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        val slot = (param.args.getOrNull(0) as? Int) ?: 0
                        try {
                            isHookExecuting.set(true)
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: TelephonyManager.$methodName($slot) | Target: $targetPackage")
                            val spoofedVal = queryIpcValue(null, configKey)
                            if (!spoofedVal.isNullOrEmpty()) {
                                param.result = spoofedVal
                                log("EVENT: VALUE_REPLACED | API: TelephonyManager.$methodName($slot) | Target: $targetPackage | Replaced: ${TestApiCatalog.maskValue(spoofedVal)}")
                            } else {
                                log("EVENT: PROFILE_LOOKUP_FAILED | Key: $configKey")
                            }
                        } catch (e: Throwable) {
                            log("EVENT: HOOK_EXECUTION_EXCEPTION | Method: $methodName($slot) | Error: ${e.message}")
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            val spoofedVal = queryIpcValue(null, configKey)
                            if (!spoofedVal.isNullOrEmpty()) {
                                param.throwable = null
                                param.result = spoofedVal
                            }
                        } catch (_: Throwable) {
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: TelephonyManager.$methodName(int)")
        } catch (e: Throwable) {
            log("Hook TelephonyManager.$methodName(int) skipped: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 4. WifiInfo & Network Information Hooks
    // -------------------------------------------------------------------------
    private fun installWifiHooks(lpparam: LoadPackageParam) {
        try {
            val wifiInfoClass = XposedHelpers.findClass("android.net.wifi.WifiInfo", lpparam.classLoader)

            // 4.1 WifiInfo.getMacAddress()
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

            // 4.2 WifiInfo.getIpAddress() -> little-endian 32-bit int
            XposedHelpers.findAndHookMethod(
                wifiInfoClass,
                "getIpAddress",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: WifiInfo.getIpAddress() | Target: ${lpparam.packageName}")
                            val syntheticIp = queryIpcValue(null, NPatchConfig.KEY_TEST_IPV4)
                                ?: queryIpcValue(null, NPatchConfig.KEY_LOC_SYNTHETIC_IP)
                                ?: "192.0.2.101"
                            val ipInt = parseIpv4ToLittleEndianInt(syntheticIp)
                            param.result = ipInt
                            log("EVENT: VALUE_REPLACED | API: WifiInfo.getIpAddress() | Target: ${lpparam.packageName} | Replaced: $syntheticIp (int=$ipInt)")
                        } catch (e: Throwable) {
                            log("EVENT: HOOK_EXECUTION_EXCEPTION | Method: WifiInfo.getIpAddress | Error: ${e.message}")
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: WifiInfo.getIpAddress()")

            // 4.3 WifiInfo.getSSID()
            XposedHelpers.findAndHookMethod(
                wifiInfoClass,
                "getSSID",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            val spoofedSsid = queryIpcValue(null, NPatchConfig.KEY_WIFI_SSID) ?: "\"TestLab-WiFi\""
                            param.result = spoofedSsid
                            log("EVENT: VALUE_REPLACED | API: WifiInfo.getSSID() | Target: ${lpparam.packageName} | Replaced: $spoofedSsid")
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: WifiInfo.getSSID()")

            // 4.4 WifiInfo.getBSSID()
            XposedHelpers.findAndHookMethod(
                wifiInfoClass,
                "getBSSID",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            val spoofedBssid = queryIpcValue(null, NPatchConfig.KEY_WIFI_BSSID) ?: "02:00:00:00:00:00"
                            param.result = spoofedBssid
                            log("EVENT: VALUE_REPLACED | API: WifiInfo.getBSSID() | Target: ${lpparam.packageName} | Replaced: $spoofedBssid")
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: WifiInfo.getBSSID()")

            // 4.5 WifiManager.getDhcpInfo()
            val wifiManagerClass = XposedHelpers.findClass("android.net.wifi.WifiManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                wifiManagerClass,
                "getDhcpInfo",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: WifiManager.getDhcpInfo() | Target: ${lpparam.packageName}")
                            val syntheticIp = queryIpcValue(null, NPatchConfig.KEY_TEST_IPV4)
                                ?: queryIpcValue(null, NPatchConfig.KEY_LOC_SYNTHETIC_IP)
                                ?: "192.0.2.101"
                            val ipInt = parseIpv4ToLittleEndianInt(syntheticIp)
                            val parts = syntheticIp.split(".")
                            val gatewayStr = if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.1" else "192.0.2.1"
                            val gatewayInt = parseIpv4ToLittleEndianInt(gatewayStr)

                            val dhcp = android.net.DhcpInfo()
                            dhcp.ipAddress = ipInt
                            dhcp.gateway = gatewayInt
                            dhcp.serverAddress = gatewayInt
                            dhcp.netmask = 0x00FFFFFF // 255.255.255.0 little-endian
                            dhcp.dns1 = parseIpv4ToLittleEndianInt("8.8.8.8")
                            dhcp.dns2 = parseIpv4ToLittleEndianInt("8.8.4.4")

                            param.result = dhcp
                            log("EVENT: VALUE_REPLACED | API: WifiManager.getDhcpInfo() | Target: ${lpparam.packageName} | Replaced: IP=$syntheticIp, GW=$gatewayStr")
                        } catch (e: Throwable) {
                            log("EVENT: HOOK_EXECUTION_EXCEPTION | Method: WifiManager.getDhcpInfo | Error: ${e.message}")
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: WifiManager.getDhcpInfo()")

        } catch (e: Throwable) {
            log("Wifi hooks skipped: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 4.6 Java NetworkInterface Hooks (getHardwareAddress)
    // -------------------------------------------------------------------------
    private fun installNetworkInterfaceHooks(lpparam: LoadPackageParam) {
        try {
            val netIfClass = XposedHelpers.findClass("java.net.NetworkInterface", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                netIfClass,
                "getHardwareAddress",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        try {
                            isHookExecuting.set(true)
                            val spoofedMac = queryIpcValue(null, NPatchConfig.KEY_MAC)
                            if (!spoofedMac.isNullOrEmpty()) {
                                val macBytes = parseMacToBytes(spoofedMac)
                                if (macBytes != null) {
                                    param.result = macBytes
                                    log("EVENT: VALUE_REPLACED | API: NetworkInterface.getHardwareAddress() | Target: ${lpparam.packageName} | Replaced: ${TestApiCatalog.maskValue(spoofedMac)}")
                                }
                            }
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: NetworkInterface.getHardwareAddress()")
        } catch (e: Throwable) {
            log("NetworkInterface hook skipped: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // 5. Location Subsystem Hooks
    // -------------------------------------------------------------------------
    private fun installLocationHooks(lpparam: LoadPackageParam) {
        try {
            val locationManagerClass = XposedHelpers.findClass("android.location.LocationManager", lpparam.classLoader)

            // 5.1 Hook getLastKnownLocation(String)
            XposedHelpers.findAndHookMethod(
                locationManagerClass,
                "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        val requestedProvider = (param.args.getOrNull(0) as? String) ?: "gps"
                        try {
                            isHookExecuting.set(true)
                            log("EVENT: API_INVOCATION_INTERCEPTED | API: LocationManager.getLastKnownLocation($requestedProvider) | Target: ${lpparam.packageName}")
                            val locProfile = queryLocationIpcProfile(null)
                            if (locProfile != null) {
                                val locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader)
                                val syntheticLoc = XposedHelpers.newInstance(locationClass, requestedProvider)
                                XposedHelpers.callMethod(syntheticLoc, "setLatitude", locProfile.latitude)
                                XposedHelpers.callMethod(syntheticLoc, "setLongitude", locProfile.longitude)
                                XposedHelpers.callMethod(syntheticLoc, "setAltitude", locProfile.altitude)
                                XposedHelpers.callMethod(syntheticLoc, "setAccuracy", locProfile.accuracy)
                                XposedHelpers.callMethod(syntheticLoc, "setSpeed", locProfile.speed)
                                XposedHelpers.callMethod(syntheticLoc, "setBearing", locProfile.bearing)
                                XposedHelpers.callMethod(syntheticLoc, "setTime", locProfile.timestamp)
                                try {
                                    XposedHelpers.callMethod(syntheticLoc, "setElapsedRealtimeNanos", locProfile.elapsedRealtimeNanos)
                                } catch (_: Throwable) {}

                                param.result = syntheticLoc
                                log("EVENT: VALUE_REPLACED | API: LocationManager.getLastKnownLocation($requestedProvider) | Target: ${lpparam.packageName} | Replaced: Lat=${locProfile.latitude}, Lng=${locProfile.longitude}, Alt=${locProfile.altitude}")
                            } else {
                                log("EVENT: PROFILE_LOOKUP_FAILED | Subsystem: location | Key: locationProfile")
                            }
                        } catch (e: Throwable) {
                            log("EVENT: HOOK_EXECUTION_EXCEPTION | Method: getLastKnownLocation | Error: ${e.message}")
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        val requestedProvider = (param.args.getOrNull(0) as? String) ?: "gps"
                        try {
                            isHookExecuting.set(true)
                            val locProfile = queryLocationIpcProfile(null)
                            if (locProfile != null) {
                                val locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader)
                                val syntheticLoc = XposedHelpers.newInstance(locationClass, requestedProvider)
                                XposedHelpers.callMethod(syntheticLoc, "setLatitude", locProfile.latitude)
                                XposedHelpers.callMethod(syntheticLoc, "setLongitude", locProfile.longitude)
                                XposedHelpers.callMethod(syntheticLoc, "setAltitude", locProfile.altitude)
                                XposedHelpers.callMethod(syntheticLoc, "setAccuracy", locProfile.accuracy)
                                XposedHelpers.callMethod(syntheticLoc, "setSpeed", locProfile.speed)
                                XposedHelpers.callMethod(syntheticLoc, "setBearing", locProfile.bearing)
                                XposedHelpers.callMethod(syntheticLoc, "setTime", locProfile.timestamp)
                                try {
                                    XposedHelpers.callMethod(syntheticLoc, "setElapsedRealtimeNanos", locProfile.elapsedRealtimeNanos)
                                } catch (_: Throwable) {}

                                param.throwable = null
                                param.result = syntheticLoc
                            }
                        } catch (_: Throwable) {
                        } finally {
                            isHookExecuting.set(false)
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: LocationManager.getLastKnownLocation(String)")

            // 5.2 Hook isProviderEnabled(String)
            XposedHelpers.findAndHookMethod(
                locationManagerClass,
                "isProviderEnabled",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (isHookExecuting.get() == true) return
                        val provider = (param.args.getOrNull(0) as? String) ?: ""
                        if (provider == "gps" || provider == "network" || provider == "fused") {
                            param.result = true
                            log("EVENT: VALUE_REPLACED | API: LocationManager.isProviderEnabled($provider) -> true")
                        }
                    }
                }
            )
            log("EVENT: HOOK_REGISTERED | Hook: LocationManager.isProviderEnabled(String)")

        } catch (e: Throwable) {
            log("LocationManager hooks skipped: ${e.message}")
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

    private fun queryLocationIpcProfile(cr: ContentResolver?): com.example.deviceidlab.model.LocationProfile? {
        val resolver = cr ?: resolveContentResolver() ?: return null
        return try {
            val locUri = NPatchConfig.LOCATION_PROVIDER_URI
            resolver.query(locUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val jsonIdx = cursor.getColumnIndex(NPatchConfig.KEY_LOC_JSON)
                    if (jsonIdx >= 0) {
                        val json = cursor.getString(jsonIdx)
                        if (!json.isNullOrEmpty()) {
                            try {
                                val worldwide = com.example.deviceidlab.manager.LocationJsonSerializer.parseWorldwide(json)
                                log("EVENT: VALUE_GENERATED | Worldwide Profile: ${worldwide.city}, ${worldwide.country} | Coords: ${worldwide.latitude}, ${worldwide.longitude} | TZ: ${worldwide.timezone} | Synthetic IP: ${worldwide.syntheticIp}")
                                return@use worldwide.toLocationProfile()
                            } catch (_: Throwable) {
                                return@use com.example.deviceidlab.manager.LocationJsonSerializer.parse(json)
                            }
                        }
                    }
                    val latIdx = cursor.getColumnIndex(NPatchConfig.KEY_LOC_LATITUDE)
                    val lngIdx = cursor.getColumnIndex(NPatchConfig.KEY_LOC_LONGITUDE)
                    if (latIdx >= 0 && lngIdx >= 0) {
                        val lat = cursor.getString(latIdx)?.toDoubleOrNull() ?: 0.0
                        val lng = cursor.getString(lngIdx)?.toDoubleOrNull() ?: 0.0
                        val alt = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_ALTITUDE).coerceAtLeast(0))?.toDoubleOrNull() ?: 0.0
                        val acc = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_ACCURACY).coerceAtLeast(0))?.toFloatOrNull() ?: 5.0f
                        val speed = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_SPEED).coerceAtLeast(0))?.toFloatOrNull() ?: 0.0f
                        val bearing = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_BEARING).coerceAtLeast(0))?.toFloatOrNull() ?: 0.0f
                        val provider = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_PROVIDER).coerceAtLeast(0)) ?: "gps"
                        val profileId = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_PROFILE_ID).coerceAtLeast(0)) ?: "loc_ipc"
                        val city = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_CITY).coerceAtLeast(0)) ?: "Unknown"
                        val country = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_COUNTRY).coerceAtLeast(0)) ?: "Unknown"
                        val synthIp = cursor.getString(cursor.getColumnIndex(NPatchConfig.KEY_LOC_SYNTHETIC_IP).coerceAtLeast(0)) ?: "203.0.113.42"
                        log("EVENT: VALUE_GENERATED | Location IPC: $city, $country ($lat, $lng) IP: $synthIp")
                        return@use com.example.deviceidlab.model.LocationProfile(
                            profileId = profileId,
                            latitude = lat,
                            longitude = lng,
                            altitude = alt,
                            accuracy = acc,
                            speed = speed,
                            bearing = bearing,
                            provider = provider
                        )
                    }
                }
                null
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

    private fun parseIpv4ToLittleEndianInt(ip: String?): Int {
        if (ip.isNullOrEmpty()) return 0
        val parts = ip.split(".")
        if (parts.size != 4) return 0
        val b0 = parts[0].toIntOrNull() ?: 0
        val b1 = parts[1].toIntOrNull() ?: 0
        val b2 = parts[2].toIntOrNull() ?: 0
        val b3 = parts[3].toIntOrNull() ?: 0
        return (b0 and 0xFF) or ((b1 and 0xFF) shl 8) or ((b2 and 0xFF) shl 16) or ((b3 and 0xFF) shl 24)
    }

    private fun parseMacToBytes(mac: String?): ByteArray? {
        if (mac.isNullOrEmpty()) return null
        val clean = mac.replace(":", "").replace("-", "")
        if (clean.length != 12) return null
        return try {
            ByteArray(6) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun log(message: String) {
        XposedBridge.log("[$TAG] $message")
        Log.d(TAG, message)
    }
}

