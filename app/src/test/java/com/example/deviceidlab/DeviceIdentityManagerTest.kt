package com.example.deviceidlab

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.deviceidlab.database.AppDatabase
import com.example.deviceidlab.database.UsedIdentityDao
import com.example.deviceidlab.database.UsedIdentityEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceIdentityManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UsedIdentityDao
    private lateinit var manager: DeviceIdentityManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.usedIdentityDao()
        manager = DeviceIdentityManager(
            identityDao = dao,
            maxPoolSize = 1_000_000L,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInitialStateIsEmpty() = runBlocking {
        manager.initialize()
        assertNull(manager.currentIdentity.value)
        assertEquals(0, manager.usedCount.value)
    }

    @Test
    fun testGenerateSingleIdentity() = runBlocking {
        val result = manager.generateNextIdentity()
        assertTrue("Allocation should succeed", result is GenerationResult.Success)
        val identity = (result as GenerationResult.Success).identity

        assertTrue("Identity number within 1..1M", identity.identityNumber in 1L..1_000_000L)
        assertEquals(1, dao.countUsed())
        assertEquals(identity, manager.currentIdentity.value)
        assertEquals(1, manager.usedCount.value)
        assertTrue(dao.isUsed(identity.identityNumber))
    }

    @Test
    fun test100AllocationsWithoutDuplicates() = runBlocking {
        val allocated = mutableSetOf<Long>()
        for (i in 1..100) {
            val result = manager.generateNextIdentity()
            assertTrue(result is GenerationResult.Success)
            val idNum = (result as GenerationResult.Success).identity.identityNumber
            assertTrue("Must not contain duplicates: $idNum", allocated.add(idNum))
        }
        assertEquals(100, allocated.size)
        assertEquals(100, dao.countUsed())
        assertEquals(100, manager.usedCount.value)
    }

    @Test
    fun test1000AllocationsWithoutDuplicates() = runBlocking {
        val allocated = mutableSetOf<Long>()
        for (i in 1..1000) {
            val result = manager.generateNextIdentity()
            assertTrue(result is GenerationResult.Success)
            val idNum = (result as GenerationResult.Success).identity.identityNumber
            assertTrue(allocated.add(idNum))
        }
        assertEquals(1000, allocated.size)
        assertEquals(1000, dao.countUsed())
    }

    @Test
    fun test10000AllocationsWithoutDuplicates() = runBlocking {
        val allocated = mutableSetOf<Long>()
        for (i in 1..10000) {
            val result = manager.generateNextIdentity()
            assertTrue(result is GenerationResult.Success)
            val idNum = (result as GenerationResult.Success).identity.identityNumber
            assertTrue(allocated.add(idNum))
        }
        assertEquals(10000, allocated.size)
        assertEquals(10000, dao.countUsed())
    }

    @Test
    fun testPersistenceAndAppRestart() = runBlocking {
        // Step 1: Allocate first identity
        val res1 = manager.generateNextIdentity() as GenerationResult.Success
        val firstId = res1.identity

        // Step 2: Allocate second identity
        val res2 = manager.generateNextIdentity() as GenerationResult.Success
        val secondId = res2.identity

        assertEquals(2, dao.countUsed())

        // Step 3: Simulate app restart by instantiating a new manager pointing to same DB
        val restartedManager = DeviceIdentityManager(
            identityDao = dao,
            maxPoolSize = 1_000_000L,
            ioDispatcher = Dispatchers.Unconfined
        )
        restartedManager.initialize()

        // Verify latest identity and count were recovered
        assertEquals(2, restartedManager.usedCount.value)
        assertNotNull(restartedManager.currentIdentity.value)
        assertEquals(secondId.identityNumber, restartedManager.currentIdentity.value?.identityNumber)
        assertEquals(secondId.androidTestId, restartedManager.currentIdentity.value?.androidTestId)
        assertEquals(secondId.telephonyTestId, restartedManager.currentIdentity.value?.telephonyTestId)

        // Generating next ID must continue without colliding with either
        val res3 = restartedManager.generateNextIdentity() as GenerationResult.Success
        assertTrue(res3.identity.identityNumber != firstId.identityNumber)
        assertTrue(res3.identity.identityNumber != secondId.identityNumber)
        assertEquals(3, restartedManager.usedCount.value)
    }

    @Test
    fun testDatabaseReset() = runBlocking {
        // Generate a few IDs
        manager.generateNextIdentity()
        manager.generateNextIdentity()
        assertEquals(2, manager.usedCount.value)

        // Reset
        manager.resetDatabase()
        assertEquals(0, manager.usedCount.value)
        assertNull(manager.currentIdentity.value)
        assertEquals(0, dao.countUsed())

        // New allocation starts cleanly
        val res = manager.generateNextIdentity() as GenerationResult.Success
        assertEquals(1, manager.usedCount.value)
        assertNotNull(res.identity)
    }

    @Test
    fun testPoolExhaustion() = runBlocking {
        // Test with a micro-pool of size 5
        val smallPoolManager = DeviceIdentityManager(
            identityDao = dao,
            maxPoolSize = 5L,
            ioDispatcher = Dispatchers.Unconfined
        )
        smallPoolManager.initialize()

        val used = mutableSetOf<Long>()
        for (i in 1..5) {
            val res = smallPoolManager.generateNextIdentity()
            assertTrue("Allocation $i must succeed", res is GenerationResult.Success)
            used.add((res as GenerationResult.Success).identity.identityNumber)
        }
        assertEquals(5, used.size)
        assertEquals(5, dao.countUsed())

        // 6th allocation MUST trigger PoolExhausted
        val overflowRes = smallPoolManager.generateNextIdentity()
        assertTrue("Must be PoolExhausted", overflowRes is GenerationResult.PoolExhausted)
        assertEquals(5, dao.countUsed())
    }

    @Test
    fun testDuplicatePreventionWithPreExistingEntities() = runBlocking {
        // Pre-insert IDs 1, 2, 3 manually
        for (i in 1L..3L) {
            val id = RandomIdGenerator.createIdentity(i)
            dao.insert(UsedIdentityEntity(0, id.identityNumber, id.androidTestId, id.telephonyTestId, id.createdAt))
        }
        assertEquals(3, dao.countUsed())

        val smallPool = DeviceIdentityManager(
            identityDao = dao,
            maxPoolSize = 5L,
            ioDispatcher = Dispatchers.Unconfined
        )
        smallPool.initialize()
        assertEquals(3, smallPool.usedCount.value)

        // Next allocations must strictly pick from remaining (4 and 5)
        val res4 = smallPool.generateNextIdentity() as GenerationResult.Success
        assertTrue("Must pick 4 or 5", res4.identity.identityNumber in setOf(4L, 5L))

        val res5 = smallPool.generateNextIdentity() as GenerationResult.Success
        assertTrue("Must pick remaining 4 or 5", res5.identity.identityNumber in setOf(4L, 5L))
        assertTrue("Must not duplicate", res4.identity.identityNumber != res5.identity.identityNumber)

        val exhausted = smallPool.generateNextIdentity()
        assertTrue(exhausted is GenerationResult.PoolExhausted)
    }
}
