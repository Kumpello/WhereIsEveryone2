package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(
    entities = [FriendDatabaseEntity::class, UserLocationEntity::class, DiagnosticsEventEntity::class],
    version = 5,
    exportSchema = true
)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun userLocationDao(): UserLocationDao
    abstract fun diagnosticsEventDao(): DiagnosticsEventDao

    suspend fun clearSessionData() = withTransaction {
        friendDao().clearAll()
        userLocationDao().clearAll()
    }
}
