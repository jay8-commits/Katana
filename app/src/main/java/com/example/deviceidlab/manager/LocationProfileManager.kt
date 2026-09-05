package com.example.deviceidlab.manager

import android.content.Context
import android.util.Log
import com.example.deviceidlab.model.CityRecord
import com.example.deviceidlab.model.LocationProfile
import com.example.deviceidlab.model.WorldCityDatabase
import com.example.deviceidlab.model.WorldwideLocationProfile

/**
 * Controller-side manager for storing and configuring Worldwide Location & Synthetic IP Profiles.
 *
 * Supports three city selection modes:
 * 1. MANUAL CITY: Explicitly select a city from the catalog.
 * 2. RANDOM WORLD CITY: Select any city from the global catalog (optionally with seed).
 * 3. RANDOM COUNTRY -> CITY: Pick country, then random city in that country.
 *
 * Lifecycle:
 * - Newly generated profiles start as AVAILABLE.
 * - Calling activateWorldwideProfile advances the current profile to CONSUMED,
 *   and sets the new profile to ACTIVE.
 */
class LocationProfileManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("location_profile_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "LocationProfileManager"
        private const val KEY_ACTIVE_LOCATION_JSON = "active_location_json"
        private const val KEY_ACTIVE_WORLDWIDE_JSON = "active_worldwide_json"
        private const val KEY_PREVIOUS_WORLDWIDE_JSON = "previous_worldwide_json"
    }

    /**
     * Retrieves the currently active WorldwideLocationProfile.
     * Defaults to Tokyo preset if none is configured.
     */
    fun getActiveWorldwideProfile(): WorldwideLocationProfile {
        val json = prefs.getString(KEY_ACTIVE_WORLDWIDE_JSON, null)
        if (!json.isNullOrEmpty()) {
            try {
                return LocationJsonSerializer.parseWorldwide(json)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse stored WorldwideLocationProfile JSON, falling back to default preset", e)
            }
        }
        return WorldwideLocationProfile.PRESET_TOKYO
    }

    /**
     * Backward-compatible getter returning standard LocationProfile.
     */
    fun getActiveLocation(): LocationProfile {
        return getActiveWorldwideProfile().toLocationProfile()
    }

    /**
     * Sets and activates a new WorldwideLocationProfile.
     * Marks previous profile as CONSUMED and current as ACTIVE.
     */
    fun activateWorldwideProfile(newProfile: WorldwideLocationProfile): WorldwideLocationProfile {
        val current = getActiveWorldwideProfile()
        val consumed = current.copy(state = "CONSUMED")
        val consumedJson = LocationJsonSerializer.serializeWorldwide(consumed)

        val activated = newProfile.copy(
            state = "ACTIVE",
            timestamp = System.currentTimeMillis(),
            elapsedRealtimeNanos = System.nanoTime()
        )
        val activatedJson = LocationJsonSerializer.serializeWorldwide(activated)

        prefs.edit()
            .putString(KEY_PREVIOUS_WORLDWIDE_JSON, consumedJson)
            .putString(KEY_ACTIVE_WORLDWIDE_JSON, activatedJson)
            .putString(KEY_ACTIVE_LOCATION_JSON, LocationJsonSerializer.serialize(activated.toLocationProfile()))
            .apply()

        Log.d(TAG, "Active Worldwide Profile set to: ${activated.city}, ${activated.country} [${activated.syntheticIp}]")
        return activated
    }

    fun getPreviousWorldwideProfile(): WorldwideLocationProfile? {
        val json = prefs.getString(KEY_PREVIOUS_WORLDWIDE_JSON, null) ?: return null
        return try {
            LocationJsonSerializer.parseWorldwide(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * MODE 1: MANUAL CITY SELECTION
     */
    fun selectManualCity(cityId: String): WorldwideLocationProfile {
        val city = WorldCityDatabase.findById(cityId)
            ?: throw IllegalArgumentException("City with ID '$cityId' not found.")
        val profile = buildProfileFromCity(city, state = "AVAILABLE")
        return activateWorldwideProfile(profile)
    }

    /**
     * MODE 2: RANDOM WORLD CITY SELECTION
     */
    fun selectRandomWorldCity(seed: Long? = null): WorldwideLocationProfile {
        val city = WorldCityDatabase.getRandomCity(seed)
        val profile = buildProfileFromCity(city, seed = seed, state = "AVAILABLE")
        return activateWorldwideProfile(profile)
    }

    /**
     * MODE 3: RANDOM COUNTRY -> CITY SELECTION
     */
    fun selectRandomCityInCountry(country: String, seed: Long? = null): WorldwideLocationProfile {
        val city = WorldCityDatabase.getRandomCityInCountry(country, seed)
            ?: throw IllegalArgumentException("No cities found for country '$country'.")
        val profile = buildProfileFromCity(city, seed = seed, state = "AVAILABLE")
        return activateWorldwideProfile(profile)
    }

    /**
     * Generates a new profile for a city without activating it immediately.
     */
    fun generateAvailableProfile(city: CityRecord, seed: Long? = null): WorldwideLocationProfile {
        return buildProfileFromCity(city, seed = seed, state = "AVAILABLE")
    }

    /**
     * Resets active profile to Tokyo preset.
     */
    fun clearProfile(): WorldwideLocationProfile {
        return activateWorldwideProfile(WorldwideLocationProfile.PRESET_TOKYO)
    }

    private fun buildProfileFromCity(
        city: CityRecord,
        seed: Long? = null,
        state: String = "AVAILABLE"
    ): WorldwideLocationProfile {
        val ip = SyntheticIpGenerator.generateTestNet3(seed)
        val pid = "loc_${city.id}_${System.currentTimeMillis().toString(36)}"
        return WorldwideLocationProfile(
            profileId = pid,
            city = city.city,
            country = city.country,
            countryCode = city.countryCode,
            latitude = city.latitude,
            longitude = city.longitude,
            timezone = city.timezone,
            provider = "gps",
            altitude = 20.0,
            accuracy = 3.5f,
            speed = 0.0f,
            bearing = 0.0f,
            syntheticIp = ip,
            state = state
        )
    }

    /**
     * Backward-compatible setter.
     */
    fun setActiveLocation(profile: LocationProfile) {
        val worldwide = WorldwideLocationProfile(
            profileId = profile.profileId,
            city = "Custom Location",
            country = "Custom",
            countryCode = "XX",
            latitude = profile.latitude,
            longitude = profile.longitude,
            timezone = "UTC",
            provider = profile.provider,
            altitude = profile.altitude,
            accuracy = profile.accuracy,
            speed = profile.speed,
            bearing = profile.bearing,
            timestamp = profile.timestamp,
            elapsedRealtimeNanos = profile.elapsedRealtimeNanos,
            syntheticIp = "203.0.113.42",
            state = "ACTIVE"
        )
        activateWorldwideProfile(worldwide)
    }

    fun setPreset(presetId: String): LocationProfile {
        val preset = when {
            presetId.contains("nyc", ignoreCase = true) || presetId.contains("new_york", ignoreCase = true) ->
                WorldwideLocationProfile.PRESET_NEW_YORK
            presetId.contains("london", ignoreCase = true) ->
                WorldwideLocationProfile.PRESET_LONDON
            presetId.contains("manila", ignoreCase = true) ->
                WorldwideLocationProfile.PRESET_MANILA
            else -> WorldwideLocationProfile.PRESET_TOKYO
        }
        return activateWorldwideProfile(preset).toLocationProfile()
    }

    fun getAllPresets(): List<LocationProfile> = listOf(
        WorldwideLocationProfile.PRESET_TOKYO.toLocationProfile(),
        WorldwideLocationProfile.PRESET_NEW_YORK.toLocationProfile(),
        WorldwideLocationProfile.PRESET_LONDON.toLocationProfile(),
        WorldwideLocationProfile.PRESET_MANILA.toLocationProfile()
    )
}
