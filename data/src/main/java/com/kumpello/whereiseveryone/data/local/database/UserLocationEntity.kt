package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_location")
internal data class UserLocationEntity(
    @PrimaryKey val id: Int = 0, // Single row for user location
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
    val lastUpdate: Long
)