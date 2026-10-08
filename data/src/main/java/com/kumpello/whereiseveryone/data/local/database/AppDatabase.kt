package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database( //TODO: Migration!
    entities = [FriendDatabaseEntity::class, UserLocationEntity::class],
    version = 3,
    exportSchema = false
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun userLocationDao(): UserLocationDao
}