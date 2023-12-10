package com.kumpello.whereiseveryone.main

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.kumpello.whereiseveryone.NavGraphs
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.destinations.FriendsScreenDestination
import com.kumpello.whereiseveryone.destinations.MapScreenDestination
import com.kumpello.whereiseveryone.destinations.SettingsScreenDestination
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.map.domain.repository.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val mapViewModel : MapViewModel by inject()
    private val friendsViewModel : FriendsViewModel by inject()
    private val settingsViewModel : SettingsViewModel by inject()
    private val getKeyUseCase : GetKeyUseCase by inject()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            Timber.d("ServiceConnection: connected to service.")
            val binder = iBinder as LocationService.LocalBinder
            mService = binder.service
            mIsBound = true
            viewModel.positions = mService!!.event
            mService!!.setCurrentToken(getKeyUseCase.getAuthToken()!!)
            mService!!.startFriendsUpdates()
            mService!!.setUpdateInterval(mService!!.UPDATE_LOCATION_INTERVAL_FOREGROUND)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.d("ServiceConnection: disconnected from service.")
            mIsBound = false
            mService!!.stopFriendsUpdates()
            mService!!.setUpdateInterval(mService!!.UPDATE_LOCATION_INTERVAL_BACKGROUND)
        }
    }
    private lateinit var viewModel: MapViewModel
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isBackGroundPermissionGranted = false
    private var isFineLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false
    var mService: LocationService? = null
    var mIsBound: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = getPermissionsLauncher()
        requestPermissions()

        val intent = Intent(this, LocationService::class.java)
        applicationContext.startForegroundService(intent)

        setContent {
            WhereIsEveryoneTheme {
                AuthenticationScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onStop() {
        super.onStop()
        unbindService()
        mIsBound = false
    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }

    private fun bindService() {
        Intent(this, LocationService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindService() {
        Intent(this, LocationService::class.java).also {
            unbindService(serviceConnection)
        }
    }

    private fun getPermissionsLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isBackGroundPermissionGranted =
                    permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION]
                        ?: isBackGroundPermissionGranted
            }
            isFineLocationPermissionGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                    ?: isFineLocationPermissionGranted
            isCoarseLocationPermissionGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION]
                    ?: isCoarseLocationPermissionGranted
            if (!isBackGroundPermissionGranted ||
                !isCoarseLocationPermissionGranted ||
                !isFineLocationPermissionGranted
            ) {
                //TODO: Action when user deny permissions
            }
        }
    }

    private fun requestPermissions() {
        isCoarseLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        isFineLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isBackGroundPermissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        val permissionRequestList = ArrayList<String>()

        if (!isCoarseLocationPermissionGranted) {
            permissionRequestList.add(Manifest.permission.READ_CONTACTS)
        }

        if (!isFineLocationPermissionGranted) {
            permissionRequestList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (!isBackGroundPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionRequestList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (permissionRequestList.isNotEmpty()) {
            permissionLauncher.launch(permissionRequestList.toTypedArray())
        }
    }

    @Composable
    private fun AuthenticationScreen() {
        WhereIsEveryoneTheme {
            DestinationsNavHost(
                navGraph = NavGraphs.main,
                dependenciesContainerBuilder = {
                    dependency(MapScreenDestination) { mapViewModel }
                    dependency(SettingsScreenDestination) { settingsViewModel }
                    dependency(FriendsScreenDestination) { friendsViewModel }
                }
            )
        }
    }
}