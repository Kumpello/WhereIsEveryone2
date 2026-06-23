package com.kumpello.whereiseveryone.main

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainRoute
import com.kumpello.whereiseveryone.main.friends.presentation.FriendsViewModel
import com.kumpello.whereiseveryone.main.friends.ui.FriendsScreen
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceInterface
import com.kumpello.whereiseveryone.main.map.presentation.MapViewModel
import com.kumpello.whereiseveryone.main.map.ui.MapScreen
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.kumpello.whereiseveryone.main.settings.ui.SettingsScreen
import kotlinx.coroutines.launch
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class MainActivity : ComponentActivity(), LocationServiceInterface {

    private val mapViewModel: MapViewModel by viewModel()
    private val friendsViewModel: FriendsViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel { parametersOf(this@MainActivity) }

    private val getKeyUseCase: GetKeyUseCase by inject()

    private var locationService: LocationService? = null
    private var isLocationServiceBound: Boolean = false

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        permissionsLauncher = getPermissionsLauncher()
        mapViewModel.setPermissions(this)

        setContent {
            WhereIsEveryoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }

        lifecycleScope.launch {
            mapViewModel.action.collect { action ->
                when (action) {
                    is MapViewModel.Action.ShowPermissionSettings -> requestPermissionsOrStart(
                        permissionsLauncher,
                        action.permissions
                    ) {
                        lifecycleScope.launch {
                            val isEnabled = getKeyUseCase.getValue(WhereIsEveryoneApplication.LOCATION_SHARING_ENABLED_KEY)
                                ?.toBoolean() ?: true
                            if (isEnabled) {
                                initializeLocationServices()
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val isEnabled = getKeyUseCase.getValue(WhereIsEveryoneApplication.LOCATION_SHARING_ENABLED_KEY)
                ?.toBoolean() ?: true

            if (!isLocationServiceBound
                && !mapViewModel.state.value.permissions.containsValue(false)
                && isEnabled
            ) {
                initializeLocationServices()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isLocationServiceBound) {
            setLocationService(LocationService.UpdateType.Background)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isLocationServiceBound) {
            setLocationService(LocationService.UpdateType.Background)
        }
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
        Timber.tag(TAG).d("Stopping location service")
        locationService?.stopLocationService()
        unbindLocationService()
    }

    private fun bindLocationService() {
        if (!isLocationServiceBound) {
            Intent(this, LocationServiceImpl::class.java).also { intent ->
                isLocationServiceBound = bindService(intent, locationServiceConnection, BIND_AUTO_CREATE)
            }
        }
    }

    private fun unbindLocationService() {
        if (isLocationServiceBound) {
            unbindService(locationServiceConnection)
            isLocationServiceBound = false
            locationService = null
        }
    }

    private fun getPermissionsLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsResult ->
            if (!permissionsResult.containsValue(false)) {
                initializeLocationServices()
                requestBackgroundPermission(permissionsLauncher)
            } else {
                //TODO: Action when user deny permissions
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
            startDestination = MainRoute.Map
        ) {
            composable<MainRoute.Map> {
                MapScreen(navController = navController, viewModel = mapViewModel)
            }
            composable<MainRoute.Friends> {
                FriendsScreen(navController = navController, viewModel = friendsViewModel)
            }
            composable<MainRoute.Settings> {
                SettingsScreen(navController = navController, viewModel = settingsViewModel)
            }
        }
    }

    private val locationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            Timber.tag(TAG).d("LocationServiceConnection: connected to service.")
            val binder = iBinder as LocationServiceImpl.LocationBinder
            locationService = binder.service
            isLocationServiceBound = true
            // Do stuff
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.tag(TAG).d("LocationServiceConnection: disconnected from service.")
            locationService = null
        }
    }

    companion object {
        const val STATUS_PARAM = "STATUS"
        private const val TAG = "MAIN_ACTIVITY"
    }
}
