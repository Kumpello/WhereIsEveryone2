package com.kumpello.whereiseveryone.main.common.domain.manager

import com.kumpello.whereiseveryone.main.common.database.FriendDao
import com.kumpello.whereiseveryone.main.common.database.toDatabaseEntity
import com.kumpello.whereiseveryone.main.common.database.toDomain
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class FriendsManager(
    private val getFriendsDataUseCase: GetFriendsDataUseCase,
    private val friendDao: FriendDao
) {
    private var job = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + job)
    private val pollingInterval = 15_000L
    private lateinit var currentResponse: FriendsResponse.FriendsData

    fun cancel() {
        job.cancel()
    }

    private fun ensureActiveScope() {
        if (!job.isActive) {
            job = SupervisorJob()
            scope = CoroutineScope(Dispatchers.IO + job)
        }
    }

    suspend fun observeFriends(): Flow<FriendsResponse> {
        ensureActiveScope()
        return flow {
            while (scope.isActive) {
                runCatching {
                    Timber.tag(TAG).d("Polling for fresh friends data")
                    val response = getFriendsDataUseCase.execute()
                    if (response is FriendsResponse.FriendsData) {
                        Timber.tag(TAG).d("Successfully fetched %d friends", response.positions.size)
                        currentResponse = response
                    }
                    emit(response)
                }.onFailure {
                    Timber.tag(TAG).e("Error during friends polling: %s", it.message)
                }

                delay(pollingInterval.milliseconds)
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FriendsResponse.FriendsData(friendDao.getFriends().map { it.toDomain() })
        ).onCompletion {
            Timber.tag(TAG).d("Saving friends to database")
            if (::currentResponse.isInitialized) {
                friendDao.insertFriends(currentResponse.positions.map { it.toDatabaseEntity() })
            }
        }
    }

    companion object {
        private const val TAG = "FRIENDS_MANAGER"
    }

}
