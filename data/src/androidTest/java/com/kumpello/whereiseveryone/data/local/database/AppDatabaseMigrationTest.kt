package com.kumpello.whereiseveryone.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migration4To5PreservesFriendsAndLocationAndCreatesQueue() {
        helper.createDatabase("migration-4-5", 4).apply {
            execSQL(
                "INSERT INTO friends " +
                    "(username, status, state, latitude, longitude, lastUpdate, isCarMode, friendSince) " +
                    "VALUES ('friend', 'available', 'accepted', 52.0, 21.0, 1234, 1, 1200)"
            )
            execSQL(
                "INSERT INTO user_location (id, latitude, longitude, lastUpdate, isCarMode) " +
                    "VALUES (0, 51.0, 20.0, 5678, 0)"
            )
            close()
        }

        helper.runMigrationsAndValidate("migration-4-5", 5, true, MIGRATION_4_5).use { database ->
            database.query(
                "SELECT username, status, state, latitude, longitude, lastUpdate, isCarMode, friendSince " +
                    "FROM friends"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("friend", cursor.getString(0))
                assertEquals("available", cursor.getString(1))
                assertEquals("accepted", cursor.getString(2))
                assertEquals(52.0, cursor.getDouble(3), 0.0)
                assertEquals(21.0, cursor.getDouble(4), 0.0)
                assertEquals(1234L, cursor.getLong(5))
                assertEquals(1, cursor.getInt(6))
                assertEquals(1200L, cursor.getLong(7))
            }
            database.query(
                "SELECT id, latitude, longitude, lastUpdate, isCarMode FROM user_location"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
                assertEquals(51.0, cursor.getDouble(1), 0.0)
                assertEquals(20.0, cursor.getDouble(2), 0.0)
                assertEquals(5678L, cursor.getLong(3))
                assertEquals(0, cursor.getInt(4))
            }
            database.execSQL(
                "INSERT INTO diagnostics_events (id, createdAt, payload, ownerId) " +
                    "VALUES ('event', 9000, '{}', NULL)"
            )
            database.query("SELECT id, ownerId FROM diagnostics_events").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("event", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
        }
    }
}
