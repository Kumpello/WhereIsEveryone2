package com.kumpello.whereiseveryone.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** The version 4 schema is the first exported baseline; existing rows remain untouched. */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS diagnostics_events (" +
                "id TEXT NOT NULL, createdAt INTEGER NOT NULL, payload TEXT NOT NULL, " +
                "ownerId TEXT, PRIMARY KEY(id))",
        )
    }
}
