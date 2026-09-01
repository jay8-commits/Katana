package com.example.deviceidlab.model

data class ProfileActivationResult(
    val success: Boolean,
    val profile: DeviceProfile,
    val message: String,
    val wasRejected: Boolean = false,
    val rejectionReason: String? = null
)
