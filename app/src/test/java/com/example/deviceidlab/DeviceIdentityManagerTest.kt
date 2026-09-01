package com.example.deviceidlab

import com.example.deviceidlab.generator.RandomIdGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdentityManagerTest {

    @Test
    fun testGenerateProfileIntegrity() {
        val profile = RandomIdGenerator.generateProfile("Test Profile")
        assertNotNull(profile.id)
        assertNotNull(profile.androidId)
        assertEquals(16, profile.androidId.length)
        assertNotNull(profile.imei)
        assertEquals(15, profile.imei.length)
        assertTrue(profile.serialNumber.isNotEmpty())
        assertTrue(profile.macAddress.contains(":"))
    }

    @Test
    fun testRandomHexUniqueness() {
        val hex1 = RandomIdGenerator.generateRandomHex(16)
        val hex2 = RandomIdGenerator.generateRandomHex(16)
        assertTrue(hex1 != hex2)
    }
}
