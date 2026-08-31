package com.example.deviceidlab.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an identity that has been allocated from the 1..1,000,000 pool.
 * A unique database index on [identityNumber] guarantees no duplicate allocation can occur.
 */
@Entity(
    tableName = "used_identities",
    indices = [
        Index(value = ["identityNumber"], unique = true)
    ]
)
data class UsedIdentityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "identityNumber")
    val identityNumber: Long,

    @ColumnInfo(name = "androidTestId")
    val androidTestId: String,

    @ColumnInfo(name = "telephonyTestId")
    val telephonyTestId: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)
