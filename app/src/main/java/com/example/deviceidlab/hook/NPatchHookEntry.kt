package com.example.deviceidlab.hook

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class NPatchHookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == "com.example.deviceidlab") {
            return
        }

        XposedBridge.log("NPatchHookEntry: Initializing dynamic identity hook for ${lpparam.packageName}")

        try {
            // Hook Settings.Secure.getString for ANDROID_ID
            XposedHelpers.findAndHookMethod(
                Settings.Secure::class.java,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val name = param.args[1] as? String
                        if (Settings.Secure.ANDROID_ID == name) {
                            val cr = param.args[0] as? ContentResolver
                            val spoofedId = queryIpcValue(cr, NPatchConfig.KEY_ANDROID_ID)
                            if (!spoofedId.isNullOrEmpty()) {
                                param.result = spoofedId
                            }
                        }
                    }
                }
            )

            // Hook TelephonyManager getDeviceId / getImei
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java,
                "getDeviceId",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val spoofedImei = queryIpcValue(null, NPatchConfig.KEY_IMEI)
                        if (!spoofedImei.isNullOrEmpty()) {
                            param.result = spoofedImei
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("NPatchHookEntry error: ${e.message}")
        }
    }

    private fun queryIpcValue(cr: ContentResolver?, key: String): String? {
        try {
            val uri = NPatchConfig.PROVIDER_URI
            val resolver = cr ?: return null
            resolver.query(uri, arrayOf(key), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(key)
                    if (idx >= 0) {
                        return cursor.getString(idx)
                    }
                }
            }
        } catch (_: Throwable) {}
        return null
    }
}
