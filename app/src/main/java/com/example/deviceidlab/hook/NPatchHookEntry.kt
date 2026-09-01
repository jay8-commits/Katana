package com.example.deviceidlab.hook

import android.content.ContentResolver
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class NPatchHookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "NPatch"
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Skip self (the controller module)
        if (lpparam.packageName == "com.example.deviceidlab") {
            return
        }

        log("Target package detected: ${lpparam.packageName}, process: ${lpparam.processName}")
        log("NPatchHookEntry loaded successfully in target process ${lpparam.processName}")

        installSettingsSecureHooks(lpparam.classLoader)
        installTelephonyHooks(lpparam.classLoader)
    }

    private fun installSettingsSecureHooks(classLoader: ClassLoader) {
        try {
            val settingsSecureClass = XposedHelpers.findClass("android.provider.Settings\$Secure", classLoader)

            // 1. Hook Settings.Secure.getString(ContentResolver, String)
            XposedHelpers.findAndHookMethod(
                settingsSecureClass,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val name = param.args[1] as? String ?: return
                        if (Settings.Secure.ANDROID_ID == name) {
                            val originalVal = param.result as? String
                            val cr = param.args[0] as? ContentResolver
                            log("Intercepted Settings.Secure.getString for ANDROID_ID (original: $originalVal)")
                            val spoofedId = queryIpcValue(cr, NPatchConfig.KEY_ANDROID_ID)
                            if (!spoofedId.isNullOrEmpty()) {
                                log("Replaced ANDROID_ID with generated profile: $spoofedId")
                                param.result = spoofedId
                            } else {
                                log("Warning: queryIpcValue returned empty or null for ANDROID_ID")
                            }
                        }
                    }
                }
            )

            // 2. Hook Settings.Secure.getStringForUser(ContentResolver, String, int)
            try {
                XposedHelpers.findAndHookMethod(
                    settingsSecureClass,
                    "getStringForUser",
                    ContentResolver::class.java,
                    String::class.java,
                    java.lang.Integer.TYPE,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val name = param.args[1] as? String ?: return
                            if (Settings.Secure.ANDROID_ID == name) {
                                val originalVal = param.result as? String
                                val cr = param.args[0] as? ContentResolver
                                log("Intercepted Settings.Secure.getStringForUser for ANDROID_ID (original: $originalVal)")
                                val spoofedId = queryIpcValue(cr, NPatchConfig.KEY_ANDROID_ID)
                                if (!spoofedId.isNullOrEmpty()) {
                                    log("Replaced ANDROID_ID (forUser) with generated profile: $spoofedId")
                                    param.result = spoofedId
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                log("getStringForUser hook skipped or not present on this API level: ${e.message}")
            }

            log("Hook installed: Settings.Secure.getString & getStringForUser")
        } catch (e: Throwable) {
            log("Error installing Settings.Secure hooks: ${e.message}")
        }
    }

    private fun installTelephonyHooks(classLoader: ClassLoader) {
        try {
            val telephonyManagerClass = XposedHelpers.findClass("android.telephony.TelephonyManager", classLoader)

            XposedHelpers.findAndHookMethod(
                telephonyManagerClass,
                "getDeviceId",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalVal = param.result as? String
                        log("Intercepted TelephonyManager.getDeviceId (original: $originalVal)")
                        val spoofedImei = queryIpcValue(null, NPatchConfig.KEY_IMEI)
                        if (!spoofedImei.isNullOrEmpty()) {
                            log("Replaced getDeviceId with generated IMEI: $spoofedImei")
                            param.result = spoofedImei
                        }
                    }
                }
            )
            log("Hook installed: TelephonyManager.getDeviceId")
        } catch (e: Throwable) {
            log("TelephonyManager hook skipped: ${e.message}")
        }
    }

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
                } else {
                    log("IPC Cursor empty for key $key")
                    null
                }
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
