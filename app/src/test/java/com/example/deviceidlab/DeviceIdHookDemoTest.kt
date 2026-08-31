package com.example.deviceidlab

import com.example.deviceidlab.demo.DeviceIdHookDemo
import com.example.deviceidlab.demo.InterceptionBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying that the interception demonstration strictly operates only on
 * com.example.deviceidlab and correctly substitutes active test values.
 */
class DeviceIdHookDemoTest {

    @Before
    fun setUp() {
        InterceptionBridge.clearLogs()
        InterceptionBridge.setInterceptionActive(true)
        InterceptionBridge.updateActiveSimulatedIds(
            androidTestId = "91d04b7e8fa3c2d1",
            telephonyTestId = "384729105837421"
        )
    }

    @After
    fun tearDown() {
        InterceptionBridge.clearLogs()
    }

    @Test
    fun testSettingsSecureAndroidIdSubstitutedForTargetPackage() {
        val result = DeviceIdHookDemo.interceptSettingsSecureGetString(
            callerPackage = "com.example.deviceidlab",
            settingName = "android_id",
            originalProvider = { "original_hardware_android_id" }
        )

        assertEquals("Should return substituted test Android ID", "91d04b7e8fa3c2d1", result)
        val logs = InterceptionBridge.invocationLogs.value
        assertEquals(1, logs.size)
        assertTrue(logs[0].wasIntercepted)
        assertEquals("com.example.deviceidlab", logs[0].callerPackage)
    }

    @Test
    fun testSettingsSecurePassesThroughForUnrelatedPackage() {
        val result = DeviceIdHookDemo.interceptSettingsSecureGetString(
            callerPackage = "com.unrelated.thirdparty.app",
            settingName = "android_id",
            originalProvider = { "original_hardware_android_id" }
        )

        assertEquals("Unrelated packages must NEVER be intercepted", "original_hardware_android_id", result)
        val logs = InterceptionBridge.invocationLogs.value
        assertEquals(1, logs.size)
        assertFalse(logs[0].wasIntercepted)
    }

    @Test
    fun testSettingsSecurePassesThroughForNonAndroidIdKey() {
        val result = DeviceIdHookDemo.interceptSettingsSecureGetString(
            callerPackage = "com.example.deviceidlab",
            settingName = "bluetooth_name",
            originalProvider = { "Pixel_8_Pro" }
        )

        assertEquals("Non-ANDROID_ID keys must pass through", "Pixel_8_Pro", result)
        val logs = InterceptionBridge.invocationLogs.value
        assertEquals(1, logs.size)
        assertFalse(logs[0].wasIntercepted)
    }

    @Test
    fun testTelephonyHookSubstitutedForTargetPackage() {
        val result = DeviceIdHookDemo.interceptTelephonyGetDeviceId(
            callerPackage = "com.example.deviceidlab",
            originalProvider = { "Restricted" }
        )

        assertEquals("Should return substituted test Telephony ID", "384729105837421", result)
        val logs = InterceptionBridge.invocationLogs.value
        assertEquals(1, logs.size)
        assertTrue(logs[0].wasIntercepted)
    }

    @Test
    fun testTelephonyHookPassesThroughForUnrelatedPackage() {
        val result = DeviceIdHookDemo.interceptTelephonyGetDeviceId(
            callerPackage = "com.other.app",
            originalProvider = { "Real_IMEI_Or_Restricted" }
        )

        assertEquals("Real_IMEI_Or_Restricted", result)
        val logs = InterceptionBridge.invocationLogs.value
        assertEquals(1, logs.size)
        assertFalse(logs[0].wasIntercepted)
    }

    @Test
    fun testInterceptionDisabled() {
        InterceptionBridge.setInterceptionActive(false)

        val result = DeviceIdHookDemo.interceptSettingsSecureGetString(
            callerPackage = "com.example.deviceidlab",
            settingName = "android_id",
            originalProvider = { "original_android_id" }
        )

        assertEquals("original_android_id", result)
        val logs = InterceptionBridge.invocationLogs.value
        assertFalse(logs[0].wasIntercepted)
    }
}
