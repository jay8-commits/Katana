package com.example.deviceidlab.demo

/**
 * Educational Adapter documenting NPatch / Xposed runtime hooking integration.
 *
 * In an environment equipped with runtime method hooking (such as NPatch, LSPosed,
 * or SandHook), this class demonstrates how the entry point hook would attach to
 * [com.example.deviceidlab] without hardcoding or requiring third-party runtime binaries.
 *
 * ARCHITECTURAL FLOW:
 *
 * 1. Module Load Phase:
 *    - The hooking framework invokes the module with `loadPackageParam`.
 *    - We strictly verify: `if (loadPackageParam.packageName != "com.example.deviceidlab") return;`
 *
 * 2. Hook Injection Phase:
 *    - Hook Target 1: `android.provider.Settings.Secure.getString(ContentResolver, String)`
 *      Before / After Hook:
 *      if (param.args[1] == "android_id") {
 *          param.result = InterceptionBridge.activeSimulatedAndroidId.value;
 *      }
 *
 *    - Hook Target 2: `android.telephony.TelephonyManager.getDeviceId()`
 *      param.result = InterceptionBridge.activeSimulatedTelephonyId.value;
 *
 * 3. In-App Safe Execution:
 *    - In our standard test environment, [DeviceIdHookDemo] provides the in-process
 *      simulation of this exact behavior without requiring rooted device instrumentation.
 */
class NPatchAdapter {

    val targetPackage: String = InterceptionBridge.TARGET_PACKAGE

    fun isTargetApplicable(packageName: String): Boolean {
        return packageName == targetPackage
    }

    /**
     * Simulates hook invocation for Settings.Secure.
     */
    fun onInterceptSettingsString(packageName: String, key: String, originalValue: String?): String? {
        return DeviceIdHookDemo.interceptSettingsSecureGetString(
            callerPackage = packageName,
            settingName = key,
            originalProvider = { originalValue }
        )
    }

    /**
     * Simulates hook invocation for TelephonyManager.
     */
    fun onInterceptTelephony(packageName: String, originalValue: String?): String? {
        return DeviceIdHookDemo.interceptTelephonyGetDeviceId(
            callerPackage = packageName,
            originalProvider = { originalValue }
        )
    }
}
