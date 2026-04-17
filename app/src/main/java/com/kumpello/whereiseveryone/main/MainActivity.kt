package com.kumpello.whereiseveryone.main

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainNavigation
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.friends.ui.FriendsScreen
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceInterface
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.map.presentation.PositionsService
import com.kumpello.whereiseveryone.main.map.presentation.PositionsServiceImpl
import com.kumpello.whereiseveryone.main.map.ui.MapScreen
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.kumpello.whereiseveryone.main.settings.ui.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class MainActivity : ComponentActivity(), LocationServiceInterface {

    private val mapViewModel: MapViewModel by viewModel()
    private val friendsViewModel: FriendsViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel { parametersOf(locationService) }

    private var locationService: LocationService? =
        null //TODO: Create abstraction for service + bound status
    private var positionsService: PositionsService? = null
    private var isLocationServiceBound: Boolean = false
    private var isPositionsServiceBound: Boolean = false

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        permissionsLauncher = getPermissionsLauncher()
        mapViewModel.setPermissions(this)

        setContent {
            WhereIsEveryoneTheme {
                MainScreen()
            }
        }

        lifecycleScope.launch {
            mapViewModel.action.collect { action ->
                when (action) {
                    is MapViewModel.Action.ShowPermissionSettings -> requestPermissionsOrStart(
                        permissionsLauncher,
                        action.permissions
                    ) {
                        initializeLocationServices()
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isLocationServiceBound
            && !mapViewModel.viewState.value.permissions.containsValue(false)
        ) {
            initializeLocationServices()
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

    override fun startLocationService() {
        val serviceIntent = Intent(applicationContext, LocationServiceImpl::class.java)
        //TODO: Add value to extra
        serviceIntent.putExtra(STATUS_PARAM, "test value")
        ContextCompat.startForegroundService(applicationContext, serviceIntent)
    }

    private fun setLocationService(type: LocationService.UpdateType) {
        when (type) {
            LocationService.UpdateType.Background -> locationService?.changeUpdateType(type)
            LocationService.UpdateType.Foreground -> locationService?.changeUpdateType(type)
        }
    }

    override fun stopLocationService() {
        Timber.d("Stopping location service")
        unbindLocationService()
        locationService?.stopLocationService()
    }

    private fun bindLocationService() {
        Intent(this, LocationServiceImpl::class.java).also { intent ->
            bindService(intent, locationServiceConnection, BIND_AUTO_CREATE)
        }
    }

    private fun bindPositionsService() {
        Intent(this, PositionsServiceImpl::class.java).also { intent ->
            bindService(intent, positionsServiceConnection, BIND_AUTO_CREATE)
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
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsResult ->
            if (permissionsResult.containsKey(ACCESS_BACKGROUND_LOCATION)) {
                if (permissionsResult[ACCESS_BACKGROUND_LOCATION] == true) {
                    //TODO: ?
                } else {
                    //TODO: Action when user deny permissions
                }
            } else {
                if (!permissionsResult.containsValue(false)) {
                    initializeLocationServices()
                    requestBackgroundPermission(permissionsLauncher)
                } else {
                    //TODO: Action when user deny permissions
                }
            }
            mapViewModel.setPermissions(this)
        }
    }

    private fun initializeLocationServices() {
        startLocationService()
        bindLocationService()
        setLocationService(LocationService.UpdateType.Foreground)
    }

    private fun requestPermissionsOrStart(
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        permissions: Map<String, Boolean>,
        function: () -> Unit
    ) {
        val neededPermissions = permissions
            .filter { permission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permission.key != ACCESS_BACKGROUND_LOCATION
                } else {
                    true
                }
            }
            .filter { permission -> !permission.value }
            .keys
        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        } else {
            function()
        }
    }

    private fun requestBackgroundPermission(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(arrayOf(ACCESS_BACKGROUND_LOCATION))
        }
    }

    @Composable
    private fun MainScreen() {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = MainNavigation.Map.route
        ) {
            composable(MainNavigation.Map.route) {
                MapScreen(navController = navController, viewModel = mapViewModel)
            }
            composable(MainNavigation.Friends.route) {
                FriendsScreen(navController = navController, viewModel = friendsViewModel)
            }
            composable(MainNavigation.Settings.route) {
                SettingsScreen(navController = navController, viewModel = settingsViewModel)
            }
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
            isPositionsServiceBound = false
        }
    }

    companion object {
        const val STATUS_PARAM = "STATUS"
    }
}