package com.kumpello.whereiseveryone.main

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.authentication.AuthenticationActivity
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.main.common.MainRoute
import com.kumpello.whereiseveryone.main.friends.presentation.AddFriendViewModel
import com.kumpello.whereiseveryone.main.friends.presentation.ShareProfileViewModel
import com.kumpello.whereiseveryone.main.friends.ui.FriendsScreen
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import com.kumpello.whereiseveryone.main.map.presentation.MapScreenViewModel
import com.kumpello.whereiseveryone.main.map.ui.MapScreen
import com.kumpello.whereiseveryone.main.settings.presentation.SettingsViewModel
import com.kumpello.whereiseveryone.main.settings.ui.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val mapScreenViewModel: MapScreenViewModel by viewModel()
    private val addFriendViewModel: AddFriendViewModel by viewModel()
    private val shareProfileViewModel: ShareProfileViewModel by viewModel()
    private val settingsViewModel: SettingsViewModel by viewModel()

    private val preferencesManager: PreferencesManager by inject()
    private val locationService: LocationService by inject()

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    private var nfcAdapter: NfcAdapter? = null

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        lifecycleScope.launch {
            val token = preferencesManager.get(PreferencesKey.AuthToken)
            if (token.isNullOrEmpty()) {
                Timber.tag(TAG).d("No auth token found, redirecting to AuthenticationActivity")
                val intent = Intent(this@MainActivity, AuthenticationActivity::class.java).apply {
                    data = this@MainActivity.intent.data
                }
                startActivity(intent)
                finish()
                return@launch
            }
        }

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
                    is ShareProfileViewModel.Action.Toast -> {
                        Toast.makeText(
                            this@MainActivity,
                            getString(action.id),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        lifecycleScope.launch {
            addFriendViewModel.action.collect { action ->
                when (action) {
                    is AddFriendViewModel.Action.NotifyFriendAdded -> {
                        navController?.let { controller ->
                            if (controller.currentDestination?.route?.contains("Map") == false) {
                                controller.navigate(MainRoute.Map) {
                                    popUpTo(MainRoute.Map) { inclusive = false }
                                }
                            }
                        }
                    }

                    is AddFriendViewModel.Action.Toast -> {
                        Toast.makeText(
                            this@MainActivity,
                            getString(action.id),
                            Toast.LENGTH_SHORT
                        ).show()
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
                            val isEnabled = preferencesManager.get(PreferencesKey.LocationSharingEnabled) ?: true
                            if (isEnabled) {
                                locationService.startLocationService()
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
            usernameToShare = preferencesManager.get(PreferencesKey.UserName)
            val isEnabled = preferencesManager.get(PreferencesKey.LocationSharingEnabled) ?: true

            if (!mapScreenViewModel.state.value.permissions.containsValue(false)
                && isEnabled
            ) {
                locationService.startLocationService()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        locationService.changeUpdateType(LocationService.UpdateType.Background)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationService.changeUpdateType(LocationService.UpdateType.Background)
    }

    private fun getPermissionsLauncher(): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsResult ->
            if (!permissionsResult.containsValue(false)) {
                locationService.startLocationService()
                requestBackgroundPermissionIfNeeded(permissionsLauncher)
            } else {
                //TODO: Action when user deny permissions
            }
            mapScreenViewModel.trigger(MapScreenViewModel.Event.SetPermissions(mapScreenViewModel.getPermissions(this)))
        }
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
        val controller = rememberNavController()
        navController = controller
        NavHost(
            navController = controller,
            startDestination = MainRoute.Map
        ) {
            composable<MainRoute.Map> {
                MapScreen(
                    navController = controller,
                    screenViewModel = mapScreenViewModel
                )
            }
            composable<MainRoute.Friends> {
                FriendsScreen(navController = controller)
            }
            composable<MainRoute.Settings> {
                SettingsScreen(
                    navController = controller,
                    viewModel = settingsViewModel
                )
            }
        }
    }

    companion object {
        private const val TAG = "MAIN_ACTIVITY"
    }
}
