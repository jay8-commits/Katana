package com.example.deviceidlab

import com.example.deviceidlab.generator.RandomIdGenerator
import com.example.deviceidlab.hook.NPatchConfig
import com.example.deviceidlab.hook.TestApiCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(profile.buildModel.isNotEmpty())
        assertTrue(profile.buildManufacturer.isNotEmpty())
        assertTrue(profile.buildBrand.isNotEmpty())
        assertTrue(profile.buildProduct.isNotEmpty())
        assertTrue(profile.buildDevice.isNotEmpty())
        assertTrue(profile.buildFingerprint.isNotEmpty())
    }

    @Test
    fun testRandomHexUniqueness() {
        val hex1 = RandomIdGenerator.generateRandomHex(16)
        val hex2 = RandomIdGenerator.generateRandomHex(16)
        assertTrue(hex1 != hex2)
    }

    @Test
    fun testMaskValueUtility() {
        assertEquals("<null>", TestApiCatalog.maskValue(null))
        assertEquals("<empty>", TestApiCatalog.maskValue(""))
        assertEquals("****", TestApiCatalog.maskValue("1234"))
        val masked = TestApiCatalog.maskValue("58e8039d8acedb72")
        assertTrue(masked.startsWith("58...72"))
        assertFalse(masked.contains("039d8ace"))
    }

    @Test
    fun testCatalogAllowlistInventory() {
        val apis = TestApiCatalog.SUPPORTED_APIS
        assertEquals("Strictly 16 APIs must be registered in the catalog", 16, apis.size)
        assertTrue(apis.any { it.configKey == NPatchConfig.KEY_ANDROID_ID && it.isDynamic })
        assertTrue(apis.any { it.configKey == NPatchConfig.KEY_BUILD_MODEL && it.requiresProcessRestart })
        assertTrue(apis.any { it.configKey == NPatchConfig.KEY_SERIAL })
        assertTrue(apis.any { it.configKey == NPatchConfig.KEY_IMEI })
        assertTrue(apis.any { it.configKey == NPatchConfig.KEY_MAC })
    }

    @Test
    fun testDynamicVsStaticFlags() {
        val staticApis = TestApiCatalog.SUPPORTED_APIS.filter { it.requiresProcessRestart }
        val dynamicApis = TestApiCatalog.SUPPORTED_APIS.filter { it.isDynamic }

        assertEquals(7, staticApis.size) // MODEL, MANUFACTURER, BRAND, PRODUCT, DEVICE, FINGERPRINT, SERIAL field
        assertEquals(9, dynamicApis.size) // ANDROID_ID (2), getSerial, 5 Telephony, MAC
    }
}
