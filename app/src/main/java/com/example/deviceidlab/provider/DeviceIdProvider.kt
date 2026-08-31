package com.example.deviceidlab.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.example.deviceidlab.hook.NPatchAuditManager
import com.example.deviceidlab.hook.NPatchConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exported ContentProvider acting as the real-time runtime communication mechanism
 * between the DeviceIdRandomizationLab Controller and the embedded NPatch runtime
 * running inside target application processes.
 *
 * Architecture:
 * - Allows target APKs to be patched and embedded ONLY ONCE.
 * - Whenever the target process calls Settings.Secure.getString(..., "android_id") or
 *   TelephonyManager.getDeviceId() / getImei(), the embedded NPatch hook queries this
 *   ContentProvider using the ContentResolver already present in the hooked method call or Context.
 * - This provides instantaneous cross-UID / cross-process communication on Android 7.0 - 15+
 *   without requiring APK repatching, rebuilding, reinstalling, or device reboot.
 */
class DeviceIdProvider : ContentProvider() {

    companion object {
        const val TAG = "DeviceIdProvider"
        const val AUTHORITY = "com.example.deviceidlab.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

        const val METHOD_GET_CURRENT_TEST_ID = "GET_CURRENT_TEST_ID"
        const val METHOD_GET_CURRENT_TEST_IDS = "GET_CURRENT_TEST_IDS"
        const val METHOD_SET_CURRENT_TEST_ID = "SET_CURRENT_TEST_ID"
        const val METHOD_SET_CURRENT_TEST_IDS = "SET_CURRENT_TEST_IDS"
        const val METHOD_REPORT_INTERCEPTION = "REPORT_INTERCEPTION"

        const val KEY_TEST_ID = "test_id"
        const val KEY_ANDROID_TEST_ID = "android_test_id"
        const val KEY_TELEPHONY_TEST_ID = "telephony_test_id"
        const val KEY_INTERCEPTION_ENABLED = "interception_enabled"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_TARGET_PACKAGE = "target_package"
        const val KEY_TARGET_PROCESS = "target_process"
        const val KEY_TARGET_PID = "target_pid"
        const val KEY_API_NAME = "api_name"
        const val KEY_IDENTIFIER_TYPE = "identifier_type"
        const val KEY_ORIGINAL_ID = "original_id"
        const val KEY_RETURNED_ID = "returned_id"

        const val TYPE_ANDROID_ID = "ANDROID_ID"
        const val TYPE_TELEPHONY_ID = "TELEPHONY_ID"

        private val _currentAndroidTestIdFlow = MutableStateFlow("NPATCH_ANDROID_001")
        val currentAndroidTestIdFlow: StateFlow<String> = _currentAndroidTestIdFlow.asStateFlow()

        private val _currentTelephonyTestIdFlow = MutableStateFlow("NPATCH_TELEPHONY_001")
        val currentTelephonyTestIdFlow: StateFlow<String> = _currentTelephonyTestIdFlow.asStateFlow()

        // Backwards compatibility flow
        val currentTestIdFlow: StateFlow<String> = _currentAndroidTestIdFlow.asStateFlow()

        private val _lastInterceptedAndroidIdFlow = MutableStateFlow<String?>(null)
        val lastInterceptedAndroidIdFlow: StateFlow<String?> = _lastInterceptedAndroidIdFlow.asStateFlow()

        private val _lastInterceptedTelephonyIdFlow = MutableStateFlow<String?>(null)
        val lastInterceptedTelephonyIdFlow: StateFlow<String?> = _lastInterceptedTelephonyIdFlow.asStateFlow()

        // Backwards compatibility flow
        val lastInterceptedIdFlow: StateFlow<String?> = _lastInterceptedAndroidIdFlow.asStateFlow()

        private val _lastTargetReadAndroidIdFlow = MutableStateFlow<String?>(null)
        val lastTargetReadAndroidIdFlow: StateFlow<String?> = _lastTargetReadAndroidIdFlow.asStateFlow()

        private val _lastTargetReadTelephonyIdFlow = MutableStateFlow<String?>(null)
        val lastTargetReadTelephonyIdFlow: StateFlow<String?> = _lastTargetReadTelephonyIdFlow.asStateFlow()

        // Backwards compatibility flow
        val lastTargetReadFlow: StateFlow<String?> = _lastTargetReadAndroidIdFlow.asStateFlow()

        private val _targetProcessDetectedFlow = MutableStateFlow(false)
        val targetProcessDetectedFlow: StateFlow<Boolean> = _targetProcessDetectedFlow.asStateFlow()

        private val _lastTargetProcessNameFlow = MutableStateFlow("None")
        val lastTargetProcessNameFlow: StateFlow<String> = _lastTargetProcessNameFlow.asStateFlow()

        private val _lastTargetPidFlow = MutableStateFlow(0)
        val lastTargetPidFlow: StateFlow<Int> = _lastTargetPidFlow.asStateFlow()

        @Volatile
        var activeAndroidTestId: String = "NPATCH_ANDROID_001"
            private set

        @Volatile
        var activeTelephonyTestId: String = "NPATCH_TELEPHONY_001"
            private set

        // Backwards compatibility
        val activeTestId: String get() = activeAndroidTestId

        @Volatile
        var isInterceptionEnabled: Boolean = true
            private set

        fun updateTestIds(context: Context?, newAndroidId: String, newTelephonyId: String) {
            if (newAndroidId.isNotBlank()) {
                activeAndroidTestId = newAndroidId
                _currentAndroidTestIdFlow.value = newAndroidId
            }
            if (newTelephonyId.isNotBlank()) {
                activeTelephonyTestId = newTelephonyId
                _currentTelephonyTestIdFlow.value = newTelephonyId
            }
            Log.i(TAG, "[$TAG] [NPATCH] Current test IDs updated: Android='$newAndroidId', Telephony='$newTelephonyId'")

            if (context != null) {
                try {
                    val prefs = context.getSharedPreferences(NPatchConfig.PREF_FILE, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString(NPatchConfig.KEY_ACTIVE_ANDROID_ID, activeAndroidTestId)
                        .putString(NPatchConfig.KEY_ACTIVE_TELEPHONY_ID, activeTelephonyTestId)
                        .putBoolean(NPatchConfig.KEY_INTERCEPTION_ENABLED, true)
                        .apply()
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to persist to prefs: ${t.message}")
                }
            }
        }

        fun updateTestId(context: Context?, newId: String) {
            updateTestIds(context, newId, activeTelephonyTestId)
        }

        fun updateTelephonyTestId(context: Context?, newId: String) {
            updateTestIds(context, activeAndroidTestId, newId)
        }
    }

    override fun onCreate(): Boolean {
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(NPatchConfig.PREF_FILE, Context.MODE_PRIVATE)
                val savedAndroidId = prefs.getString(NPatchConfig.KEY_ACTIVE_ANDROID_ID, null)
                if (!savedAndroidId.isNullOrEmpty()) {
                    activeAndroidTestId = savedAndroidId
                    _currentAndroidTestIdFlow.value = savedAndroidId
                }
                val savedTelephonyId = prefs.getString(NPatchConfig.KEY_ACTIVE_TELEPHONY_ID, null)
                if (!savedTelephonyId.isNullOrEmpty()) {
                    activeTelephonyTestId = savedTelephonyId
                    _currentTelephonyTestIdFlow.value = savedTelephonyId
                }
            } catch (_: Throwable) {}
        }
        Log.i(TAG, "[$TAG] [NPATCH] DeviceIdProvider initialized. Active Android: '$activeAndroidTestId', Telephony: '$activeTelephonyTestId'")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val result = Bundle()
        val callerPkg = try { callingPackage } catch (_: Throwable) { "unknown" }

        when (method) {
            METHOD_GET_CURRENT_TEST_IDS, METHOD_GET_CURRENT_TEST_ID, "get_current_test_id" -> {
                Log.d(TAG, "[$TAG] [NPATCH] GET_CURRENT_TEST_IDS requested by caller '$callerPkg' -> Android='$activeAndroidTestId', Telephony='$activeTelephonyTestId'")
                result.putString(KEY_TEST_ID, activeAndroidTestId)
                result.putString(KEY_ANDROID_TEST_ID, activeAndroidTestId)
                result.putString(KEY_TELEPHONY_TEST_ID, activeTelephonyTestId)
                result.putBoolean(KEY_INTERCEPTION_ENABLED, isInterceptionEnabled)
                result.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }
            METHOD_SET_CURRENT_TEST_IDS -> {
                val newAndroidId = extras?.getString(KEY_ANDROID_TEST_ID) ?: arg ?: activeAndroidTestId
                val newTelephonyId = extras?.getString(KEY_TELEPHONY_TEST_ID) ?: activeTelephonyTestId
                updateTestIds(context, newAndroidId, newTelephonyId)
                result.putBoolean("success", true)
                result.putString(KEY_ANDROID_TEST_ID, activeAndroidTestId)
                result.putString(KEY_TELEPHONY_TEST_ID, activeTelephonyTestId)
            }
            METHOD_SET_CURRENT_TEST_ID, "set_current_test_id" -> {
                val newId = extras?.getString(KEY_TEST_ID) ?: arg
                if (!newId.isNullOrBlank()) {
                    updateTestId(context, newId)
                    result.putBoolean("success", true)
                    result.putString(KEY_TEST_ID, activeAndroidTestId)
                } else {
                    result.putBoolean("success", false)
                }
            }
            METHOD_REPORT_INTERCEPTION, "report_interception" -> {
                val targetPkg = extras?.getString(KEY_TARGET_PACKAGE) ?: callerPkg ?: "unknown_package"
                val targetProc = extras?.getString(KEY_TARGET_PROCESS) ?: targetPkg
                val targetPid = extras?.getInt(KEY_TARGET_PID, 0) ?: 0
                val apiName = extras?.getString(KEY_API_NAME) ?: "Settings.Secure.getString(android_id)"
                val idType = extras?.getString(KEY_IDENTIFIER_TYPE) ?: (
                    if (apiName.contains("Telephony", ignoreCase = true) || apiName.contains("getDeviceId", ignoreCase = true) || apiName.contains("getImei", ignoreCase = true)) {
                        TYPE_TELEPHONY_ID
                    } else {
                        TYPE_ANDROID_ID
                    }
                )
                val origId = extras?.getString(KEY_ORIGINAL_ID) ?: ""
                val retId = extras?.getString(KEY_RETURNED_ID) ?: (if (idType == TYPE_TELEPHONY_ID) activeTelephonyTestId else activeAndroidTestId)

                _targetProcessDetectedFlow.value = true
                _lastTargetProcessNameFlow.value = targetProc
                _lastTargetPidFlow.value = targetPid

                if (idType == TYPE_TELEPHONY_ID) {
                    _lastInterceptedTelephonyIdFlow.value = retId
                    _lastTargetReadTelephonyIdFlow.value = retId
                } else {
                    _lastInterceptedAndroidIdFlow.value = retId
                    _lastTargetReadAndroidIdFlow.value = retId
                }

                NPatchAuditManager.recordHookEvent(
                    context = context,
                    targetPackage = targetPkg,
                    targetProcess = targetProc,
                    targetPid = targetPid,
                    hookEntryStatus = "EXECUTED",
                    hookInstallationStatus = "ACTIVE_INTERCEPTION",
                    canaryIntercepted = true,
                    apiName = apiName,
                    originalId = origId,
                    injectedId = if (idType == TYPE_TELEPHONY_ID) activeTelephonyTestId else activeAndroidTestId,
                    returnedId = retId
                )

                Log.i(TAG, "[$TAG] [NPATCH] Interception reported [$idType]: target='$targetPkg' (PID $targetPid), api='$apiName', returnedId='$retId'")
                result.putBoolean("success", true)
            }
            else -> {
                result.putString(KEY_TEST_ID, activeAndroidTestId)
                result.putString(KEY_ANDROID_TEST_ID, activeAndroidTestId)
                result.putString(KEY_TELEPHONY_TEST_ID, activeTelephonyTestId)
            }
        }
        return result
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.com.example.deviceidlab.testid"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}

