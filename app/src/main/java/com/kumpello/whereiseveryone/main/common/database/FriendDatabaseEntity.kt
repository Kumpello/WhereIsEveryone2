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
    @Embedded val location: UserInfoEntity?
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
    location = location?.let {
        UserInfoEntity(
            latitude = it.latitude,
            longitude = it.longitude,
            bearing = it.bearing,
            altitude = it.altitude,
            accuracy = it.accuracy,
            lastUpdate = it.last_update
        )
    }
)

fun FriendDatabaseEntity.toDomain() = FriendData(
    username = username,
    status = status,
    state = state,
    location = location?.let {
        UserInfo(
            latitude = it.latitude,
            longitude = it.longitude,
            bearing = it.bearing,
            altitude = it.altitude,
            accuracy = it.accuracy,
            last_update = it.lastUpdate
        )
    }
)
