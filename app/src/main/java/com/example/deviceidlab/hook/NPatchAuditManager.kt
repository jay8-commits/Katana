package com.example.deviceidlab.hook

import android.content.Context
import android.util.Log

data class HookAuditEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val targetPackage: String,
    val targetProcess: String,
    val targetPid: Int,
    val hookEntryStatus: String,
    val hookInstallationStatus: String,
    val canaryIntercepted: Boolean,
    val apiName: String,
    val originalId: String = "",
    val injectedId: String = "",
    val returnedId: String = "",
    val stage: String = NPatchAuditManager.HOOK_INVOKED
)

data class NPatchVerificationAudit(
    val moduleDetected: Boolean,
    val targetPackage: String,
    val targetProcess: String,
    val hookEntryStatus: String,
    val hookInstallationStatus: String,
    val canaryStatus: String,
    val lastHookTimestamp: Long,
    val finalResult: String,
    val isVerified: Boolean,
    val currentStage: String = NPatchAuditManager.TARGET_OBSERVED
)

/**
 * Singleton audit manager tracking runtime NPatch 1.0.7 hook registration,
 * invocation events, and 5-stage verification states.
 */
object NPatchAuditManager {
    private const val TAG = "NPatchAuditManager"

    // 5-Stage Verification Lifecycle States
    const val HOOK_REGISTERED = "HOOK_REGISTERED"
    const val HOOK_INVOKED = "HOOK_INVOKED"
    const val VALUE_GENERATED = "VALUE_GENERATED"
    const val VALUE_RETURNED = "VALUE_RETURNED"
    const val TARGET_OBSERVED = "TARGET_OBSERVED"

    private val auditEvents = mutableListOf<HookAuditEvent>()

    @Synchronized
    fun recordHookEvent(
        context: Context? = null,
        targetPackage: String,
        targetProcess: String,
        targetPid: Int,
        hookEntryStatus: String,
        hookInstallationStatus: String,
        canaryIntercepted: Boolean,
        apiName: String,
        originalId: String = "",
        injectedId: String = "",
        returnedId: String = "",
        stage: String = HOOK_INVOKED
    ) {
        val event = HookAuditEvent(
            targetPackage = targetPackage,
            targetProcess = targetProcess,
            targetPid = targetPid,
            hookEntryStatus = hookEntryStatus,
            hookInstallationStatus = hookInstallationStatus,
            canaryIntercepted = canaryIntercepted,
            apiName = apiName,
            originalId = originalId,
            injectedId = injectedId,
            returnedId = returnedId,
            stage = stage
        )
        auditEvents.add(event)
        if (auditEvents.size > 200) {
            auditEvents.removeAt(0)
        }

        Log.i(TAG, "[$TAG] [HOOK AUDIT] api='$apiName' in pkg='$targetPackage' (PID $targetPid) -> returned='$returnedId'")
    }

    @Synchronized
    fun getAuditEvents(): List<HookAuditEvent> = auditEvents.toList()

    @Synchronized
    fun getAuditEventsForPackage(pkg: String): List<HookAuditEvent> =
        auditEvents.filter { it.targetPackage == pkg }

    @Synchronized
    fun getVerificationAudit(targetPackage: String): NPatchVerificationAudit {
        val pkgEvents = auditEvents.filter { it.targetPackage == targetPackage }
        val lastEvent = pkgEvents.lastOrNull()
        val hasCanary = pkgEvents.any { it.canaryIntercepted }
        val hasExecution = pkgEvents.any { it.hookEntryStatus == "EXECUTED" }

        val isVerified = hasCanary && hasExecution
        return NPatchVerificationAudit(
            moduleDetected = hasExecution,
            targetPackage = targetPackage,
            targetProcess = lastEvent?.targetProcess ?: "unknown",
            hookEntryStatus = lastEvent?.hookEntryStatus ?: "NOT_EXECUTED",
            hookInstallationStatus = lastEvent?.hookInstallationStatus ?: "NOT_INSTALLED",
            canaryStatus = if (hasCanary) "PASS_CANARY_ACTIVE" else "FAIL_NO_CANARY",
            lastHookTimestamp = lastEvent?.timestamp ?: 0L,
            finalResult = if (isVerified) "VERIFIED" else "PENDING",
            isVerified = isVerified
        )
    }

    @Synchronized
    fun clearEvents() {
        auditEvents.clear()
    }
}
