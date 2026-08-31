package com.example.deviceidlab

import com.example.deviceidlab.model.DeviceIdentity
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Deterministic test identifier generator.
 *
 * Ensures that for any given identity index (1..1,000,000), the generated test
 * identifiers are 100% reproducible across application restarts and database queries.
 */
object RandomIdGenerator {

    private const val ANDROID_ID_SALT = "AndroidID_Deterministic_Salt_Lab_v1_"
    private const val TELEPHONY_ID_SALT = "Telephony_Deterministic_Salt_Lab_v1_"

    /**
     * Generates a deterministic 16-hexadecimal character test Android ID
     * for the given identity index.
     *
     * Example: "91d04b7e8fa3c2d1"
     */
    fun generateAndroidTestId(identityNumber: Long): String {
        val input = "$ANDROID_ID_SALT$identityNumber".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        // Take first 8 bytes = 16 hex characters
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a deterministic 15-numeric character test Telephony/IMEI identifier
     * for the given identity index.
     *
     * Example: "384729105837421"
     */
    fun generateTelephonyTestId(identityNumber: Long): String {
        val input = "$TELEPHONY_ID_SALT$identityNumber".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        val bigInt = BigInteger(1, digest)
        val modulus = BigInteger("1000000000000000") // 10^15
        val rawNum = bigInt.mod(modulus).toString().padStart(15, '0')
        // Ensure non-zero leading digit (e.g., standard cellular TAC prefix 35 or 86)
        return if (rawNum.startsWith("0")) "3" + rawNum.substring(1) else rawNum
    }

    /**
     * Constructs a full [DeviceIdentity] for the specified identity number.
     */
    fun createIdentity(
        identityNumber: Long,
        createdAt: Long = System.currentTimeMillis()
    ): DeviceIdentity {
        return DeviceIdentity(
            identityNumber = identityNumber,
            androidTestId = generateAndroidTestId(identityNumber),
            telephonyTestId = generateTelephonyTestId(identityNumber),
            createdAt = createdAt
        )
    }
}
