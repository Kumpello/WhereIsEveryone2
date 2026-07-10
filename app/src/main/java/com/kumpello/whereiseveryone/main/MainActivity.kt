package com.kumpello.whereiseveryone.main

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
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
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.common.domain.ucecase.GetKeyUseCase
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainRoute
import com.kumpello.whereiseveryone.main.friends.presentation.AddFriendViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.ShareProfileViewModel
import com.kumpello.whereiseveryone.main.friends.ui.FriendsScreen
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceImpl
import com.kumpello.whereiseveryone.main.map.presentation.LocationServiceInterface
import com.kumpello.whereiseveryone.main.map.presentation.MapScreenViewModel
import com.kumpello.whereiseveryone.main.map.ui.MapScreen
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.kumpello.whereiseveryone.main.settings.ui.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class MainActivity : ComponentActivity(), LocationServiceInterface {

    private val mapScreenViewModel: MapScreenViewModel by viewModel()
    private val addFriendViewModel: AddFriendViewModel by viewModel()
    private val shareProfileViewModel: ShareProfileViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel { parametersOf(this@MainActivity) }

    private val getKeyUseCase: GetKeyUseCase by inject()

    private var locationService: LocationService? = null
    private var isLocationServiceBound: Boolean = false

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Timber.tag(TAG).d("NFC is not available on this device")
        }

        permissionsLauncher = getPermissionsLauncher()
        // Initialize permissions
        mapScreenViewModel.trigger(MapScreenViewModel.Event.SetPermissions(mapScreenViewModel.getPermissions(this)))

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

        handleIntent(intent)

        lifecycleScope.launch {
            shareProfileViewModel.action.collect { action ->
                when (action) {
                    is ShareProfileViewModel.Action.TriggerNfcSharing -> {
                        val uri = "whereiseveryone://addfriend/${action.username}"
                        val message = NdefMessage(
                            arrayOf(
                                NdefRecord.createUri(uri),
                                NdefRecord.createApplicationRecord(packageName)
                            )
                        )
                        try {
                            val method = nfcAdapter?.javaClass?.getMethod(
                                "setNdefPushMessage",
                                NdefMessage::class.java,
                                Activity::class.java
                            )
                            method?.invoke(nfcAdapter, message, this@MainActivity)
                            Toast.makeText(
                                this@MainActivity,
                                "NFC Sharing enabled for ${action.username}. Bring devices together.",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "Error setting NDEF push message")
                            Toast.makeText(
                                this@MainActivity,
                                "NFC Sharing not supported on this device/version",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    else -> Unit
                }
            }
        }

        lifecycleScope.launch {
            mapScreenViewModel.action.collect { action ->
                when (action) {
                    is MapScreenViewModel.Action.ShowPermissionSettings -> requestPermissionsOrStart(
                        permissionsLauncher,
                        action.permissions
                    ) {
                        lifecycleScope.launch {
                            val isEnabled = getKeyUseCase.getValue(WhereIsEveryoneApplication.LOCATION_SHARING_ENABLED_KEY)
                                ?.toBoolean() ?: true
                            if (isEnabled) {
                                startLocationService()
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Timber.tag(TAG).d("handleIntent: %s", intent)
        intent?.data?.let { uri ->
            Timber.tag(TAG).d("handleIntent: uri = %s, scheme = %s, host = %s", uri, uri.scheme, uri.host)
            if (uri.scheme == "whereiseveryone" && uri.host == "addfriend") {
                Timber.tag(TAG).d("handleIntent: Triggering OnUriReceived with %s", uri)
                addFriendViewModel.trigger(AddFriendViewModel.Event.OnUriReceived(uri))
            }
        }
    }

    private var usernameToShare: String? = null

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            usernameToShare = getKeyUseCase.getValue(WhereIsEveryoneApplication.USER_NAME_KEY)
            val isEnabled = getKeyUseCase.getValue(WhereIsEveryoneApplication.LOCATION_SHARING_ENABLED_KEY)
                ?.toBoolean() ?: true

            if (!isLocationServiceBound
                && !mapScreenViewModel.state.value.permissions.containsValue(false)
                && isEnabled
            ) {
                startLocationService()
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

    fun startForegroundLocationService() {
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
                startLocationService()
                requestBackgroundPermissionIfNeeded(permissionsLauncher)
            } else {
                //TODO: Action when user deny permissions
            }
            mapScreenViewModel.trigger(MapScreenViewModel.Event.SetPermissions(mapScreenViewModel.getPermissions(this)))
        }
    }

    override fun startLocationService() {
        startForegroundLocationService()
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

    private fun requestBackgroundPermissionIfNeeded(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && shouldRequestBackgroundPermission()) {
            permissionLauncher.launch(arrayOf(ACCESS_BACKGROUND_LOCATION))
        }
    }

    private fun shouldRequestBackgroundPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        } else {
            false
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
                MapScreen(
                    navController = navController,
                    screenViewModel = mapScreenViewModel
                )
            }
            composable<MainRoute.Friends> {
                FriendsScreen(navController = navController)
            }
            composable<MainRoute.Settings> {
                SettingsScreen(
                    navController = navController,
                    viewModel = settingsViewModel
                )
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
