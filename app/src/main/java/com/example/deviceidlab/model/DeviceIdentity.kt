package com.example.deviceidlab.model

/**
 * Represents a simulated device identity allocated from the 1..1,000,000 pool.
 *
 * @property identityNumber Numeric index between 1 and 1,000,000.
 * @property androidTestId Deterministic 16-character lowercase hexadecimal test identifier.
 * @property telephonyTestId Deterministic 15-character numeric test identifier.
 * @property createdAt Epoch timestamp in milliseconds when this identity was generated.
 */
data class DeviceIdentity(
    val identityNumber: Long,
    val androidTestId: String,
    val telephonyTestId: String,
    val createdAt: Long = System.currentTimeMillis()
)
