package com.example.deviceidlab.model

import java.io.Serializable
import kotlin.math.abs

/**
 * Dedicated Worldwide Location Profile data model.
 *
 * Encapsulates a coherent geographic and synthetic network profile:
 * - City, Country, Country Code, Coordinates, Timezone
 * - Sensor coordinates (Altitude, Accuracy, Speed, Bearing)
 * - Synthetic Test IP (RFC 5737 test address range: 203.0.113.0/24)
 * - Lifecycle state: AVAILABLE -> ACTIVE -> CONSUMED
 *
 * Strict coordinate validation:
 * -90.0 <= latitude <= 90.0
 * -180.0 <= longitude <= 180.0
 */
data class WorldwideLocationProfile(
    val profileId: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val provider: String = "gps",
    val altitude: Double = 0.0,
    val accuracy: Float = 5.0f,
    val speed: Float = 0.0f,
    val bearing: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val elapsedRealtimeNanos: Long = System.nanoTime(),
    val syntheticIp: String = "203.0.113.42",
    val state: String = "ACTIVE"
) : Serializable {

    companion object {
        const val DEFAULT_COORDINATE_TOLERANCE = 1e-5 // ~1.11 meters at equator
        const val DEFAULT_ALTITUDE_TOLERANCE = 0.5 // meters
        const val DEFAULT_ACCURACY_TOLERANCE = 0.1f // meters

        val PRESET_TOKYO = WorldwideLocationProfile(
            profileId = "loc_tokyo_01",
            city = "Tokyo",
            country = "Japan",
            countryCode = "JP",
            latitude = 35.6762,
            longitude = 139.6503,
            timezone = "Asia/Tokyo",
            provider = "gps",
            altitude = 40.0,
            accuracy = 3.5f,
            speed = 0.0f,
            bearing = 90.0f,
            syntheticIp = "203.0.113.42",
            state = "ACTIVE"
        )

        val PRESET_NEW_YORK = WorldwideLocationProfile(
            profileId = "loc_nyc_02",
            city = "New York",
            country = "United States",
            countryCode = "US",
            latitude = 40.7128,
            longitude = -74.0060,
            timezone = "America/New_York",
            provider = "gps",
            altitude = 10.0,
            accuracy = 4.0f,
            speed = 1.0f,
            bearing = 180.0f,
            syntheticIp = "203.0.113.87",
            state = "ACTIVE"
        )

        val PRESET_LONDON = WorldwideLocationProfile(
            profileId = "loc_london_03",
            city = "London",
            country = "United Kingdom",
            countryCode = "GB",
            latitude = 51.5074,
            longitude = -0.1278,
            timezone = "Europe/London",
            provider = "network",
            altitude = 11.0,
            accuracy = 4.5f,
            speed = 0.0f,
            bearing = 0.0f,
            syntheticIp = "203.0.113.121",
            state = "ACTIVE"
        )

        val PRESET_MANILA = WorldwideLocationProfile(
            profileId = "loc_manila_04",
            city = "Manila",
            country = "Philippines",
            countryCode = "PH",
            latitude = 14.5995,
            longitude = 120.9842,
            timezone = "Asia/Manila",
            provider = "gps",
            altitude = 15.0,
            accuracy = 3.0f,
            speed = 0.0f,
            bearing = 45.0f,
            syntheticIp = "203.0.113.155",
            state = "ACTIVE"
        )
    }

    init {
        require(latitude in -90.0..90.0) {
            "Latitude must be between -90.0 and 90.0, was $latitude"
        }
        require(longitude in -180.0..180.0) {
            "Longitude must be between -180.0 and 180.0, was $longitude"
        }
    }

    fun matchesCoordinates(
        otherLat: Double,
        otherLng: Double,
        tolerance: Double = DEFAULT_COORDINATE_TOLERANCE
    ): Boolean {
        return abs(latitude - otherLat) <= tolerance && abs(longitude - otherLng) <= tolerance
    }

    fun matchesAltitude(
        otherAlt: Double,
        tolerance: Double = DEFAULT_ALTITUDE_TOLERANCE
    ): Boolean {
        return abs(altitude - otherAlt) <= tolerance
    }

    fun matchesAccuracy(
        otherAcc: Float,
        tolerance: Float = DEFAULT_ACCURACY_TOLERANCE
    ): Boolean {
        return abs(accuracy - otherAcc) <= tolerance
    }

    /**
     * Converts to basic LocationProfile for backwards compatibility with legacy hooks.
     */
    fun toLocationProfile(): LocationProfile {
        return LocationProfile(
            profileId = profileId,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracy = accuracy,
            speed = speed,
            bearing = bearing,
            timestamp = timestamp,
            elapsedRealtimeNanos = elapsedRealtimeNanos,
            provider = provider
        )
    }
}
