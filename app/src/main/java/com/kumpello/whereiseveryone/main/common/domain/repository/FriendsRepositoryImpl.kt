package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.domain.model.FriendData
import com.kumpello.whereiseveryone.common.logging.httpFailure
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import timber.log.Timber

class FriendsRepositoryImpl(
    private val friendsApi: FriendsApi
) : FriendsRepository {

    override suspend fun getFriends(): FriendsResponse {
        val friends = LinkedHashMap<String, FriendData>()
        for (state in FriendState.entries) {
            var cursor: String? = null
            val visited = mutableSetOf<String>()
            do {
                currentCoroutineContext().ensureActive()
                val response = friendsApi.getFriends(state.state, cursor)
                if (!response.isSuccessful) {
                    Timber.tag(TAG).httpFailure("Fetch friends page", response.code())
                    return FriendsResponse.ErrorData(
                        response.code(), response.errorBody().toString(), response.message()
                    )
                }
                val page = response.body() ?: throw IOException("Missing friends page")
                if (page.items.size > PAGE_SIZE || page.items.any { it.state != state.state }) {
                    throw IOException("Invalid friends page")
                }
                page.items.forEach { friend ->
                    // A relationship can move between lists during traversal. Accepted
                    // entries take precedence, and each username is returned once.
                    friends.putIfAbsent(friend.username, friend)
                }
                cursor = page.next_cursor
                if (cursor != null && (cursor.isBlank() || !visited.add(cursor))) {
                    throw IOException("Friends page cursor did not advance")
                }
            } while (cursor != null)
        }
        // Publish only a complete traversal, preserving cached data on any failure.
        Timber.tag(TAG).d("Successfully fetched all friends pages")
        return FriendsResponse.FriendsData(friends.values.toList())
    }

    companion object {
        private const val TAG = "FRIENDS_REPO"
        private const val PAGE_SIZE = 50
    }

}
