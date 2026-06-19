package com.kumpello.whereiseveryone.main.common.domain.manager

import com.kumpello.whereiseveryone.main.common.database.FriendDao
import com.kumpello.whereiseveryone.main.common.database.toDatabaseEntity
import com.kumpello.whereiseveryone.main.common.database.toDomain
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class FriendsManager(
    private val getFriendsDataUseCase: GetFriendsDataUseCase,
    private val friendDao: FriendDao
) {
    private val pollingInterval = 15_000L

    fun observeFriends(): Flow<FriendsResponse> = flow {
        // Emit cached data first
        val initialEntities = friendDao.getFriends()
        Timber.tag(TAG).d("Emitting %d cached friends", initialEntities.size)
        emit(FriendsResponse.FriendsData(initialEntities.map { it.toDomain() }))

        while (currentCoroutineContext().isActive) {
            runCatching {
                Timber.tag(TAG).d("Polling for fresh friends data")
                val response = getFriendsDataUseCase.execute()
                if (response is FriendsResponse.FriendsData) {
                    Timber.tag(TAG).d("Successfully fetched %d friends", response.positions.size)
                    friendDao.insertFriends(response.positions.map { it.toDatabaseEntity() })
                }
                emit(response)
            }.onFailure {
                Timber.tag(TAG).e("Error during friends polling: %s", it.message)
            }

            delay(pollingInterval.milliseconds)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "FRIENDS_MANAGER"
    }

}
