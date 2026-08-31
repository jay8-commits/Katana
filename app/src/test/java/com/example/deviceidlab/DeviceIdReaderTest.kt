package com.example.deviceidlab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.deviceidlab.demo.InterceptionBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceIdReaderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        InterceptionBridge.clearLogs()
        InterceptionBridge.setInterceptionActive(true)
    }

    @After
    fun tearDown() {
        InterceptionBridge.clearLogs()
    }

    @Test
    fun testIsNpatchHookActive_returnsFalseWhenUnhooked() {
        // Baseline unhooked JVM runtime must return false
        assertFalse(
            "Canary method must return false when running unhooked in standard runtime",
            DeviceIdReader.isNpatchHookActive()
        )
    }

    @Test
    fun testPerformInjectionTest_structureAndDynamicComparison() {
        val testInjectedId = "deadbeef12345678"
        val result = DeviceIdReader.performInjectionTest(context, testInjectedId)

        assertNotNull(result)
        assertEquals(testInjectedId, result.injectedId)
        assertNotNull(result.originalId)
        assertNotNull(result.currentId)

        // Dynamically verifies isSuccess equals (currentId == injectedId)
        val expectedSuccess = (result.currentId == result.injectedId)
        assertEquals(expectedSuccess, result.isSuccess)
    }

    @Test
    fun testPerformInjectionTest_failedWhenIdUnchanged() {
        InterceptionBridge.setInterceptionActive(false)
        val testInjectedId = "deadbeef12345678"
        val result = DeviceIdReader.performInjectionTest(context, testInjectedId)

        assertNotNull(result)
        // When running unhooked, the value from Settings.Secure remains unchanged
        if (result.originalId == result.currentId) {
            assertFalse("Result must evaluate to failed when baseline and current ID are identical", result.isSuccess)
        }
    }

    @Test
    fun testPerformInjectionTest_invalidInputClassification() {
        val result = DeviceIdReader.performInjectionTest(context, "", "com.example.deviceidlab")
        assertEquals("INVALID_INPUT", result.hookStatus)
        assertFalse(result.isSuccess)
        assertNotNull(result.failureReason)
    }

    @Test
    fun testPerformInjectionTest_notInstalledClassification() {
        val nonExistentPackage = "com.nonexistent.dummy.app.xyz123"
        val result = DeviceIdReader.performInjectionTest(context, "a1b2c3d4e5f67890", nonExistentPackage)
        assertEquals("NOT_INSTALLED", result.hookStatus)
        assertFalse(result.isSuccess)
        assertEquals(nonExistentPackage, result.targetPackage)
        assertNotNull(result.failureReason)
    }

    @Test
    fun testPerformInjectionTest_simulationStateWhenBridgeActive() {
        val testId = "1122334455667788"
        DeviceIdReader.saveInjectedAndroidId(context, testId)
        InterceptionBridge.setInterceptionActive(true)
        InterceptionBridge.setSimulatedAndroidId(testId)

        val result = DeviceIdReader.performInjectionTest(context, testId, "com.example.deviceidlab")
        assertEquals(testId, result.injectedId)
        assertEquals(testId, result.currentId)
        assertTrue(result.isSuccess)
        assertEquals("SIMULATION_ONLY", result.hookStatus)
    }

    @Test
    fun testPerformInjectionTest_targetNotPatchedWhenUnhooked() {
        InterceptionBridge.setInterceptionActive(false)
        val testId = "1122334455667788"
        val result = DeviceIdReader.performInjectionTest(context, testId, "com.example.deviceidlab")
        assertFalse(result.isSuccess)
        assertEquals("TARGET_NOT_PATCHED", result.hookStatus)
        assertNotNull(result.failureReason)
    }

    @Test
    fun testConfigurationUpdateAndReload_ScenarioAThroughE() {
        // Scenario A: Baseline unhooked
        InterceptionBridge.setInterceptionActive(false)
        val initialId = DeviceIdReader.readAndroidId(context).value
        assertNotNull(initialId)

        // Scenario B & C: Inject ID A (Interception active)
        val idA = "aaaa1111bbbb2222"
        InterceptionBridge.setInterceptionActive(true)
        DeviceIdReader.saveInjectedAndroidId(context, idA)
        assertEquals(idA, DeviceIdReader.getSavedInjectedAndroidId(context))
        var readResultA = DeviceIdReader.readAndroidId(context).value
        assertEquals(idA, readResultA)

        // Scenario D & E: Update to ID B
        val idB = "cccc3333dddd4444"
        DeviceIdReader.saveInjectedAndroidId(context, idB)
        assertEquals(idB, DeviceIdReader.getSavedInjectedAndroidId(context))
        var readResultB = DeviceIdReader.readAndroidId(context).value
        assertEquals(idB, readResultB)
    }

    @Test
    fun testVerifyNpatchInjection_unhookedBaseline() {
        val details = DeviceIdReader.verifyNpatchInjection(context, "com.example.deviceidlab")
        assertNotNull(details)
        assertEquals("com.example.deviceidlab", details.targetPackage)
        assertFalse("Unhooked runtime must not be verified as active", details.isVerified)
        assertEquals("INJECTION NOT DETECTED", details.finalResult)
    }

    @Test
    fun testVerifyNpatchInjection_withAuditRecord() {
        // Record a mock audit event simulating target process injection
        com.example.deviceidlab.hook.NPatchAuditManager.recordHookEvent(
            context = context,
            targetPackage = "com.example.deviceidlab",
            targetProcess = "com.example.deviceidlab",
            hookEntryStatus = "EXECUTED",
            hookInstallationStatus = "INSTALLED (3 hooks active)"
        )

        val details = DeviceIdReader.verifyNpatchInjection(context, "com.example.deviceidlab")
        assertNotNull(details)
        assertEquals("com.example.deviceidlab", details.targetPackage)
        assertEquals("com.example.deviceidlab", details.targetProcess)
        assertTrue("Details must reflect execution when audit recorded", details.hookEntryStatus.startsWith("EXECUTED"))
        assertTrue("Details must reflect installation when audit recorded", details.hookInstallationStatus.startsWith("INSTALLED"))
        assertTrue("Verification must succeed when audit record is present for target", details.isVerified)
        assertEquals("INJECTION VERIFIED", details.finalResult)
    }
}
