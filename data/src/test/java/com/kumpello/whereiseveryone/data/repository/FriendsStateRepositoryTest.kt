package com.kumpello.whereiseveryone.data.repository

import app.cash.turbine.test
import com.kumpello.whereiseveryone.data.local.database.FriendDao
import com.kumpello.whereiseveryone.data.local.database.FriendDatabaseEntity
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import com.kumpello.whereiseveryone.data.network.model.FriendData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsStateRepositoryTest {

    private val friendsRepository: FriendsRepository = mockk()
    private val friendDao: FriendDao = mockk(relaxed = true)

    @Test
    fun `resolving a request clears it without waiting for another successful refresh`() = runTest {
        val manager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        coEvery { friendsRepository.getFriends() } returns FriendsResponse.FriendsData(listOf(
            FriendData("alice", state = "pending_incoming", location = null, friend_since = null),
            FriendData("bob", state = "pending_incoming", location = null, friend_since = null),
        ))
        try {
            manager.refresh()
            manager.resolveIncomingFriendRequest("alice")
            assertEquals(listOf("bob"), manager.incomingFriendRequests.value)
            coEvery { friendsRepository.getFriends() } returns FriendsResponse.ErrorData(503, "Unavailable", "Unavailable")
            manager.refresh()
            assertEquals(listOf("bob"), manager.incomingFriendRequests.value)
            manager.resolveIncomingFriendRequest("bob")
            assertTrue(manager.incomingFriendRequests.value.isEmpty())
        } finally {
            manager.cancel()
        }
    }

    @Test
    fun `incoming requests track complete refreshes and retain names through errors`() = runTest {
        val manager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        coEvery { friendsRepository.getFriends() } returnsMany listOf(
            FriendsResponse.FriendsData(listOf(
                FriendData("alice", state = "pending_incoming", location = null, friend_since = null),
                FriendData("alice", state = "pending_incoming", location = null, friend_since = null),
                FriendData("outgoing", state = "pending_outgoing", location = null, friend_since = null),
                FriendData("accepted", state = "accepted", location = null, friend_since = null),
            )),
            FriendsResponse.ErrorData(503, "Unavailable", "Unavailable"),
            FriendsResponse.FriendsData(listOf(
                FriendData("alice", state = "accepted", location = null, friend_since = null),
                FriendData("bob", state = "pending_incoming", location = null, friend_since = null),
            )),
            FriendsResponse.FriendsData(emptyList()),
        )
        try {
            manager.refresh()
            assertEquals(listOf("alice"), manager.incomingFriendRequests.value)
            manager.refresh()
            assertEquals(listOf("alice"), manager.incomingFriendRequests.value)
            manager.refresh()
            assertEquals(listOf("bob"), manager.incomingFriendRequests.value)
            manager.refresh()
            assertEquals(emptyList<String>(), manager.incomingFriendRequests.value)
        } finally {
            manager.cancel()
        }
    }

    @Test
    fun `ending a session clears requests and ignores an earlier in flight refresh`() = runTest {
        val manager = FriendsStateRepository(friendsRepository, friendDao, StandardTestDispatcher(testScheduler))
        val delayedResponse = CompletableDeferred<FriendsResponse>()
        val incoming = FriendsResponse.FriendsData(listOf(
            FriendData("alice", state = "pending_incoming", location = null, friend_since = null)
        ))
        coEvery { friendsRepository.getFriends() } returns incoming
        manager.refresh()
        assertEquals(listOf("alice"), manager.incomingFriendRequests.value)
        coEvery { friendsRepository.getFriends() } coAnswers { delayedResponse.await() }
        val refresh = async { manager.refresh() }
        runCurrent()
        manager.cancel()
        delayedResponse.complete(incoming)
        refresh.await()
        assertTrue(manager.incomingFriendRequests.value.isEmpty())
    }

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
    fun `power changes immediately switch between one and fifteen second polling`() = runTest {
        val charging = MutableStateFlow(false)
        val friendsManager = FriendsStateRepository(
            friendsRepository, friendDao, StandardTestDispatcher(testScheduler), charging,
        )
        coEvery { friendsRepository.getFriends() } returns FriendsResponse.FriendsData(emptyList())

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                advanceTimeBy(5.seconds)
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }

                charging.value = true
                runCurrent()
                coVerify(exactly = 2) { friendsRepository.getFriends() }
                advanceTimeBy(999.milliseconds)
                runCurrent()
                coVerify(exactly = 2) { friendsRepository.getFriends() }
                advanceTimeBy(1.milliseconds)
                runCurrent()
                coVerify(exactly = 3) { friendsRepository.getFriends() }

                charging.value = false
                runCurrent()
                coVerify(exactly = 4) { friendsRepository.getFriends() }
                advanceTimeBy(14_999.milliseconds)
                runCurrent()
                coVerify(exactly = 4) { friendsRepository.getFriends() }
                advanceTimeBy(1.milliseconds)
                runCurrent()
                coVerify(exactly = 5) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }

    @Test
    fun `powered polling retries network and HTTP failures every second`() = runTest {
        val friendsManager = FriendsStateRepository(
            friendsRepository, friendDao, StandardTestDispatcher(testScheduler), flowOf(true),
        )
        val freshData = FriendsResponse.FriendsData(emptyList())
        val error = FriendsResponse.ErrorData(503, "Unavailable", "Unavailable")
        coEvery { friendDao.getFriends() } returns listOf(
            FriendDatabaseEntity("cached", "status", "accepted", null, null)
        )
        coEvery { friendsRepository.getFriends() } throws IOException("Offline") andThen error andThen freshData

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                advanceTimeBy(999.milliseconds)
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                advanceTimeBy(1.milliseconds)
                runCurrent()
                assertEquals(error, awaitItem())
                advanceTimeBy(1.seconds)
                runCurrent()
                assertEquals(freshData, awaitItem())
                advanceTimeBy(1.seconds)
                runCurrent()
                coVerify(exactly = 4) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }

    @Test
    fun `changing power cancels an in flight fetch before starting the replacement`() = runTest {
        val charging = MutableStateFlow(false)
        val friendsManager = FriendsStateRepository(
            friendsRepository, friendDao, StandardTestDispatcher(testScheduler), charging,
        )
        val freshData = FriendsResponse.FriendsData(emptyList())
        var requests = 0
        var cancelled = false
        coEvery { friendDao.getFriends() } returns listOf(
            FriendDatabaseEntity("cached", "status", "accepted", null, null)
        )
        coEvery { friendsRepository.getFriends() } coAnswers {
            requests++
            if (requests == 1) {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            assertTrue(cancelled)
            freshData
        }

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                charging.value = true
                runCurrent()
                assertEquals(freshData, awaitItem())
                assertTrue(cancelled)
                coVerify(exactly = 2) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }

    @Test
    fun `ending a powered session cancels retries and power observation`() = runTest {
        val charging = MutableStateFlow(true)
        val friendsManager = FriendsStateRepository(
            friendsRepository, friendDao, StandardTestDispatcher(testScheduler), charging,
        )
        coEvery { friendsRepository.getFriends() } throws IOException("Offline")

        try {
            friendsManager.observeFriends().test {
                awaitItem()
                runCurrent()
                assertEquals(1, charging.subscriptionCount.value)
                friendsManager.cancel()
                runCurrent()
                assertEquals(0, charging.subscriptionCount.value)
                charging.value = false
                advanceTimeBy(60.seconds)
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
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

                advanceTimeBy(14_999.milliseconds)
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                advanceTimeBy(1.milliseconds)
                runCurrent()
                coVerify(exactly = 2) { friendsRepository.getFriends() }
                expectNoEvents()

                advanceTimeBy(15.seconds)
                runCurrent()
                assertEquals(freshData, awaitItem())
                advanceTimeBy(15.seconds)
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
                advanceTimeBy(15.seconds)
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
            advanceTimeBy(60.seconds)
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
                advanceTimeBy(60.seconds)
                runCurrent()
                coVerify(exactly = 1) { friendsRepository.getFriends() }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            friendsManager.cancel()
        }
    }
}
