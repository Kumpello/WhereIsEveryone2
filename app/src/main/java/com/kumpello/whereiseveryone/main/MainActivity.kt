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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import com.kumpello.whereiseveryone.NavGraphs
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.destinations.MapScreenDestination
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.map.presentation.PositionsService
import com.kumpello.whereiseveryone.main.map.presentation.PositionsServiceImpl
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by viewModel()

    private var isBackGroundPermissionGranted = false //TODO: There is too much variables, Enum list, map?
    private var isFineLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false
    private var locationService: LocationService? = null //TODO: Create abstraction for service + bound status
    private var isLocationServiceBound: Boolean = false
    private var positionsService: PositionsService? = null
    private var isPositionsServiceBound: Boolean = false
    //TODO: Init somehow better, avoid vars

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        checkInitialPermissions()
        requestPermissionsOrStartLocationService(getPermissionsLauncher())

        setContent {
            WhereIsEveryoneTheme {
                MainScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isLocationServiceBound) {
            bindLocationService()
            setLocationService(LocationService.UpdateType.Foreground)
        }
        if (!isPositionsServiceBound) {
            bindPositionsService()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isLocationServiceBound) {
            setLocationService(LocationService.UpdateType.Background)
        }
        if (isPositionsServiceBound) {
            unbindPositionsService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindLocationService()
    }

    private fun startLocationService() {
        val serviceIntent = Intent(applicationContext, LocationServiceImpl::class.java)
        //TODO: Add value to extra
        serviceIntent.putExtra(STATUS_PARAM, "test value")
        ContextCompat.startForegroundService(applicationContext, intent)
        //applicationContext.startForegroundService(intent)
    }

    private fun setLocationService(type: LocationService.UpdateType) {
        when(type) {
            LocationService.UpdateType.Background -> locationService?.changeUpdateType(type)
            LocationService.UpdateType.Foreground -> locationService?.changeUpdateType(type)
        }
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationServiceImpl::class.java)
        stopService(serviceIntent)
    }

    private fun bindLocationService() {
        Intent(this, LocationServiceImpl::class.java).also { intent ->
            bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun bindPositionsService() {
        Intent(this, PositionsServiceImpl::class.java).also { intent ->
            bindService(intent, positionsServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindLocationService() {
        Intent(this, LocationServiceImpl::class.java).also {
            unbindService(locationServiceConnection)
        }
    }

    private fun unbindPositionsService() {
        Intent(this, PositionsServiceImpl::class.java).also {
            unbindService(positionsServiceConnection)
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
            } else {
                startLocationService()
            }
        }
    }

    private fun requestPermissionsOrStartLocationService(permissionLauncher: ActivityResultLauncher<Array<String>>) {
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
        } else {
            startLocationService()
        }
    }

    private fun checkInitialPermissions() {
        isBackGroundPermissionGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                checkPermission(ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        isFineLocationPermissionGranted = checkPermission(ACCESS_FINE_LOCATION)
        isCoarseLocationPermissionGranted = checkPermission(ACCESS_COARSE_LOCATION)
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    private fun MainScreen() {
        WhereIsEveryoneTheme {
            DestinationsNavHost(
                navGraph = NavGraphs.main,
                dependenciesContainerBuilder = {
                    dependency(MapScreenDestination) { mapViewModel }
                }
            )
        }
    }

    private val locationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            Timber.d("LocationServiceConnection: connected to service.")
            val binder = iBinder as LocationServiceImpl.LocationBinder
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
            val binder = iBinder as PositionsServiceImpl.PositionsBinder
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

        const val STATUS_PARAM = "STATUS"
    }
}