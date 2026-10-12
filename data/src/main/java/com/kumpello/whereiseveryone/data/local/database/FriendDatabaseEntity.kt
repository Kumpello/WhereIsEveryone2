package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kumpello.whereiseveryone.data.network.model.FriendData
import com.kumpello.whereiseveryone.data.network.model.UserInfo

@Entity(tableName = "friends")
internal data class FriendDatabaseEntity(
    @PrimaryKey val username: String,
    val status: String,
    val state: String,
    @Embedded val location: UserInfoEntity?,
    val friendSince: Long?
)

internal data class UserInfoEntity(
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
    val lastUpdate: Long,
    val isCarMode: Boolean = false,
)

internal fun FriendData.toDatabaseEntity() = FriendDatabaseEntity(
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
            speed = it.speed,
            lastUpdate = it.last_update,
            isCarMode = it.isCarMode,
        )
    },
    friendSince = friend_since
)

internal fun FriendDatabaseEntity.toDomain() = FriendData(
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
            speed = it.speed,
            last_update = it.lastUpdate,
            isCarMode = it.isCarMode,
        )
    },
    friend_since = friendSince
)
