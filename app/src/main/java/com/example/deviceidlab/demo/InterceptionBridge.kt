package com.example.deviceidlab.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Local in-process bridge between the controlled test application [com.example.deviceidlab]
 * and the demo interception layer.
 *
 * Provides safe state distribution without network servers or background daemons.
 */
object InterceptionBridge {
    const val TARGET_PACKAGE = "com.example.deviceidlab"

    private val _isInterceptionActive = MutableStateFlow(true)
    val isInterceptionActive: StateFlow<Boolean> = _isInterceptionActive.asStateFlow()

    private val _activeSimulatedAndroidId = MutableStateFlow<String?>(null)
    val activeSimulatedAndroidId: StateFlow<String?> = _activeSimulatedAndroidId.asStateFlow()

    private val _activeSimulatedTelephonyId = MutableStateFlow<String?>(null)
    val activeSimulatedTelephonyId: StateFlow<String?> = _activeSimulatedTelephonyId.asStateFlow()

    private val _invocationLogs = MutableStateFlow<List<HookInvocationLog>>(emptyList())
    val invocationLogs: StateFlow<List<HookInvocationLog>> = _invocationLogs.asStateFlow()

    private val logBuffer = CopyOnWriteArrayList<HookInvocationLog>()

    fun setInterceptionActive(active: Boolean) {
        _isInterceptionActive.value = active
    }

    fun setSimulatedAndroidId(id: String?) {
        _activeSimulatedAndroidId.value = id
    }

    fun updateActiveSimulatedIds(androidTestId: String?, telephonyTestId: String?) {
        _activeSimulatedAndroidId.value = androidTestId
        _activeSimulatedTelephonyId.value = telephonyTestId
    }

    fun logInvocation(log: HookInvocationLog) {
        logBuffer.add(0, log)
        // Keep last 50 entries
        if (logBuffer.size > 50) {
            logBuffer.removeAt(logBuffer.size - 1)
        }
        _invocationLogs.value = logBuffer.toList()
    }

    fun clearLogs() {
        logBuffer.clear()
        _invocationLogs.value = emptyList()
    }
}
