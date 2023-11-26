package com.kumpello.whereiseveryone.map

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.LatLng
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.map.presentation.MainActivityViewModel
import com.kumpello.whereiseveryone.map.domain.repository.LocationService
import com.kumpello.whereiseveryone.friends.ui.Friends
import com.kumpello.whereiseveryone.map.ui.Map
import com.kumpello.whereiseveryone.settings.ui.Settings
import com.kumpello.whereiseveryone.ui.navigation.MainRoutes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, iBinder: IBinder) {
            Log.d("MainActivity:", "ServiceConnection: connected to service.")
            val binder = iBinder as LocationService.LocalBinder
            mService = binder.service
            mIsBound = true
            viewModel.event = mService!!.event
            viewModel.setFriendsService(mService!!.friendsService)
            mService!!.setCurrentToken(application.getAuthToken()!!)
            mService!!.startFriendsUpdates()
            mService!!.setUpdateInterval(mService!!.UPDATE_LOCATION_INTERVAL_FOREGROUND)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("MainActivity:", "ServiceConnection: disconnected from service.")
            mIsBound = false
            mService!!.stopFriendsUpdates()
            mService!!.setUpdateInterval(mService!!.UPDATE_LOCATION_INTERVAL_BACKGROUND)
        }
    }
    private lateinit var application: WhereIsEveryoneApplication
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isBackGroundPermissionGranted = false
    private var isFineLocationPermissionGranted = false
    private var isCoarseLocationPermissionGranted = false
    var mService: LocationService? = null
    var mIsBound: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MainActivityViewModel by viewModels()
        this.viewModel = viewModel
        this.application = getApplication() as WhereIsEveryoneApplication

        permissionLauncher = getPermissionsLauncher()
        requestPermissions()

        val intent = Intent(this, LocationService::class.java)
        applicationContext.startForegroundService(intent)

        viewModel._uiState.value = viewModel._uiState.value.copy(
            position = LatLng(
                application.getLatitude()!!,
                application.getLongitude()!!
            )
        )

        setContent {
            WhereIsEveryoneTheme {
                Navigation()
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

}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    WhereIsEveryoneTheme {
        MainActivity()
    }
}