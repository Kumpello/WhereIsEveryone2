package com.kumpello.whereiseveryone.main.map.presentation

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kumpello.whereiseveryone.main.common.domain.usecase.GetFriendsDataUseCase
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class PositionsServiceImpl : Service(), PositionsService {

    private val binder: IBinder = PositionsBinder()
    private val updateFriendsInterval = 30000 // 30 s
    private val _positions = MutableSharedFlow<PositionsResponse>()
    private val positions = _positions.asSharedFlow() //TODO: StateFlow? Only one receiver

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val getFriendsDataUseCase: GetFriendsDataUseCase by inject()

    private var updateFriends = true

    override fun onDestroy() {
        Timber.d("PositionsService stopping")
        super.onDestroy()
    }

    inner class PositionsBinder : Binder() {
        val service: PositionsServiceImpl
            get() = this@PositionsServiceImpl
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun startFriendsUpdates() { //TODO: Something's fucky about way it is done, refactor
        scope.launch {
            while (updateFriends) {
                runCatching {
                    Timber.d("Trying to get location")
                    val positions = getFriendsDataUseCase.execute()
                    Timber.d("Emiting friends location")
                    this@PositionsServiceImpl._positions.emit(positions)
                }.onFailure { error ->
                    Timber.d(error)
                }
                delay(updateFriendsInterval.toLong())
            }
        }
    }

    override fun getFriendsFlow(): SharedFlow<PositionsResponse> {
        return positions
    }
}