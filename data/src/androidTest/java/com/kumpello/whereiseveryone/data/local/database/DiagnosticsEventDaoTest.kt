package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticsEventDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: DiagnosticsEventDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.diagnosticsEventDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun queueRetainsSeparateAccountsAndExcludesUnownedEvents() = runTest {
        dao.insert(DiagnosticsEventEntity("anonymous", 1, "{}", null))
        dao.insert(DiagnosticsEventEntity("first", 2, "{}", "account-a"))
        dao.insert(DiagnosticsEventEntity("second", 3, "{}", "account-b"))
        assertEquals(listOf("first"), dao.oldest("account-a", 10).map { it.id })
        assertEquals(listOf("second"), dao.oldest("account-b", 10).map { it.id })

        dao.delete(listOf("first"))
        assertEquals(emptyList<DiagnosticsEventEntity>(), dao.oldest("account-a", 10))
        assertEquals(listOf("second"), dao.oldest("account-b", 10).map { it.id })
    }

    @Test
    fun clearingSessionCachesPreservesAllAccountQueues() = runTest {
        database.friendDao().insertFriends(listOf(
            FriendDatabaseEntity("friend", "available", "accepted", null, null),
        ))
        database.userLocationDao().updateUserLocation(
            UserLocationEntity(latitude = 52.0, longitude = 21.0, bearing = null,
                altitude = null, accuracy = null, speed = null, lastUpdate = 1),
        )
        dao.insert(DiagnosticsEventEntity("first", 1, "{}", "account-a"))
        dao.insert(DiagnosticsEventEntity("second", 2, "{}", "account-b"))

        database.clearSessionData()

        assertEquals(emptyList<FriendDatabaseEntity>(), database.friendDao().getFriends())
        assertNull(database.userLocationDao().getUserLocation())
        assertEquals(listOf("first"), dao.oldest("account-a", 10).map { it.id })
        assertEquals(listOf("second"), dao.oldest("account-b", 10).map { it.id })
    }

    @Test
    fun queueIsBoundedOrderedAndDeduplicated() = runTest {
        dao.insert(DiagnosticsEventEntity("first", 1, "original", "account"))
        dao.insert(DiagnosticsEventEntity("first", 9, "duplicate", "account"))
        dao.insert(DiagnosticsEventEntity("second", 2, "{}", "account"))
        dao.insert(DiagnosticsEventEntity("third", 3, "{}", "account"))
        assertEquals("original", dao.oldest("account", 1).single().payload)

        dao.prune(2)
        assertEquals(listOf("second", "third"), dao.oldest("account", 10).map { it.id })
        dao.purgeBefore(3)
        assertEquals(listOf("third"), dao.oldest("account", 10).map { it.id })
        dao.delete(listOf("third"))
        assertEquals(emptyList<DiagnosticsEventEntity>(), dao.oldest("account", 10))
    }
}
