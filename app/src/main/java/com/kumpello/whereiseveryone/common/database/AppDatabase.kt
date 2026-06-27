package com.kumpello.whereiseveryone.common.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.kumpello.whereiseveryone.main.common.database.FriendDao
import com.kumpello.whereiseveryone.main.common.database.FriendDatabaseEntity
import com.kumpello.whereiseveryone.main.common.database.UserLocationDao
import com.kumpello.whereiseveryone.main.common.database.UserLocationEntity

@Database(
    entities = [FriendDatabaseEntity::class, UserLocationEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun userLocationDao(): UserLocationDao
}