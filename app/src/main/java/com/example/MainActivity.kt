package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.camera.CameraController
import com.example.camera.OrientationSensorHelper
import com.example.gallery.GalleryScreen
import com.example.media.MediaRepository
import com.example.settings.CameraPreferencesRepository
import com.example.settings.CameraSettings
import com.example.settings.SettingsScreen
import com.example.ui.CameraMainScreen
import com.example.ui.components.PermissionExplainerSheet
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    private var cameraController: CameraController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppRoot()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraController?.onDestroy()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainAppRoot() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val mediaRepository = remember { MediaRepository(context) }
    val preferencesRepository = remember { CameraPreferencesRepository(context) }
    val sensorHelper = remember { OrientationSensorHelper(context) }
    val cameraSettings by preferencesRepository.settingsFlow.collectAsState(initial = CameraSettings())

    val cameraController = remember {
        CameraController(
            context = context,
            lifecycleOwner = lifecycleOwner,
            scope = scope,
            mediaRepository = mediaRepository
        )
    }

    // Required permissions depending on Android version
    val permissions = remember {
        val list = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        list
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (permissionsState.allPermissionsGranted) {
            NavHost(navController = navController, startDestination = "camera") {
                composable("camera") {
                    CameraMainScreen(
                        cameraController = cameraController,
                        sensorHelper = sensorHelper,
                        cameraSettings = cameraSettings,
                        onOpenGallery = { navController.navigate("gallery") },
                        onOpenSettings = { navController.navigate("settings") }
                    )
                }

                composable("gallery") {
                    GalleryScreen(
                        mediaRepository = mediaRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        settings = cameraSettings,
                        hardwareCapabilities = cameraController.hardwareCapabilities.collectAsState().value,
                        preferencesRepository = preferencesRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        } else {
            // Permission Explainer Onboarding
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PermissionExplainerSheet(
                    onRequestPermissions = {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                )
            }
        }
    }
}
