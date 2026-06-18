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
        emit(FriendsResponse.FriendsData(initialEntities.map { it.toDomain() }))

        while (currentCoroutineContext().isActive) {
            runCatching {
                val response = getFriendsDataUseCase.execute()
                if (response is FriendsResponse.FriendsData) {
                    friendDao.insertFriends(response.positions.map { it.toDatabaseEntity() })
                }
                emit(response)
            }.onFailure {
                Timber.e(it)
            }

            delay(pollingInterval.milliseconds)
        }
    }.flowOn(Dispatchers.IO)

}
