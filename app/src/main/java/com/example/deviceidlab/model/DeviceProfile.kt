package com.example.deviceidlab.model

import java.io.Serializable
import java.security.MessageDigest

data class DeviceProfile(
    val id: String,
    val name: String,
    val androidId: String,
    val imei: String,
    val serialNumber: String,
    val macAddress: String,
    val buildModel: String,
    val buildManufacturer: String,
    val buildBrand: String,
    val buildProduct: String,
    val buildDevice: String,
    val buildFingerprint: String,
    val phoneNumber: String = "+1 (555) 234-5678",
    val batteryHealth: Int = 95,
    val testIpv4: String = "192.0.2.101",
    val createdAt: Long = System.currentTimeMillis(),
    val state: ProfileState = ProfileState.AVAILABLE,
    val consumedAt: Long? = null
) : Serializable {

    fun computeFingerprint(): String {
        val raw = "$id:$androidId:$imei:$serialNumber:$macAddress:$buildFingerprint:$phoneNumber:$batteryHealth:$testIpv4"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

