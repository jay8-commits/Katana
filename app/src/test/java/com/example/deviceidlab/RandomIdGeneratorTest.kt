package com.example.deviceidlab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the deterministic ID generation algorithms.
 */
class RandomIdGeneratorTest {

    @Test
    fun testAndroidTestIdFormat() {
        val testId = RandomIdGenerator.generateAndroidTestId(482731L)
        assertEquals("Android ID must be exactly 16 characters", 16, testId.length)
        assertTrue("Android ID must be lowercase hex characters", testId.matches(Regex("^[0-9a-f]{16}$")))
    }

    @Test
    fun testTelephonyTestIdFormat() {
        val testId = RandomIdGenerator.generateTelephonyTestId(482731L)
        assertEquals("Telephony ID must be exactly 15 digits", 15, testId.length)
        assertTrue("Telephony ID must contain digits only", testId.matches(Regex("^[0-9]{15}$")))
    }

    @Test
    fun testDeterminismAcrossInvocations() {
        val id1 = RandomIdGenerator.generateAndroidTestId(100L)
        val id2 = RandomIdGenerator.generateAndroidTestId(100L)
        assertEquals("Same index must produce identical Android ID", id1, id2)

        val tel1 = RandomIdGenerator.generateTelephonyTestId(100L)
        val tel2 = RandomIdGenerator.generateTelephonyTestId(100L)
        assertEquals("Same index must produce identical Telephony ID", tel1, tel2)
    }

    @Test
    fun testDistinctIndicesProduceDistinctIds() {
        val androidIds = (1L..500L).map { RandomIdGenerator.generateAndroidTestId(it) }.toSet()
        assertEquals("500 distinct indices must produce 500 distinct Android IDs", 500, androidIds.size)

        val telephonyIds = (1L..500L).map { RandomIdGenerator.generateTelephonyTestId(it) }.toSet()
        assertEquals("500 distinct indices must produce 500 distinct Telephony IDs", 500, telephonyIds.size)
    }

    @Test
    fun testCreateIdentityModel() {
        val identity = RandomIdGenerator.createIdentity(482731L, 1700000000000L)
        assertEquals(482731L, identity.identityNumber)
        assertEquals(1700000000000L, identity.createdAt)
        assertEquals(16, identity.androidTestId.length)
        assertEquals(15, identity.telephonyTestId.length)
    }
}
