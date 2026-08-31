package com.example.deviceidlab.demo

/**
 * Educational Hook / Interception Demonstration Layer.
 *
 * Scoped STRICTLY and EXCLUSIVELY to package `com.example.deviceidlab`.
 *
 * Demonstrates how a controlled test environment can intercept device identifier APIs
 * and return simulated test values instead of hardware or platform identifiers.
 */
object DeviceIdHookDemo {

    private const val TARGET_PACKAGE = "com.example.deviceidlab"
    private const val ANDROID_ID_NAME = "android_id"

    /**
     * Intercepts `Settings.Secure.getString(ContentResolver, name)`.
     *
     * Flow:
     * 1. Validate caller package is [TARGET_PACKAGE]. If not, pass through.
     * 2. Check if interception is enabled. If not, pass through.
     * 3. If requested parameter is `android_id` and test ID is available -> Substitute TEST_ANDROID_ID.
     * 4. Otherwise pass through original value.
     */
    fun interceptSettingsSecureGetString(
        callerPackage: String,
        settingName: String,
        originalProvider: () -> String?
    ): String? {
        val originalValue = try { originalProvider() } catch (e: Exception) { null }

        // Security check: Only operate on controlled test application
        if (callerPackage != TARGET_PACKAGE) {
            InterceptionBridge.logInvocation(
                HookInvocationLog(
                    callerPackage = callerPackage,
                    targetApi = "Settings.Secure.getString()",
                    requestedParam = settingName,
                    returnedValue = originalValue ?: "null",
                    wasIntercepted = false,
                    reason = "Caller package is not target ($callerPackage != $TARGET_PACKAGE). Passed through."
                )
            )
            return originalValue
        }

        if (!InterceptionBridge.isInterceptionActive.value) {
            InterceptionBridge.logInvocation(
                HookInvocationLog(
                    callerPackage = callerPackage,
                    targetApi = "Settings.Secure.getString()",
                    requestedParam = settingName,
                    returnedValue = originalValue ?: "null",
                    wasIntercepted = false,
                    reason = "Interception layer is currently disabled in settings."
                )
            )
            return originalValue
        }

        if (settingName.equals(ANDROID_ID_NAME, ignoreCase = true)) {
            val simulatedId = InterceptionBridge.activeSimulatedAndroidId.value
            if (!simulatedId.isNullOrEmpty()) {
                InterceptionBridge.logInvocation(
                    HookInvocationLog(
                        callerPackage = callerPackage,
                        targetApi = "Settings.Secure.getString()",
                        requestedParam = settingName,
                        returnedValue = simulatedId,
                        wasIntercepted = true,
                        reason = "Substituted simulated test Android ID."
                    )
                )
                return simulatedId
            }
        }

        // Other settings keys pass through untouched
        InterceptionBridge.logInvocation(
            HookInvocationLog(
                callerPackage = callerPackage,
                targetApi = "Settings.Secure.getString()",
                requestedParam = settingName,
                returnedValue = originalValue ?: "null",
                wasIntercepted = false,
                reason = "Setting key is not ANDROID_ID. Original value returned."
            )
        )
        return originalValue
    }

    /**
     * Intercepts `TelephonyManager.getDeviceId()` / `TelephonyManager.getImei()`.
     *
     * Flow:
     * 1. Validate caller package is [TARGET_PACKAGE].
     * 2. If interception enabled and simulated telephony ID is available -> Substitute TEST_TELEPHONY_ID.
     * 3. Otherwise execute original provider or return original result.
     */
    fun interceptTelephonyGetDeviceId(
        callerPackage: String,
        originalProvider: () -> String?
    ): String? {
        val originalValue = try { originalProvider() } catch (e: Exception) { "Restricted / SecurityException" }

        // Security check: Only operate on controlled test application
        if (callerPackage != TARGET_PACKAGE) {
            InterceptionBridge.logInvocation(
                HookInvocationLog(
                    callerPackage = callerPackage,
                    targetApi = "TelephonyManager.getDeviceId()",
                    requestedParam = "NONE",
                    returnedValue = originalValue ?: "null",
                    wasIntercepted = false,
                    reason = "Caller package is not target ($callerPackage != $TARGET_PACKAGE)."
                )
            )
            return originalValue
        }

        if (!InterceptionBridge.isInterceptionActive.value) {
            InterceptionBridge.logInvocation(
                HookInvocationLog(
                    callerPackage = callerPackage,
                    targetApi = "TelephonyManager.getDeviceId()",
                    requestedParam = "NONE",
                    returnedValue = originalValue ?: "null",
                    wasIntercepted = false,
                    reason = "Interception layer is currently disabled in settings."
                )
            )
            return originalValue
        }

        val simulatedTelephonyId = InterceptionBridge.activeSimulatedTelephonyId.value
        if (!simulatedTelephonyId.isNullOrEmpty()) {
            InterceptionBridge.logInvocation(
                HookInvocationLog(
                    callerPackage = callerPackage,
                    targetApi = "TelephonyManager.getDeviceId()",
                    requestedParam = "NONE",
                    returnedValue = simulatedTelephonyId,
                    wasIntercepted = true,
                    reason = "Substituted simulated test Telephony ID (bypassing restriction for test APK)."
                )
            )
            return simulatedTelephonyId
        }

        return originalValue
    }
}
