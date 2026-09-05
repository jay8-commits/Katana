package com.example.deviceidlab.model

import java.io.Serializable
import kotlin.math.abs

/**
 * Dedicated Location Profile data model.
 *
 * Encapsulates synthetic geographic and sensor coordinates delivered via IPC
 * to target applications during test execution.
 *
 * Strict validation:
 * -90.0 <= latitude <= 90.0
 * -180.0 <= longitude <= 180.0
 */
data class LocationProfile(
    val profileId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 5.0f,
    val speed: Float = 0.0f,
    val bearing: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val elapsedRealtimeNanos: Long = System.nanoTime(),
    val provider: String = "gps"
) : Serializable {

    companion object {
        const val DEFAULT_COORDINATE_TOLERANCE = 1e-5 // ~1.11 meters at equator
        const val DEFAULT_ALTITUDE_TOLERANCE = 0.5 // meters
        const val DEFAULT_ACCURACY_TOLERANCE = 0.1f // meters

        val PRESET_TOKYO = LocationProfile(
            profileId = "loc_preset_tokyo",
            latitude = 35.6762,
            longitude = 139.6503,
            altitude = 40.0,
            accuracy = 3.5f,
            speed = 0.0f,
            bearing = 90.0f,
            provider = "gps"
        )

        val PRESET_SAN_FRANCISCO = LocationProfile(
            profileId = "loc_preset_sf",
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 16.0,
            accuracy = 4.0f,
            speed = 1.2f,
            bearing = 180.0f,
            provider = "gps"
        )

        val PRESET_LONDON = LocationProfile(
            profileId = "loc_preset_london",
            latitude = 51.5074,
            longitude = -0.1278,
            altitude = 11.0,
            accuracy = 8.0f,
            speed = 0.0f,
            bearing = 0.0f,
            provider = "network"
        )

        val PRESET_NULL_ISLAND = LocationProfile(
            profileId = "loc_preset_null_island",
            latitude = 0.0,
            longitude = 0.0,
            altitude = 0.0,
            accuracy = 1.0f,
            speed = 0.0f,
            bearing = 0.0f,
            provider = "fused"
        )

        val ALL_PRESETS = listOf(
            PRESET_TOKYO,
            PRESET_SAN_FRANCISCO,
            PRESET_LONDON,
            PRESET_NULL_ISLAND
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

    /**
     * Coordinate tolerance match helper to avoid unsafe exact floating-point equality.
     */
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
}
