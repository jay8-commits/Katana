package com.example.deviceidlab

import com.example.deviceidlab.database.UsedIdentityDao
import com.example.deviceidlab.database.UsedIdentityEntity
import com.example.deviceidlab.model.DeviceIdentity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.SecureRandom

/**
 * Result sealed hierarchy for identity allocation operations.
 */
sealed interface GenerationResult {
    data class Success(val identity: DeviceIdentity) : GenerationResult
    data class PoolExhausted(val message: String = "ID POOL EXHAUSTED: All 1,000,000 test identities have been used.") : GenerationResult
    data class Error(val message: String) : GenerationResult
}

/**
 * Manages the allocation, persistence, and state tracking of the 1,000,000 test identity pool.
 *
 * Guarantees:
 * 1. Zero duplication: Database unique index constraint and transactional verification.
 * 2. Secure randomness: Uses [SecureRandom] for index selection.
 * 3. Near-exhaustion handling: Fast deterministic fallback search if random selection encounters repeated collisions.
 * 4. Pool ceiling: Hard stop at 1,000,000 identities with explicit [GenerationResult.PoolExhausted].
 * 5. Persistence across app restarts: Restores latest active identity on startup.
 */
class DeviceIdentityManager(
    private val identityDao: UsedIdentityDao,
    private val maxPoolSize: Long = MAX_POOL_CAPACITY,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        const val MAX_POOL_CAPACITY: Long = 1_000_000L
        private const val MAX_RANDOM_COLLISION_RETRIES = 25
        private const val DETERMINISTIC_FALLBACK_THRESHOLD = 999_990L
    }

    private val secureRandom = SecureRandom()

    private val _currentIdentity = MutableStateFlow<DeviceIdentity?>(null)
    val currentIdentity: StateFlow<DeviceIdentity?> = _currentIdentity.asStateFlow()

    private val _usedCount = MutableStateFlow(0)
    val usedCount: StateFlow<Int> = _usedCount.asStateFlow()

    val totalPoolCapacity: Long = maxPoolSize

    /**
     * Initializes the manager by loading the current count and latest active identity from SQLite.
     */
    suspend fun initialize() = withContext(ioDispatcher) {
        val count = identityDao.countUsed()
        _usedCount.value = count
        val latest = identityDao.getLatestIdentity()
        if (latest != null) {
            _currentIdentity.value = DeviceIdentity(
                identityNumber = latest.identityNumber,
                androidTestId = latest.androidTestId,
                telephonyTestId = latest.telephonyTestId,
                createdAt = latest.createdAt
            )
        }
    }

    /**
     * Allocates a new, unused test identity from the pool.
     */
    suspend fun generateNextIdentity(): GenerationResult = withContext(ioDispatcher) {
        val currentCount = identityDao.countUsed()
        if (currentCount >= maxPoolSize) {
            return@withContext GenerationResult.PoolExhausted()
        }

        var candidateIndex: Long? = null

        // 1. If we have plenty of free slots, use SecureRandom
        if (currentCount < DETERMINISTIC_FALLBACK_THRESHOLD) {
            for (attempt in 0 until MAX_RANDOM_COLLISION_RETRIES) {
                // SecureRandom generates long between 1 and maxPoolSize (inclusive)
                val randomNum = 1L + (secureRandom.nextDouble() * maxPoolSize).toLong().coerceIn(0L, maxPoolSize - 1)
                if (!identityDao.isUsed(randomNum)) {
                    candidateIndex = randomNum
                    break
                }
            }
        }

        // 2. If collisions occurred or we are near pool exhaustion, do deterministic fallback search
        if (candidateIndex == null) {
            candidateIndex = findFirstUnusedIndex()
        }

        if (candidateIndex == null) {
            return@withContext GenerationResult.PoolExhausted()
        }

        // 3. Construct deterministic identity
        val identity = RandomIdGenerator.createIdentity(candidateIndex)
        val entity = UsedIdentityEntity(
            identityNumber = identity.identityNumber,
            androidTestId = identity.androidTestId,
            telephonyTestId = identity.telephonyTestId,
            createdAt = identity.createdAt
        )

        try {
            identityDao.insert(entity)
            _currentIdentity.value = identity
            _usedCount.value = identityDao.countUsed()
            GenerationResult.Success(identity)
        } catch (e: Exception) {
            // Collision or race condition on unique index -> retry once or return error
            val retryCandidate = findFirstUnusedIndex()
            if (retryCandidate != null) {
                val retryIdentity = RandomIdGenerator.createIdentity(retryCandidate)
                val retryEntity = UsedIdentityEntity(
                    identityNumber = retryIdentity.identityNumber,
                    androidTestId = retryIdentity.androidTestId,
                    telephonyTestId = retryIdentity.telephonyTestId,
                    createdAt = retryIdentity.createdAt
                )
                try {
                    identityDao.insert(retryEntity)
                    _currentIdentity.value = retryIdentity
                    _usedCount.value = identityDao.countUsed()
                    GenerationResult.Success(retryIdentity)
                } catch (retryEx: Exception) {
                    GenerationResult.Error("Failed to reserve identity: ${retryEx.message}")
                }
            } else {
                GenerationResult.PoolExhausted()
            }
        }
    }

    /**
     * Deterministically finds the first unused index in the 1..maxPoolSize range.
     */
    private suspend fun findFirstUnusedIndex(): Long? {
        val usedSet = identityDao.getAllUsedIdentityNumbers().toHashSet()
        for (i in 1L..maxPoolSize) {
            if (!usedSet.contains(i)) {
                return i
            }
        }
        return null
    }

    /**
     * Resets the test database, clearing all recorded identities and resetting the pool.
     */
    suspend fun resetDatabase() = withContext(ioDispatcher) {
        identityDao.clearAll()
        _currentIdentity.value = null
        _usedCount.value = 0
    }
}
