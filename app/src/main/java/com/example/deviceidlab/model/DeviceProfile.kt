package com.example.deviceidlab.model

import java.io.Serializable

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
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
