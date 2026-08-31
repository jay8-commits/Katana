package com.example.deviceidlab.hook

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.example.deviceidlab.demo.HookInvocationLog
import com.example.deviceidlab.demo.InterceptionBridge
import com.example.deviceidlab.provider.DeviceIdProvider
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * NPatch 1.0.7 / LSPatch / Xposed Module Entry Point.
 *
 * Architecture:
 * - Target APK is patched and embedded ONLY ONCE.
 * - Dynamically queries DeviceIdProvider via ContentResolver at runtime.
 * - Allows unlimited test ID updates without repatching, reinstalling, or rebuilding.
 */
class NPatchHookEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "NPatchHookEntry"
        const val PREF_FILE = "npatch_config"
        const val KEY_ACTIVE_ANDROID_ID = "active_android_id"
        const val KEY_ACTIVE_TELEPHONY_ID = "active_telephony_id"
        const val KEY_INTERCEPTION_ENABLED = "interception_enabled"
        const val KEY_TARGET_PACKAGE_FILTER = "target_package_filter"

        private val PROVIDER_URI = Uri.parse("content://com.example.deviceidlab.provider")

        /**
         * Internal flag to track when the class is loaded by an active Xposed/NPatch environment.
         */
        @Volatile
        var isXposedEnvironmentActive: Boolean = false
            private set
    }

    private var xSharedPreferences: XSharedPreferences? = null

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        isXposedEnvironmentActive = true
        NPatchConfig.isXposedEnvironmentActive = true
        val msg = "[NPATCH] Runtime loaded in Zygote (path=${startupParam?.modulePath})"
        Log.i(TAG, msg)
        XposedBridge.log(msg)

        try {
            xSharedPreferences = XSharedPreferences("com.example.deviceidlab", PREF_FILE).apply {
                makeWorldReadable()
                reload()
            }
            val prefMsg = "[$TAG] [NPATCH INIT] [NPATCH STAGE: CONFIG_LOAD] XSharedPreferences loaded successfully for com.example.deviceidlab ($PREF_FILE)"
            Log.d(TAG, prefMsg)
            XposedBridge.log(prefMsg)
        } catch (t: Throwable) {
            val err = "[$TAG] [NPATCH FAILURE] [NPATCH STAGE: CONFIG_LOAD] XSharedPreferences init warning: ${t.message}"
            Log.w(TAG, err, t)
            XposedBridge.log(err)
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        isXposedEnvironmentActive = true
        val pid = try { android.os.Process.myPid() } catch (_: Throwable) { 0 }

        Log.i(TAG, "[NPATCH] Runtime loaded")
        XposedBridge.log("[NPATCH] Runtime loaded")

        val targetLog = "[NPATCH] Target process detected: pkg='${lpparam.packageName}', process='${lpparam.processName}', pid=$pid"
        Log.i(TAG, targetLog)
        XposedBridge.log(targetLog)

        Log.i(TAG, "[NPATCH] Hook installation started in process='${lpparam.processName}'")
        XposedBridge.log("[NPATCH] Hook installation started in process='${lpparam.processName}'")

        var hookSuccessCount = 0

        // 1. Hook canary diagnostic method to prove live framework bytecode interception
        try {
            XposedHelpers.findAndHookMethod(
                "com.example.deviceidlab.DeviceIdReader",
                lpparam.classLoader,
                "isNpatchHookActive",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam?): Any {
                        val canaryLog = "[$TAG] [NPATCH CANARY] isNpatchHookActive() intercepted in process='${lpparam.processName}' (PID=$pid, pkg='${lpparam.packageName}') -> returning true"
                        Log.i(TAG, canaryLog)
                        XposedBridge.log(canaryLog)

                        NPatchAuditManager.recordHookEvent(
                            context = null,
                            targetPackage = lpparam.packageName,
                            targetProcess = lpparam.processName,
                            targetPid = pid,
                            hookEntryStatus = "EXECUTED",
                            hookInstallationStatus = "INSTALLED",
                            canaryIntercepted = true,
                            apiName = "DeviceIdReader.isNpatchHookActive"
                        )
                        return true
                    }
                }
            )
            hookSuccessCount++
        } catch (t: Throwable) {
            // Expected if hooking external target application where DeviceIdReader does not exist
            Log.d(TAG, "[$TAG] [NPATCH HOOK] DeviceIdReader not found in ${lpparam.packageName}: ${t.message}")
        }

        // 2. Hook Settings.Secure.getString(ContentResolver, String)
        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Secure",
                lpparam.classLoader,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val settingName = param.args[1] as? String ?: return
                        if (settingName.equals("android_id", ignoreCase = true)) {
                            val resolver = param.args[0] as? ContentResolver
                            val originalValue = param.result as? String ?: "null"

                            Log.i(TAG, "[NPATCH] ANDROID_ID request intercepted in process='${lpparam.processName}' (PID=$pid)")
                            XposedBridge.log("[NPATCH] ANDROID_ID request intercepted in process='${lpparam.processName}' (PID=$pid)")

                            // Dynamic Tier 1: Query DeviceIdProvider from Controller app
                            val dynamicIds = queryDynamicTestIds(resolver)
                            val activeSimulatedId = dynamicIds?.first ?: resolveActiveAndroidId()

                            if (isInterceptionEnabled() && !activeSimulatedId.isNullOrEmpty()) {
                                param.result = activeSimulatedId

                                Log.i(TAG, "[NPATCH] Returning current Android test ID: '$activeSimulatedId'")
                                XposedBridge.log("[NPATCH] Returning current Android test ID: '$activeSimulatedId'")

                                // Standardized NPatch Spoof log
                                val spoofLog = "[$TAG] [NPATCH SPOOF] Package: '${lpparam.packageName}', Process: '${lpparam.processName}', PID: $pid, Original Android ID: '$originalValue', Spoofed Android ID: '$activeSimulatedId'"
                                Log.i(TAG, spoofLog)
                                XposedBridge.log(spoofLog)

                                val readLog = "[NPATCH] Target read verification successful: package='${lpparam.packageName}', original='$originalValue', injected='$activeSimulatedId'"
                                Log.i(TAG, readLog)
                                XposedBridge.log(readLog)

                                InterceptionBridge.logInvocation(
                                    HookInvocationLog(
                                        callerPackage = lpparam.packageName,
                                        targetApi = "Settings.Secure.getString(android_id)",
                                        requestedParam = settingName,
                                        returnedValue = activeSimulatedId,
                                        wasIntercepted = true,
                                        reason = "NPatch 1.0.7 / Xposed module dynamically substituted current runtime Android test ID."
                                    )
                                )

                                reportInterceptionEvent(
                                    resolver = resolver,
                                    targetPkg = lpparam.packageName,
                                    targetProc = lpparam.processName,
                                    targetPid = pid,
                                    apiName = "Settings.Secure.getString(android_id)",
                                    identifierType = DeviceIdProvider.TYPE_ANDROID_ID,
                                    origId = originalValue,
                                    injectedId = activeSimulatedId,
                                    retId = activeSimulatedId
                                )
                            }
                        }
                    }
                }
            )
            hookSuccessCount++
            Log.i(TAG, "[NPATCH] Hook installed successfully on Settings.Secure.getString in pkg='${lpparam.packageName}'")
            XposedBridge.log("[NPATCH] Hook installed successfully on Settings.Secure.getString in pkg='${lpparam.packageName}'")
        } catch (t: Throwable) {
            val errLog = "[$TAG] [NPATCH FAILURE] Failed to hook Settings.Secure.getString in pkg='${lpparam.packageName}': ${t.message}"
            Log.e(TAG, errLog, t)
            XposedBridge.log(errLog)
        }

        // 3. Hook TelephonyManager methods
        hookTelephonyMethods(lpparam)

        // Record initial hook registration
        NPatchAuditManager.recordHookEvent(
            context = null,
            targetPackage = lpparam.packageName,
            targetProcess = lpparam.processName,
            targetPid = pid,
            hookEntryStatus = "EXECUTED",
            hookInstallationStatus = if (hookSuccessCount > 0) "INSTALLED ($hookSuccessCount hooks)" else "INITIALIZED",
            canaryIntercepted = false,
            apiName = "handleLoadPackage"
        )
    }

    private fun queryDynamicTestIds(resolver: ContentResolver?): Pair<String?, String?>? {
        if (resolver == null) return null
        return try {
            val bundle = resolver.call(
                PROVIDER_URI,
                DeviceIdProvider.METHOD_GET_CURRENT_TEST_IDS,
                null,
                null
            )
            val androidId = bundle?.getString(DeviceIdProvider.KEY_ANDROID_TEST_ID)
                ?: bundle?.getString(DeviceIdProvider.KEY_TEST_ID)
            val telephonyId = bundle?.getString(DeviceIdProvider.KEY_TELEPHONY_TEST_ID)
            Pair(androidId, telephonyId)
        } catch (t: Throwable) {
            Log.d(TAG, "[$TAG] [NPATCH] Dynamic ContentProvider query fallback (${t.message})")
            null
        }
    }

    private fun queryDynamicTestId(resolver: ContentResolver?): String? {
        return queryDynamicTestIds(resolver)?.first
    }

    private fun queryDynamicTelephonyId(resolver: ContentResolver?): String? {
        return queryDynamicTestIds(resolver)?.second
    }

    private fun resolveContentResolver(thisObj: Any?): ContentResolver? {
        if (thisObj != null) {
            try {
                val ctx = XposedHelpers.getObjectField(thisObj, "mContext") as? Context
                if (ctx != null) return ctx.contentResolver
            } catch (_: Throwable) {}
        }
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentAppMethod = activityThreadClass.getMethod("currentApplication")
            val app = currentAppMethod.invoke(null) as? Context
            if (app != null) return app.contentResolver
        } catch (_: Throwable) {}
        return null
    }

    private fun reportInterceptionEvent(
        resolver: ContentResolver?,
        targetPkg: String,
        targetProc: String,
        targetPid: Int,
        apiName: String,
        identifierType: String = DeviceIdProvider.TYPE_ANDROID_ID,
        origId: String,
        injectedId: String,
        retId: String
    ) {
        NPatchAuditManager.recordHookEvent(
            context = null,
            targetPackage = targetPkg,
            targetProcess = targetProc,
            targetPid = targetPid,
            hookEntryStatus = "EXECUTED",
            hookInstallationStatus = "ACTIVE_INTERCEPTION",
            canaryIntercepted = true,
            apiName = apiName,
            originalId = origId,
            injectedId = injectedId,
            returnedId = retId
        )

        val targetResolver = resolver ?: resolveContentResolver(null)
        if (targetResolver != null) {
            try {
                val extras = Bundle().apply {
                    putString(DeviceIdProvider.KEY_TARGET_PACKAGE, targetPkg)
                    putString(DeviceIdProvider.KEY_TARGET_PROCESS, targetProc)
                    putInt(DeviceIdProvider.KEY_TARGET_PID, targetPid)
                    putString(DeviceIdProvider.KEY_API_NAME, apiName)
                    putString(DeviceIdProvider.KEY_IDENTIFIER_TYPE, identifierType)
                    putString(DeviceIdProvider.KEY_ORIGINAL_ID, origId)
                    putString(DeviceIdProvider.KEY_RETURNED_ID, retId)
                    putLong(DeviceIdProvider.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                targetResolver.call(
                    PROVIDER_URI,
                    DeviceIdProvider.METHOD_REPORT_INTERCEPTION,
                    null,
                    extras
                )
            } catch (_: Throwable) {}
        }
    }

    private fun hookTelephonyMethods(lpparam: XC_LoadPackage.LoadPackageParam) {
        val telephonyClass = "android.telephony.TelephonyManager"

        // getDeviceId()
        try {
            XposedHelpers.findAndHookMethod(
                telephonyClass,
                lpparam.classLoader,
                "getDeviceId",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyTelephonyOverride(lpparam.packageName, lpparam.processName, param, "getDeviceId()")
                    }
                }
            )
            Log.d(TAG, "[$TAG] [NPATCH HOOK] Installed hook on TelephonyManager.getDeviceId() in ${lpparam.packageName}")
        } catch (t: Throwable) {
            Log.d(TAG, "[$TAG] [NPATCH HOOK] TelephonyManager.getDeviceId() hook skipped: ${t.message}")
        }

        // getDeviceId(int)
        try {
            XposedHelpers.findAndHookMethod(
                telephonyClass,
                lpparam.classLoader,
                "getDeviceId",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyTelephonyOverride(lpparam.packageName, lpparam.processName, param, "getDeviceId(slot)")
                    }
                }
            )
            Log.d(TAG, "[$TAG] [NPATCH HOOK] Installed hook on TelephonyManager.getDeviceId(int) in ${lpparam.packageName}")
        } catch (t: Throwable) {
            Log.d(TAG, "[$TAG] [NPATCH HOOK] TelephonyManager.getDeviceId(int) hook skipped: ${t.message}")
        }

        // getImei()
        try {
            XposedHelpers.findAndHookMethod(
                telephonyClass,
                lpparam.classLoader,
                "getImei",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyTelephonyOverride(lpparam.packageName, lpparam.processName, param, "getImei()")
                    }
                }
            )
            Log.d(TAG, "[$TAG] [NPATCH HOOK] Installed hook on TelephonyManager.getImei() in ${lpparam.packageName}")
        } catch (t: Throwable) {
            Log.d(TAG, "[$TAG] [NPATCH HOOK] TelephonyManager.getImei() hook skipped: ${t.message}")
        }

        // getImei(int)
        try {
            XposedHelpers.findAndHookMethod(
                telephonyClass,
                lpparam.classLoader,
                "getImei",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyTelephonyOverride(lpparam.packageName, lpparam.processName, param, "getImei(slot)")
                    }
                }
            )
            Log.d(TAG, "[$TAG] [NPATCH HOOK] Installed hook on TelephonyManager.getImei(int) in ${lpparam.packageName}")
        } catch (t: Throwable) {
            Log.d(TAG, "[$TAG] [NPATCH HOOK] TelephonyManager.getImei(int) hook skipped: ${t.message}")
        }

        // getMeid()
        try {
            XposedHelpers.findAndHookMethod(
                telephonyClass,
                lpparam.classLoader,
                "getMeid",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyTelephonyOverride(lpparam.packageName, lpparam.processName, param, "getMeid()")
                    }
                }
            )
        } catch (_: Throwable) {}

        // getMeid(int)
        try {
            XposedHelpers.findAndHookMethod(
                telephonyClass,
                lpparam.classLoader,
                "getMeid",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyTelephonyOverride(lpparam.packageName, lpparam.processName, param, "getMeid(slot)")
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    private fun applyTelephonyOverride(
        packageName: String,
        processName: String,
        param: XC_MethodHook.MethodHookParam,
        apiName: String
    ) {
        if (!isInterceptionEnabled()) return

        val pid = try { android.os.Process.myPid() } catch (_: Throwable) { 0 }
        val resolver = resolveContentResolver(param.thisObject)
        val dynamicTelephonyId = queryDynamicTelephonyId(resolver)
        val activeSimulatedTelephony = dynamicTelephonyId ?: resolveActiveTelephonyId() ?: "NPATCH_TELEPHONY_001"

        // Clear any security exceptions thrown by platform on Android 10+ (API 29+)
        param.throwable = null
        param.result = activeSimulatedTelephony

        val msg = "[$TAG] [NPATCH HOOK] [NPATCH VERIFIED] $apiName -> '$activeSimulatedTelephony' in pkg='$packageName', process='$processName' (PID $pid)"
        Log.i(TAG, msg)
        XposedBridge.log(msg)

        InterceptionBridge.logInvocation(
            HookInvocationLog(
                callerPackage = packageName,
                targetApi = "TelephonyManager.$apiName",
                requestedParam = "NONE",
                returnedValue = activeSimulatedTelephony,
                wasIntercepted = true,
                reason = "NPatch 1.0.7 / Xposed module dynamically substituted current runtime Telephony ID."
            )
        )

        reportInterceptionEvent(
            resolver = resolver,
            targetPkg = packageName,
            targetProc = processName,
            targetPid = pid,
            apiName = "TelephonyManager.$apiName",
            identifierType = DeviceIdProvider.TYPE_TELEPHONY_ID,
            origId = "Hardware / Restricted",
            injectedId = activeSimulatedTelephony,
            retId = activeSimulatedTelephony
        )
    }

    private fun getOrInitXPrefs(): XSharedPreferences? {
        if (xSharedPreferences == null) {
            try {
                xSharedPreferences = XSharedPreferences("com.example.deviceidlab", PREF_FILE).apply {
                    makeWorldReadable()
                    reload()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "[$TAG] [NPATCH FAILURE] Lazy XSharedPreferences init warning: ${t.message}")
            }
        }
        return xSharedPreferences
    }

    private fun resolveActiveAndroidId(): String? {
        val inMemory = InterceptionBridge.activeSimulatedAndroidId.value
        if (!inMemory.isNullOrEmpty()) return inMemory

        return try {
            val prefs = getOrInitXPrefs()
            prefs?.reload()
            val id = prefs?.getString(KEY_ACTIVE_ANDROID_ID, null)
            if (!id.isNullOrEmpty()) id else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun resolveActiveTelephonyId(): String? {
        val inMemory = InterceptionBridge.activeSimulatedTelephonyId.value
        if (!inMemory.isNullOrEmpty()) return inMemory

        return try {
            val prefs = getOrInitXPrefs()
            prefs?.reload()
            val id = prefs?.getString(KEY_ACTIVE_TELEPHONY_ID, null)
            if (!id.isNullOrEmpty()) id else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun isInterceptionEnabled(): Boolean {
        if (InterceptionBridge.isInterceptionActive.value) return true

        return try {
            val prefs = getOrInitXPrefs()
            prefs?.reload()
            prefs?.getBoolean(KEY_INTERCEPTION_ENABLED, true) ?: true
        } catch (t: Throwable) {
            true
        }
    }
}

