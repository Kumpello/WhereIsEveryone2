package com.kumpello.whereiseveryone.main

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
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
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.destinations.FriendsScreenDestination
import com.kumpello.whereiseveryone.destinations.MapScreenDestination
import com.kumpello.whereiseveryone.destinations.SettingsScreenDestination
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by inject()
    private val friendsViewModel: FriendsViewModel by inject()
    private val settingsViewModel: SettingsViewModel by inject()

    private var isBackGroundPermissionGranted =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkPermission(ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
    private var isFineLocationPermissionGranted = checkPermission(ACCESS_FINE_LOCATION)
    private var isCoarseLocationPermissionGranted = checkPermission(ACCESS_COARSE_LOCATION)
    private var locationService: LocationService? = null
    private var isLocationServiceBound: Boolean = false
    private var positionsService: PositionsService? = null
    private var isPositionsServiceBound: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(getPermissionsLauncher())
        startLocationService()
        bindLocationService()

        setContent {
            WhereIsEveryoneTheme {
                AuthenticationScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isLocationServiceBound) {
            locationService!!.setUpdateInterval(locationService!!.UPDATE_LOCATION_INTERVAL_FOREGROUND)
        }
    }

    override fun onResume() {
        super.onResume()
        startPositionsService()
        bindPositionsService()
    }

    override fun onPause() {
        super.onPause()
        stopPositionsService()
        unbindPositionsService()
    }

    override fun onStop() {
        super.onStop()
        if (isLocationServiceBound) {
            locationService!!.setUpdateInterval(locationService!!.UPDATE_LOCATION_INTERVAL_BACKGROUND)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindLocationService()
    }

    fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        //TODO: Add value to extra
        serviceIntent.putExtra(statusParam, "test value")
        applicationContext.startForegroundService(intent)
    }

    fun startPositionsService() {
        applicationContext.startService(Intent(this, PositionsService::class.java))
    }

    fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }

    fun stopPositionsService() {
        val serviceIntent = Intent(this, PositionsService::class.java)
        stopService(serviceIntent)
    }

    private fun bindLocationService() {
        Intent(this, LocationService::class.java).also { intent ->
            bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun bindPositionsService() {
        Intent(this, PositionsService::class.java).also { intent ->
            bindService(intent, positionsServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindLocationService() {
        Intent(this, LocationService::class.java).also {
            unbindService(locationServiceConnection)
        }
    }

    private fun unbindPositionsService() {
        Intent(this, PositionsService::class.java).also {
            unbindService(locationServiceConnection)
        }
    }

    private fun getPermissionsLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isBackGroundPermissionGranted =
                    permissions[ACCESS_BACKGROUND_LOCATION]
                        ?: isBackGroundPermissionGranted
            }
            isFineLocationPermissionGranted =
                permissions[ACCESS_FINE_LOCATION]
                    ?: isFineLocationPermissionGranted
            isCoarseLocationPermissionGranted =
                permissions[ACCESS_COARSE_LOCATION]
                    ?: isCoarseLocationPermissionGranted
            if (!isBackGroundPermissionGranted ||
                !isCoarseLocationPermissionGranted ||
                !isFineLocationPermissionGranted
            ) {
                //TODO: Action when user deny permissions
            }
        }
    }

    private fun requestPermissions(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        val permissionRequestList = ArrayList<String>()

        when {
            !isCoarseLocationPermissionGranted ->
                permissionRequestList.add(ACCESS_COARSE_LOCATION)
            !isFineLocationPermissionGranted ->
                permissionRequestList.add(ACCESS_FINE_LOCATION)
            !isBackGroundPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                permissionRequestList.add(ACCESS_BACKGROUND_LOCATION)
        }

        if (permissionRequestList.isNotEmpty()) {
            permissionLauncher.launch(permissionRequestList.toTypedArray())
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
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

    private val locationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            Timber.d("LocationServiceConnection: connected to service.")
            val binder = iBinder as LocationService.LocationBinder
            locationService = binder.service
            isLocationServiceBound = true
            // Do stuff
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.d("LocationServiceConnection: disconnected from service.")
            isLocationServiceBound = false
        }
    }

    private val positionsServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            Timber.d("PositionsServiceConnection: connected to service.")
            val binder = iBinder as PositionsService.PositionsBinder
            positionsService = binder.service
            isPositionsServiceBound = true
            // Do stuff
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.d("PositionsServiceConnection: disconnected from service.")
            isLocationServiceBound = false
        }
    }

    companion object {

        const val statusParam = "STATUS"
    }
}