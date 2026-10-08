package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface FriendDao {
    @Query("SELECT * FROM friends")
    suspend fun getFriends(): List<FriendDatabaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendDatabaseEntity>)

    @Query("DELETE FROM friends")
    suspend fun clearAll()
}