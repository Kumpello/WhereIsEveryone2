package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
import org.junit.Rule
import org.junit.Test

class FriendsIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testLocation = Location(
        lat = 0.0,
        lon = 0.0,
        bearing = 0.0f,
        alt = AltDifference.WAY_HIGHER,
        accuracy = AccuracyLevel.PERFECT,
        lastUpdateTime = "1 min ago",
        lastUpdateAge = LastUpdateAge.FRESH,
        rawAlt = 0.0,
        rawAccuracy = 0.0f,
    )

    @Test
    fun friendsList_rendersMultipleFriends() {
        val friends = listOf(
            Friend("user1", "status", FriendState.ACCEPTED, testLocation),
            Friend("user2", "status", FriendState.PENDING_INCOMING, testLocation)
        )
        
        composeTestRule.setContent {
            // Simulating a list of friends
            friends.forEach { friend ->
                Friend(friend = friend, trigger = {})
            }
        }

        composeTestRule.onNodeWithText("user1").assertIsDisplayed()
        composeTestRule.onNodeWithText("user2").assertIsDisplayed()
    }
}
