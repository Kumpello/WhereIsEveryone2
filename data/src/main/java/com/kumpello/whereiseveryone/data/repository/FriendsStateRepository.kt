package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.local.database.FriendDao
import com.kumpello.whereiseveryone.data.local.database.toDatabaseEntity
import com.kumpello.whereiseveryone.data.local.database.toDomain
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import com.kumpello.whereiseveryone.data.model.FriendState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

class FriendsStateRepository internal constructor(
    private val friendsRepository: FriendsRepository,
    private val friendDao: FriendDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var job = SupervisorJob()
    private var scope = CoroutineScope(dispatcher + job)
    private val pollingInterval = 15_000L
    private var currentResponse: FriendsResponse.FriendsData? = null
    private val refreshMutex = Mutex()
    private val sessionVersion = AtomicLong()
    private val incomingRequests = MutableStateFlow<List<String>>(emptyList())

    val incomingFriendRequests = incomingRequests.asStateFlow()

    fun cancel() {
        sessionVersion.incrementAndGet()
        job.cancel()
        currentResponse = null
        incomingRequests.value = emptyList()
    }

    suspend fun refresh(): FriendsResponse {
        val version = sessionVersion.get()
        return withContext(dispatcher) {
            refreshMutex.withLock {
                val response = friendsRepository.getFriends()
                currentCoroutineContext().ensureActive()
                if (version == sessionVersion.get() && response is FriendsResponse.FriendsData) {
                    currentResponse = response
                    incomingRequests.value = response.positions.asSequence()
                        .filter { it.state == FriendState.PENDING_INCOMING.state }
                        .map { it.username }
                        .distinct()
                        .sorted()
                        .toList()
                }
                response
            }
        }
    }

    suspend fun resolveIncomingFriendRequest(username: String) {
        val version = sessionVersion.get()
        withContext(dispatcher) {
            refreshMutex.withLock {
                if (version == sessionVersion.get()) {
                    incomingRequests.value = incomingRequests.value.filterNot { it == username }
                }
            }
        }
    }

    suspend fun observeFriends(): Flow<FriendsResponse> {
        ensureActiveScope()
        val version = sessionVersion.get()
        return flow {
            while (currentCoroutineContext().isActive) {
                Timber.tag(TAG).d("Polling for fresh friends data")
                val response = refresh()
                if (response is FriendsResponse.FriendsData) {
                    Timber.tag(TAG).d("Successfully fetched %d friends", response.positions.size)
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
            val response = currentResponse
            if (version == sessionVersion.get() && response != null) {
                friendDao.insertFriends(response.positions.map { it.toDatabaseEntity() })
            }
        }
    }

    private fun ensureActiveScope() {
        if (!job.isActive) {
            job = SupervisorJob()
            scope = CoroutineScope(dispatcher + job)
        }
    }

    companion object {
        private const val TAG = "FRIENDS_MANAGER"
    }

}
