package com.kumpello.whereiseveryone.main.friends.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import org.junit.Rule
import org.junit.Test

class FriendTest {

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
    fun friend_displaysUsername() {
        val friend = Friend("user1", "status", FriendState.ACCEPTED, testLocation, friendSince = "2023-01-01")
        composeTestRule.setContent {
            Friend(friend = friend, trigger = {})
        }

        composeTestRule.onNodeWithText("user1").assertIsDisplayed()
    }

    @Test
    fun friend_accepted_showsDeleteButton() {
        val friend = Friend("user1", "status", FriendState.ACCEPTED, testLocation, friendSince = "2023-01-01")
        composeTestRule.setContent {
            Friend(friend = friend, trigger = {})
        }

        composeTestRule.onNodeWithTag("delete_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("accept_button").assertDoesNotExist()
    }

    @Test
    fun friend_pendingIncoming_showsAcceptAndRejectButtons() {
        val friend = Friend("user1", "status", FriendState.PENDING_INCOMING, testLocation, friendSince = "2023-01-01")
        composeTestRule.setContent {
            Friend(friend = friend, trigger = {})
        }

        composeTestRule.onNodeWithTag("accept_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reject_button").assertIsDisplayed()
    }

    @Test
    fun friend_click_triggersSelectFriendEvent() {
        var triggeredEvent: FriendsViewModel.Event? = null
        val friend = Friend("user1", "status", FriendState.ACCEPTED, testLocation, friendSince = "2023-01-01")
        composeTestRule.setContent {
            Friend(friend = friend, trigger = { triggeredEvent = it })
        }

        composeTestRule.onNodeWithTag("friend_card_user1").performClick()

        assert(triggeredEvent is FriendsViewModel.Event.SelectFriend)
        assertEquals("user1", (triggeredEvent as FriendsViewModel.Event.SelectFriend).friend.username)
    }
    
    private fun assertEquals(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected $expected but was $actual")
    }
}
