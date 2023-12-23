package com.kumpello.whereiseveryone.main

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import com.kumpello.whereiseveryone.main.map.domain.usecase.GetFriendsPositionsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class PositionsService : Service() {

    private val binder: IBinder = PositionsBinder()
    private val UPDATE_FRIENDS_INTERVAL = 3000 // 3 min
    private val _positions = MutableSharedFlow<PositionsResponse>()
    val positions = _positions.asSharedFlow()
    private val getFriendsPositionsUseCase: GetFriendsPositionsUseCase by inject()

    private var updateFriends = true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.d("PositionsService stopping")
        super.onDestroy()
    }

    inner class PositionsBinder : Binder() {
        val service: PositionsService
            get() = this@PositionsService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun startFriendsUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            while (updateFriends) {
                Timber.d("Trying to get location")
                val positions = getFriendsPositionsUseCase.execute()
                Timber.d("Emiting friends location")
                this@PositionsService._positions.emit(positions)
                delay(UPDATE_FRIENDS_INTERVAL.toLong())
            }
        }
    }
}