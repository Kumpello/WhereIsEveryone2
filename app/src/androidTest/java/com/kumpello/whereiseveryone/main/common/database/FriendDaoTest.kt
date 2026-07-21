package com.kumpello.whereiseveryone.main.common.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kumpello.whereiseveryone.common.database.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FriendDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.friendDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetFriends() = runTest {
        val friends = listOf(
            FriendDatabaseEntity(
                username = "user1",
                status = "status1",
                state = "accepted",
                location = UserInfoEntity(1.0, 2.0, 0f, 3.0, 4f, 1672531200000L),
                friendSince = 1672531200000L
            )
        )
        dao.insertFriends(friends)

        val result = dao.getFriends()
        assertEquals(1, result.size)
        assertEquals("user1", result[0].username)
    }

    @Test
    fun clearAll() = runTest {
        val friends = listOf(
            FriendDatabaseEntity(
                username = "user1",
                status = "status1",
                state = "accepted",
                location = UserInfoEntity(1.0, 2.0, 0f, 3.0, 4f, 1672531200000L),
                friendSince = 1672531200000L
            )
        )
        dao.insertFriends(friends)
        dao.clearAll()

        val result = dao.getFriends()
        assertEquals(0, result.size)
    }
}
