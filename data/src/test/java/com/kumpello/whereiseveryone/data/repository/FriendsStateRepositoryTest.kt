package com.kumpello.whereiseveryone.data.repository

import app.cash.turbine.test
import com.kumpello.whereiseveryone.data.local.database.FriendDao
import com.kumpello.whereiseveryone.data.local.database.FriendDatabaseEntity
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsStateRepositoryTest {

    private val friendsRepository: FriendsRepository = mockk()
    private val friendDao: FriendDao = mockk(relaxed = true)

    @Test
    fun `observeFriends emits cached data then fresh data`() = runTest {
        val friendsManager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        val cachedEntity = FriendDatabaseEntity("user1", "status1", "accepted", null, null)
        coEvery { friendDao.getFriends() } returns listOf(cachedEntity)
        
        val freshData = FriendsResponse.FriendsData(emptyList())
        coEvery { friendsRepository.getFriends() } returns freshData

        friendsManager.observeFriends().test {
            val firstEmission = awaitItem()
            assertTrue(firstEmission is FriendsResponse.FriendsData)
            assertEquals("user1", (firstEmission as FriendsResponse.FriendsData).positions[0].username)

            val secondEmission = awaitItem()
            assertEquals(freshData, secondEmission)
            
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { friendDao.insertFriends(any()) }
        friendsManager.cancel()
    }

    @Test
    fun `polling recovers after failures at fifteen second intervals and continues after success`() = runTest {
        val friendsManager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        val freshData = FriendsResponse.FriendsData(emptyList())
        coEvery { friendDao.getFriends() } returns listOf(
            FriendDatabaseEntity("cached", "status", "accepted", null, null)
        )
        coEvery { friendsRepository.getFriends() } throws IOException("Offline") andThenThrows
            IOException("Still offline") andThen freshData

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }

                advanceTimeBy(14_999)
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                advanceTimeBy(1)
                runCurrent()
                coVerify(exactly = 2) { friendsRepository.getFriends() }
                expectNoEvents()

                advanceTimeBy(15_000)
                runCurrent()
                assertEquals(freshData, awaitItem())
                advanceTimeBy(15_000)
                runCurrent()
                coVerify(exactly = 4) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }

    @Test
    fun `HTTP errors remain observable and polling continues at the normal interval`() = runTest {
        val friendsManager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        val error = FriendsResponse.ErrorData(503, "Unavailable", "Unavailable")
        coEvery { friendsRepository.getFriends() } returns error

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                assertEquals(error, awaitItem())
                advanceTimeBy(15_000)
                runCurrent()
                coVerify(exactly = 2) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }

    @Test
    fun `cancelling manager during retry delay prevents further requests`() = runTest {
        val friendsManager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        coEvery { friendsRepository.getFriends() } throws IOException("Offline")

        friendsManager.observeFriends().test {
            awaitItem()
            runCurrent()
            friendsManager.cancel()
            advanceTimeBy(60_000)
            runCurrent()
            coVerify(exactly = 1) { friendsRepository.getFriends() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `request cancellation is not retried`() = runTest {
        val friendsManager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        coEvery { friendsRepository.getFriends() } throws CancellationException("Cancelled")

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                advanceTimeBy(60_000)
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }
}
