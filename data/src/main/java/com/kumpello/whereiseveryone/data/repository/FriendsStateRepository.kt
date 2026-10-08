package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.local.database.FriendDao
import com.kumpello.whereiseveryone.data.local.database.toDatabaseEntity
import com.kumpello.whereiseveryone.data.local.database.toDomain
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class FriendsStateRepository internal constructor(
    private val friendsRepository: FriendsRepository,
    private val friendDao: FriendDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var job = SupervisorJob()
    private var scope = CoroutineScope(dispatcher + job)
    private val pollingInterval = 15_000L
    private lateinit var currentResponse: FriendsResponse.FriendsData

    fun cancel() {
        job.cancel()
    }

    private fun ensureActiveScope() {
        if (!job.isActive) {
            job = SupervisorJob()
            scope = CoroutineScope(dispatcher + job)
        }
    }

    suspend fun observeFriends(): Flow<FriendsResponse> {
        ensureActiveScope()
        return flow {
            while (currentCoroutineContext().isActive) {
                Timber.tag(TAG).d("Polling for fresh friends data")
                val response = friendsRepository.getFriends()
                if (response is FriendsResponse.FriendsData) {
                    Timber.tag(TAG).d("Successfully fetched %d friends", response.positions.size)
                    currentResponse = response
                }
                emit(response)
                delay(pollingInterval.milliseconds)
            }
        }.retryWhen { cause, _ ->
            if (cause is CancellationException) throw cause
            Timber.tag(TAG).w(cause, "Friends polling failed; will retry")
            delay(pollingInterval.milliseconds)
            true
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
