package com.example.deviceidlab.demo

/**
 * Record of an intercepted method call executed by the demo interception layer.
 */
data class HookInvocationLog(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val callerPackage: String,
    val targetApi: String,
    val requestedParam: String,
    val returnedValue: String,
    val wasIntercepted: Boolean,
    val reason: String
)
