package com.example.deviceidlab.manager

import android.content.Context
import com.example.deviceidlab.generator.RandomIdGenerator
import com.example.deviceidlab.model.DeviceProfile
import com.example.deviceidlab.model.ProfileActivationResult
import com.example.deviceidlab.model.ProfileState
import org.json.JSONArray
import org.json.JSONObject

class DeviceIdentityManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("device_identity_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_PROFILE_JSON = "active_profile_json"
        private const val KEY_CONSUMED_FINGERPRINTS = "consumed_profile_fingerprints"
        private const val KEY_CONSUMED_METADATA_JSON = "consumed_profiles_metadata_json"
    }

    /**
     * Checks if a profile (by ID or fingerprint) has already been consumed/exempted.
     */
    fun isProfileConsumed(profile: DeviceProfile): Boolean {
        val consumedSet = getConsumedFingerprints()
        val fingerprint = profile.computeFingerprint()
        return consumedSet.contains(fingerprint) || consumedSet.contains(profile.id)
    }

    /**
     * Returns all consumed fingerprints and IDs persisted in storage.
     */
    fun getConsumedFingerprints(): Set<String> {
        val set = prefs.getStringSet(KEY_CONSUMED_FINGERPRINTS, null) ?: emptySet()
        return HashSet(set)
    }

    /**
     * Retrieves the currently active profile. If none exists, generates and activates a fresh one.
     */
    fun getActiveProfile(): DeviceProfile {
        val json = prefs.getString(KEY_ACTIVE_PROFILE_JSON, null)
        if (json != null) {
            try {
                return parseProfile(json)
            } catch (_: Exception) {}
        }
        val defaultProfile = generateAvailableProfile("Default Profile")
        applyAndActivateProfile(defaultProfile)
        return defaultProfile
    }

    /**
     * Generates a guaranteed-available profile that has NEVER been consumed.
     */
    fun generateAvailableProfile(name: String = "Generated Profile"): DeviceProfile {
        var attempts = 0
        while (attempts < 100) {
            val candidate = RandomIdGenerator.generateProfile(name)
            if (!isProfileConsumed(candidate)) {
                return candidate.copy(state = ProfileState.AVAILABLE)
            }
            attempts++
        }
        // Extremely rare fallback: append high-entropy nonce
        val candidate = RandomIdGenerator.generateProfile("$name (${System.currentTimeMillis()})")
        return candidate.copy(state = ProfileState.AVAILABLE)
    }

    /**
     * Attempts to activate a profile and mark it as CONSUMED upon successful activation.
     * Enforces the invariant:
     * - If the profile is already consumed, activation is REJECTED.
     * - If activation succeeds, state transitions to CONSUMED and is permanently persisted.
     * - If activation fails, profile is NOT marked as consumed.
     */
    fun applyAndActivateProfile(profile: DeviceProfile): ProfileActivationResult {
        // Check for duplicate / already consumed
        if (isProfileConsumed(profile)) {
            return ProfileActivationResult(
                success = false,
                profile = profile.copy(state = ProfileState.CONSUMED),
                message = "REJECTED: Profile '${profile.name}' (ID: ${profile.id}) has already been consumed/exempted.",
                wasRejected = true,
                rejectionReason = "PROFILE_ALREADY_CONSUMED_OR_EXEMPTED"
            )
        }

        try {
            val now = System.currentTimeMillis()
            val activatedProfile = profile.copy(
                state = ProfileState.CONSUMED,
                consumedAt = now
            )

            // 1. Persist active profile JSON
            val json = serializeProfile(activatedProfile)
            val editor = prefs.edit()
            editor.putString(KEY_ACTIVE_PROFILE_JSON, json)

            // 2. Persist to consumed fingerprints set
            val currentSet = HashSet(getConsumedFingerprints())
            currentSet.add(activatedProfile.computeFingerprint())
            currentSet.add(activatedProfile.id)
            editor.putStringSet(KEY_CONSUMED_FINGERPRINTS, currentSet)

            // 3. Persist audit metadata for consumed history
            val metadataList = getConsumedProfilesMetadata()
            val metaObj = JSONObject().apply {
                put("id", activatedProfile.id)
                put("name", activatedProfile.name)
                put("fingerprint", activatedProfile.computeFingerprint())
                put("androidId", activatedProfile.androidId)
                put("imei", activatedProfile.imei)
                put("createdAt", activatedProfile.createdAt)
                put("consumedAt", now)
                put("state", ProfileState.CONSUMED.name)
            }
            metadataList.put(metaObj)
            editor.putString(KEY_CONSUMED_METADATA_JSON, metadataList.toString())

            val committed = editor.commit() // Synchronous commit to ensure durability across process crashes
            if (!committed) {
                return ProfileActivationResult(
                    success = false,
                    profile = profile,
                    message = "FAILED: SharedPreferences storage commit failed. Profile remains AVAILABLE.",
                    wasRejected = false,
                    rejectionReason = "STORAGE_COMMIT_FAILURE"
                )
            }

            return ProfileActivationResult(
                success = true,
                profile = activatedProfile,
                message = "SUCCESS: Profile '${activatedProfile.name}' successfully activated and permanently marked as CONSUMED.",
                wasRejected = false
            )
        } catch (e: Throwable) {
            return ProfileActivationResult(
                success = false,
                profile = profile,
                message = "FAILED: Exception during profile activation: ${e.message}. Profile remains AVAILABLE.",
                wasRejected = false,
                rejectionReason = "ACTIVATION_EXCEPTION_${e.javaClass.simpleName}"
            )
        }
    }

    /**
     * Legacy helper method for backwards-compatibility; wraps applyAndActivateProfile.
     */
    fun saveActiveProfile(profile: DeviceProfile) {
        applyAndActivateProfile(profile)
    }

    fun getConsumedProfilesMetadata(): JSONArray {
        val raw = prefs.getString(KEY_CONSUMED_METADATA_JSON, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    fun getConsumedCount(): Int {
        return getConsumedFingerprints().size / 2 // Accounts for both id and sha256 fingerprint
    }

    fun serializeProfile(p: DeviceProfile): String {
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
        obj.put("state", p.state.name)
        if (p.consumedAt != null) {
            obj.put("consumedAt", p.consumedAt)
        }
        return obj.toString()
    }

    fun parseProfile(json: String): DeviceProfile {
        val obj = JSONObject(json)
        val stateStr = obj.optString("state", ProfileState.CONSUMED.name)
        val state = try {
            ProfileState.valueOf(stateStr)
        } catch (_: Exception) {
            ProfileState.CONSUMED
        }
        val consumedAt = if (obj.has("consumedAt")) obj.getLong("consumedAt") else null

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
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            state = state,
            consumedAt = consumedAt
        )
    }
}
