package com.kumpello.whereiseveryone.main.common.domain.manager

import app.cash.turbine.test
import com.kumpello.whereiseveryone.main.common.database.FriendDao
import com.kumpello.whereiseveryone.main.common.database.FriendDatabaseEntity
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
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
class FriendsManagerTest {

    private val getFriendsDataUseCase: GetFriendsDataUseCase = mockk()
    private val friendDao: FriendDao = mockk(relaxed = true)

    @Test
    fun `observeFriends emits cached data then fresh data`() = runTest {
        val friendsManager = FriendsManager(getFriendsDataUseCase, friendDao, StandardTestDispatcher(testScheduler))
        val cachedEntity = FriendDatabaseEntity("user1", "status1", "accepted", null, null)
        coEvery { friendDao.getFriends() } returns listOf(cachedEntity)
        
        val freshData = FriendsResponse.FriendsData(emptyList())
        coEvery { getFriendsDataUseCase.execute() } returns freshData

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
        val friendsManager = FriendsManager(getFriendsDataUseCase, friendDao, StandardTestDispatcher(testScheduler))
        val freshData = FriendsResponse.FriendsData(emptyList())
        coEvery { friendDao.getFriends() } returns listOf(
            FriendDatabaseEntity("cached", "status", "accepted", null, null)
        )
        coEvery { getFriendsDataUseCase.execute() } throws IOException("Offline") andThenThrows
            IOException("Still offline") andThen freshData

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                coVerify(exactly = 1) { getFriendsDataUseCase.execute() }

                advanceTimeBy(14_999)
                runCurrent()
                coVerify(exactly = 1) { getFriendsDataUseCase.execute() }
                advanceTimeBy(1)
                runCurrent()
                coVerify(exactly = 2) { getFriendsDataUseCase.execute() }
                expectNoEvents()

                advanceTimeBy(15_000)
                runCurrent()
                assertEquals(freshData, awaitItem())
                advanceTimeBy(15_000)
                runCurrent()
                coVerify(exactly = 4) { getFriendsDataUseCase.execute() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }

    @Test
    fun `HTTP errors remain observable and polling continues at the normal interval`() = runTest {
        val friendsManager = FriendsManager(getFriendsDataUseCase, friendDao, StandardTestDispatcher(testScheduler))
        val error = FriendsResponse.ErrorData(503, "Unavailable", "Unavailable")
        coEvery { getFriendsDataUseCase.execute() } returns error

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                assertEquals(error, awaitItem())
                advanceTimeBy(15_000)
                runCurrent()
                coVerify(exactly = 2) { getFriendsDataUseCase.execute() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }

    @Test
    fun `cancelling manager during retry delay prevents further requests`() = runTest {
        val friendsManager = FriendsManager(getFriendsDataUseCase, friendDao, StandardTestDispatcher(testScheduler))
        coEvery { getFriendsDataUseCase.execute() } throws IOException("Offline")

        friendsManager.observeFriends().test {
            awaitItem()
            runCurrent()
            friendsManager.cancel()
            advanceTimeBy(60_000)
            runCurrent()
            coVerify(exactly = 1) { getFriendsDataUseCase.execute() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `request cancellation is not retried`() = runTest {
        val friendsManager = FriendsManager(getFriendsDataUseCase, friendDao, StandardTestDispatcher(testScheduler))
        coEvery { getFriendsDataUseCase.execute() } throws CancellationException("Cancelled")

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                advanceTimeBy(60_000)
                runCurrent()
                coVerify(exactly = 1) { getFriendsDataUseCase.execute() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }
}
