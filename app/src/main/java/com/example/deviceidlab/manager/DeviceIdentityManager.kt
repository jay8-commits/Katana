package com.example.deviceidlab.manager

import android.content.Context
import com.example.deviceidlab.generator.RandomIdGenerator
import com.example.deviceidlab.model.DeviceProfile
import org.json.JSONObject

class DeviceIdentityManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("device_identity_prefs", Context.MODE_PRIVATE)

    fun getActiveProfile(): DeviceProfile {
        val json = prefs.getString("active_profile_json", null)
        if (json != null) {
            try {
                return parseProfile(json)
            } catch (_: Exception) {}
        }
        val defaultProfile = RandomIdGenerator.generateProfile("Default Profile")
        saveActiveProfile(defaultProfile)
        return defaultProfile
    }

    fun saveActiveProfile(profile: DeviceProfile) {
        val json = serializeProfile(profile)
        prefs.edit().putString("active_profile_json", json).apply()
    }

    private fun serializeProfile(p: DeviceProfile): String {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("androidId", p.androidId)
        obj.put("imei", p.imei)
        obj.put("serialNumber", p.serialNumber)
        obj.put("macAddress", p.macAddress)
        obj.put("buildModel", p.buildModel)
        obj.put("buildManufacturer", p.buildManufacturer)
        obj.put("buildBrand", p.buildBrand)
        obj.put("buildProduct", p.buildProduct)
        obj.put("buildDevice", p.buildDevice)
        obj.put("buildFingerprint", p.buildFingerprint)
        obj.put("createdAt", p.createdAt)
        return obj.toString()
    }

    private fun parseProfile(json: String): DeviceProfile {
        val obj = JSONObject(json)
        return DeviceProfile(
            id = obj.getString("id"),
            name = obj.getString("name"),
            androidId = obj.getString("androidId"),
            imei = obj.getString("imei"),
            serialNumber = obj.getString("serialNumber"),
            macAddress = obj.getString("macAddress"),
            buildModel = obj.getString("buildModel"),
            buildManufacturer = obj.getString("buildManufacturer"),
            buildBrand = obj.getString("buildBrand"),
            buildProduct = obj.getString("buildProduct"),
            buildDevice = obj.getString("buildDevice"),
            buildFingerprint = obj.getString("buildFingerprint"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
