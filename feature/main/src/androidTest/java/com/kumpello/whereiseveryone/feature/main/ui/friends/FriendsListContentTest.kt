package com.kumpello.whereiseveryone.feature.main.ui.friends

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import com.kumpello.whereiseveryone.feature.main.R
import com.kumpello.whereiseveryone.core.presentation.AsyncState
import com.kumpello.whereiseveryone.core.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend
import com.kumpello.whereiseveryone.data.model.FriendState
import org.junit.Rule
import org.junit.Test

class FriendsListContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabClicksAndSwipesSelectTheMatchingFriendCategory() {
        showFriends(listOf(
            Friend("accepted_alex", "", FriendState.ACCEPTED, null),
            Friend("incoming_sam", "", FriendState.PENDING_INCOMING, null),
            Friend("outgoing_lee", "", FriendState.PENDING_OUTGOING, null)
        ))
        composeRule.onNodeWithText("accepted_alex").assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.friends_incoming_tab)).performClick()
        composeRule.onNodeWithText("incoming_sam").assertIsDisplayed()
        composeRule.onNodeWithText("accepted_alex").assertIsNotDisplayed()

        composeRule.onNodeWithText(string(R.string.friends_outgoing_tab)).performClick()
        composeRule.onNodeWithText("outgoing_lee").assertIsDisplayed()

        composeRule.onNodeWithTag("friends_pager").performTouchInput { swipeRight() }
        composeRule.onNodeWithText("incoming_sam").assertIsDisplayed()
        composeRule.onNodeWithText("outgoing_lee").assertIsNotDisplayed()
    }

    @Test
    fun emptyCategoriesExplainTheirState() {
        showFriends(emptyList())
        composeRule.onNodeWithText(string(R.string.friends_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.friends_incoming_tab)).performClick()
        composeRule.onNodeWithText(string(R.string.incoming_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.friends_outgoing_tab)).performClick()
        composeRule.onNodeWithText(string(R.string.outgoing_empty_title)).assertIsDisplayed()
    }

    private fun showFriends(friends: List<Friend>) {
        val state = FriendsViewModel.ViewState(
            friends = friends,
            deleteFriendDialogState = FriendsViewModel.DeleteFriendDialogState.Closed,
            selectedFriend = null,
            actionState = AsyncState.Idle,
            isShareDialogOpen = false,
            isNfcSharingDialogOpen = false,
            isNfcReadingDialogOpen = false,
            username = "Alex",
            friendUsername = ""
        )
        composeRule.setContent {
            WhereIsEveryoneTheme { FriendsListContent(viewState = state, onEvent = {}) }
        }
    }

    private fun string(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
