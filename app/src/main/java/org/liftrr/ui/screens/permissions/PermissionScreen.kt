package org.liftrr.ui.screens.permissions

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import org.liftrr.ui.components.CircleShapeWithIcon
import org.liftrr.ui.components.OnBoardingScreenHeadline
import org.liftrr.ui.components.OnBoardingScreenHeadlineDescription
import org.liftrr.ui.theme.LiftrrTheme

enum class PermissionId {
    CAMERA, LOCATION, BLUETOOTH
}

data class PermissionItem(
    val id: PermissionId,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val rationale: String,
    val isRequired: Boolean = true
)

private val permissions = listOf(
    PermissionItem(
        id = PermissionId.BLUETOOTH,
        icon = Icons.Outlined.Bluetooth,
        title = "Bluetooth",
        description = "Connect to LIFTRR sensor device",
        rationale = "Bluetooth is essential for discovering and connecting to the LIFTRR sensor. Please grant this permission to enable device communication.",
        isRequired = true
    ), PermissionItem(
        id = PermissionId.CAMERA,
        icon = Icons.Outlined.CameraAlt,
        title = "Camera",
        description = "Record video for pose analysis",
        rationale = "Camera access allows the app to record your exercises for pose analysis, providing feedback on your form. This is an optional feature.",
        isRequired = false
    ), PermissionItem(
        id = PermissionId.LOCATION,
        icon = Icons.Outlined.LocationOn,
        title = "Location",
        description = "Required for Bluetooth scanning on Android",
        rationale = "Android requires location access to scan for nearby Bluetooth devices. This is a system requirement for Bluetooth functionality.",
        isRequired = true
    )
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(permissionScreenViewModel: PermissionScreenViewModel = hiltViewModel(), onContinueClicked: () -> Unit = {}) {
    val state = permissionScreenViewModel.uiState.collectAsStateWithLifecycle()

    // Hoist the permission states to this higher-level composable
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA) { isGranted ->
        permissionScreenViewModel.onPermissionResult(PermissionId.CAMERA, isGranted)
    }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { isGranted ->
        permissionScreenViewModel.onPermissionResult(PermissionId.LOCATION, isGranted)
    }

    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }
    val bluetoothPermissionState = rememberMultiplePermissionsState(bluetoothPermissions) { permissionResults ->
        permissionScreenViewModel.onPermissionResult(PermissionId.BLUETOOTH, permissionResults.all { it.value })
    }

    // This state is for the "Grant All" button to launch a single request
    val allPermissionsState = rememberMultiplePermissionsState(
        permissions = bluetoothPermissions + listOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
    )

    // Update the ViewModel with the initial status of the permissions
    LaunchedEffect(Unit) {
        permissionScreenViewModel.onPermissionResult(PermissionId.CAMERA, cameraPermissionState.status.isGranted)
        permissionScreenViewModel.onPermissionResult(PermissionId.LOCATION, locationPermissionState.status.isGranted)
        permissionScreenViewModel.onPermissionResult(PermissionId.BLUETOOTH, bluetoothPermissionState.allPermissionsGranted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PermissionScreenIcon()

        Spacer(modifier = Modifier.height(20.dp))

        PermissionScreenTitle()


        Spacer(modifier = Modifier.height(20.dp))

        OnBoardingScreenHeadlineDescription("LIFTRR needs a few permissions to work properly")

        Spacer(modifier = Modifier.height(32.dp))


        PermissionsList(cameraPermissionState, locationPermissionState, bluetoothPermissionState)

        Spacer(modifier = Modifier.weight(1f))


        ElevatedButton(onClick = onContinueClicked,
            shape = RoundedCornerShape(16.dp),
            enabled = state.value.bluetoothGranted && state.value.locationGranted && state.value.cameraGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()) {
            Text("Continue", modifier = Modifier.padding(10.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        FilledTonalButton(
            onClick = {
                allPermissionsState.launchMultiplePermissionRequest()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant All Permissions", modifier = Modifier.padding(10.dp))
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsList(
    cameraPermissionState: PermissionState,
    locationPermissionState: PermissionState,
    bluetoothPermissionState: MultiplePermissionsState
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(permissions) { permission ->
            val (isGranted, shouldShowRationale, launchRequest) = when (permission.id) {
                PermissionId.CAMERA -> {
                    val status = cameraPermissionState.status
                    Triple(
                        status.isGranted,
                        status is PermissionStatus.Denied && status.shouldShowRationale
                    ) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }

                PermissionId.LOCATION -> {
                    val status =
                        locationPermissionState.status
                    Triple(
                        status.isGranted,
                        status is PermissionStatus.Denied && status.shouldShowRationale
                    ) {
                        locationPermissionState.launchPermissionRequest()
                    }
                }

                PermissionId.BLUETOOTH -> {
                    Triple(
                        bluetoothPermissionState.allPermissionsGranted,
                        bluetoothPermissionState.shouldShowRationale
                    ) {
                        bluetoothPermissionState.launchMultiplePermissionRequest()
                    }
                }
            }
            PermissionItemCard(
                permissionItem = permission,
                isGranted = isGranted,
                shouldShowRationale = shouldShowRationale,
                onRequestPermission = { launchRequest() }
            )
        }
    }
}


@Composable
fun PermissionItemCard(
    permissionItem: PermissionItem,
    isGranted: Boolean,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        permissionItem.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f)
                ) {
                    Text(permissionItem.title, fontWeight = FontWeight.Bold)
                    Text(permissionItem.description, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onRequestPermission, enabled = !isGranted) {
                    Text(if (isGranted) "Granted" else "Grant")
                }
            }
            if (shouldShowRationale) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = permissionItem.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun PermissionScreenIcon() {
    CircleShapeWithIcon(Icons.Filled.Sensors)
}


@Composable
fun PermissionScreenTitle() {
    OnBoardingScreenHeadline("App Permissions")
}

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    LiftrrTheme {
        // As this screen uses permission states that require a real Activity,
        // the preview may not fully function but should render the basic layout.
        PermissionScreen()
    }
}
