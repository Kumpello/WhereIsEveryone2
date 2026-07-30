package com.kumpello.whereiseveryone.main.common.domain.manager

import app.cash.turbine.test
import com.kumpello.whereiseveryone.main.common.database.FriendDao
import com.kumpello.whereiseveryone.main.common.database.FriendDatabaseEntity
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsManagerTest {

    private val getFriendsDataUseCase: GetFriendsDataUseCase = mockk()
    private val friendDao: FriendDao = mockk(relaxed = true)
    private val friendsManager = FriendsManager(getFriendsDataUseCase, friendDao)

    @Test
    fun `observeFriends emits cached data then fresh data`() = runTest {
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
    }
}
