package com.kumpello.whereiseveryone.main.common.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kumpello.whereiseveryone.main.map.domain.model.FriendData
import com.kumpello.whereiseveryone.main.map.domain.model.UserInfo

@Entity(tableName = "friends")
data class FriendDatabaseEntity(
    @PrimaryKey val username: String,
    val status: String,
    val state: String,
    @Embedded val location: UserInfoEntity
)

data class UserInfoEntity(
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val altitude: Double?,
    val accuracy: Float?,
    val lastUpdate: String
)

fun FriendData.toDatabaseEntity() = FriendDatabaseEntity(
    username = username,
    status = status,
    state = state,
    location = UserInfoEntity(
        latitude = location.latitude,
        longitude = location.longitude,
        bearing = location.bearing,
        altitude = location.altitude,
        accuracy = location.accuracy,
        lastUpdate = location.last_update
    )
)

fun FriendDatabaseEntity.toDomain() = FriendData(
    username = username,
    status = status,
    state = state,
    location = UserInfo(
        latitude = location.latitude,
        longitude = location.longitude,
        bearing = location.bearing,
        altitude = location.altitude,
        accuracy = location.accuracy,
        last_update = location.lastUpdate
    )
)
