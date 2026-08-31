package com.example.deviceidlab.hook

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import com.example.deviceidlab.demo.HookInvocationLog
import com.example.deviceidlab.demo.InterceptionBridge
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
 * Referenced in assets/xposed_init and dynamically loaded by NPatch 1.0.7
 * when target applications or processes are launched.
 */
class NPatchHookEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "NPatchHookEntry"
        const val PREF_FILE = "npatch_config"
        const val KEY_ACTIVE_ANDROID_ID = "active_android_id"
        const val KEY_ACTIVE_TELEPHONY_ID = "active_telephony_id"
        const val KEY_INTERCEPTION_ENABLED = "interception_enabled"
        const val KEY_TARGET_PACKAGE_FILTER = "target_package_filter"

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
        val msg = "[$TAG] [NPATCH INIT] [NPATCH STAGE: MODULE_INIT] NPatch 1.0.7 module initialized in Zygote process (path=${startupParam?.modulePath})"
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

        val loadLog = "[$TAG] [NPATCH LOAD] [NPATCH STAGE: PROCESS_LOAD] handleLoadPackage invoked: pkg='${lpparam.packageName}', process='${lpparam.processName}', isFirstApp=${lpparam.isFirstApplication}"
        Log.i(TAG, loadLog)
        XposedBridge.log(loadLog)

        val pid = try { android.os.Process.myPid() } catch (_: Throwable) { 0 }
        val targetLog = "[$TAG] [NPATCH TARGET] [NPATCH STAGE: PROCESS_LOAD] Target process recorded: process='${lpparam.processName}', package='${lpparam.packageName}', PID=$pid, classLoader=${lpparam.classLoader.javaClass.simpleName}"
        Log.i(TAG, targetLog)
        XposedBridge.log(targetLog)

        var hookSuccessCount = 0

        // 1. Hook canary diagnostic method to prove live framework bytecode interception
        try {
            XposedHelpers.findAndHookMethod(
                "com.example.deviceidlab.DeviceIdReader",
                lpparam.classLoader,
                "isNpatchHookActive",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam?): Any {
                        val canaryLog = "[$TAG] [NPATCH CANARY] [NPATCH STAGE: API_INTERCEPT] isNpatchHookActive() intercepted in process='${lpparam.processName}' (PID=$pid, pkg='${lpparam.packageName}') -> returning true"
                        Log.i(TAG, canaryLog)
                        XposedBridge.log(canaryLog)

                        val verifiedLog = "[$TAG] [NPATCH VERIFIED] Dynamic canary verification confirmed inside target process '${lpparam.processName}' (PID=$pid)"
                        Log.i(TAG, verifiedLog)
                        XposedBridge.log(verifiedLog)

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
            val canaryHookLog = "[$TAG] [NPATCH HOOK] [NPATCH STAGE: HOOK_INSTALL] Installed hook on DeviceIdReader.isNpatchHookActive() in pkg='${lpparam.packageName}'"
            Log.i(TAG, canaryHookLog)
            XposedBridge.log(canaryHookLog)
        } catch (t: Throwable) {
            // Expected if hooking external target application where DeviceIdReader does not exist
            Log.d(TAG, "[$TAG] [NPATCH HOOK] DeviceIdReader not found in ${lpparam.packageName} (external target package): ${t.message}")
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
                            val activeSimulatedId = resolveActiveAndroidId()
                            if (isInterceptionEnabled() && !activeSimulatedId.isNullOrEmpty()) {
                                val originalValue = param.result as? String ?: "null"
                                param.result = activeSimulatedId

                                // Standardized NPatch Spoof log
                                val spoofLog = "[$TAG] [NPATCH SPOOF] [NPATCH STAGE: API_INTERCEPT] Package: '${lpparam.packageName}', Process: '${lpparam.processName}', PID: $pid, Original Android ID: '$originalValue', Spoofed Android ID: '$activeSimulatedId'"
                                Log.i(TAG, spoofLog)
                                XposedBridge.log(spoofLog)

                                InterceptionBridge.logInvocation(
                                    HookInvocationLog(
                                        callerPackage = lpparam.packageName,
                                        targetApi = "Settings.Secure.getString(android_id)",
                                        requestedParam = settingName,
                                        returnedValue = activeSimulatedId,
                                        wasIntercepted = true,
                                        reason = "NPatch 1.0.7 / Xposed module substituted randomized Android ID."
                                    )
                                )

                                NPatchAuditManager.recordHookEvent(
                                    context = null,
                                    targetPackage = lpparam.packageName,
                                    targetProcess = lpparam.processName,
                                    targetPid = pid,
                                    hookEntryStatus = "EXECUTED",
                                    hookInstallationStatus = "ACTIVE_INTERCEPTION",
                                    canaryIntercepted = true,
                                    apiName = "Settings.Secure.getString(android_id)",
                                    originalId = originalValue,
                                    injectedId = activeSimulatedId,
                                    returnedId = activeSimulatedId
                                )
                            }
                        }
                    }
                }
            )
            hookSuccessCount++
            val settingsLog = "[$TAG] [NPATCH HOOK] [NPATCH STAGE: HOOK_INSTALL] Installed hook on Settings.Secure.getString in pkg='${lpparam.packageName}' (process='${lpparam.processName}')"
            Log.i(TAG, settingsLog)
            XposedBridge.log(settingsLog)
        } catch (t: Throwable) {
            val errLog = "[$TAG] [NPATCH FAILURE] [NPATCH STAGE: HOOK_INSTALL] Failed to hook Settings.Secure.getString in pkg='${lpparam.packageName}': ${t.message}"
            Log.e(TAG, errLog, t)
            XposedBridge.log(errLog)
        }

        // 3. Hook TelephonyManager methods
        hookTelephonyMethods(lpparam)

        // Record initial hook registration in audit manager
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
    }

    private fun applyTelephonyOverride(
        packageName: String,
        processName: String,
        param: XC_MethodHook.MethodHookParam,
        apiName: String
    ) {
        if (!isInterceptionEnabled()) return

        val activeSimulatedTelephony = resolveActiveTelephonyId() ?: "860123456789012"
        param.result = activeSimulatedTelephony
        val msg = "[$TAG] [NPATCH HOOK] [NPATCH VERIFIED] $apiName -> '$activeSimulatedTelephony' in pkg='$packageName', process='$processName'"
        Log.i(TAG, msg)
        XposedBridge.log(msg)

        InterceptionBridge.logInvocation(
            HookInvocationLog(
                callerPackage = packageName,
                targetApi = "TelephonyManager.$apiName",
                requestedParam = "NONE",
                returnedValue = activeSimulatedTelephony,
                wasIntercepted = true,
                reason = "NPatch 1.0.7 / Xposed module substituted randomized Telephony ID."
            )
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
