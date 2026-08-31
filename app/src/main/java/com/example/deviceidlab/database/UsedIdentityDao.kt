package com.example.deviceidlab.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsedIdentityDao {

    @Query("SELECT EXISTS(SELECT 1 FROM used_identities WHERE identityNumber = :identityNumber LIMIT 1)")
    suspend fun isUsed(identityNumber: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM used_identities WHERE identityNumber = :identityNumber LIMIT 1)")
    fun isUsedSync(identityNumber: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: UsedIdentityEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertSync(entity: UsedIdentityEntity): Long

    @Query("SELECT COUNT(*) FROM used_identities")
    suspend fun countUsed(): Int

    @Query("SELECT COUNT(*) FROM used_identities")
    fun countUsedSync(): Int

    @Query("SELECT COUNT(*) FROM used_identities")
    fun countUsedFlow(): Flow<Int>

    @Query("SELECT * FROM used_identities WHERE identityNumber = :identityNumber LIMIT 1")
    suspend fun getIdentity(identityNumber: Long): UsedIdentityEntity?

    @Query("SELECT * FROM used_identities ORDER BY id DESC LIMIT 1")
    suspend fun getLatestIdentity(): UsedIdentityEntity?

    @Query("SELECT * FROM used_identities ORDER BY id DESC LIMIT 1")
    fun getLatestIdentityFlow(): Flow<UsedIdentityEntity?>

    @Query("SELECT identityNumber FROM used_identities")
    suspend fun getAllUsedIdentityNumbers(): List<Long>

    @Query("SELECT identityNumber FROM used_identities")
    fun getAllUsedIdentityNumbersSync(): List<Long>

    @Query("DELETE FROM used_identities")
    suspend fun clearAll()

    @Query("DELETE FROM used_identities")
    fun clearAllSync()
}
