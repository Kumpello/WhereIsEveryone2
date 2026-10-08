package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface UserLocationDao {
    @Query("SELECT * FROM user_location WHERE id = 0")
    suspend fun getUserLocation(): UserLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserLocation(userLocation: UserLocationEntity)
}