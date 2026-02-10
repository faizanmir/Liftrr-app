package org.liftrr.ui.screens.session

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.liftrr.ui.theme.LiftrrTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupScreen(
    viewModel: SessionSetupViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDeviceConnection: () -> Unit = {},
    onStartWorkout: (WorkoutMode) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshConnectionStatus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Start Workout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is SessionSetupUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is SessionSetupUiState.Ready -> {
                SessionSetupContent(
                    deviceStatus = DeviceStatus(
                        isConnected = state.isDeviceConnected,
                        deviceName = state.deviceName,
                        batteryPercent = state.batteryPercent
                    ),
                    workoutModeOptions = state.workoutModeOptions,
                    onNavigateToDeviceConnection = onNavigateToDeviceConnection,
                    onStartWorkout = onStartWorkout,
                    modifier = Modifier.padding(padding)
                )
            }

            is SessionSetupUiState.Error -> {
                ErrorView(
                    message = state.message,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

data class DeviceStatus(
    val isConnected: Boolean,
    val deviceName: String?,
    val batteryPercent: Int
)

@Composable
private fun SessionSetupContent(
    deviceStatus: DeviceStatus,
    workoutModeOptions: List<WorkoutModeOption>,
    onNavigateToDeviceConnection: () -> Unit,
    onStartWorkout: (WorkoutMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "Content fade in"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .alpha(alpha)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Choose Your Tracking Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Select how you want to track your workout session",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Device Status Card
        DeviceStatusCard(deviceStatus = deviceStatus)

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Selection Cards (State hoisted from ViewModel)
        workoutModeOptions.forEach { option ->
            WorkoutModeCard(
                option = option,
                onActionClick = { action ->
                    when (action) {
                        is ModeAction.StartWorkout -> onStartWorkout(action.mode)
                        is ModeAction.ConnectDevice -> onNavigateToDeviceConnection()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Info Card
        if (!deviceStatus.isConnected) {
            InfoCard(
                text = "Connect your Liftrr sensor for accurate velocity tracking and real-time performance feedback.",
                onConnectClick = onNavigateToDeviceConnection
            )
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeviceStatusCard(
    deviceStatus: DeviceStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (deviceStatus.isConnected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (deviceStatus.isConnected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (deviceStatus.isConnected) Color(0xFF7CB342) else Color(0xFF9E9E9E)
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (deviceStatus.isConnected) "Device Connected" else "No Device Connected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (deviceStatus.isConnected && deviceStatus.deviceName != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = deviceStatus.deviceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (deviceStatus.batteryPercent > 0) {
                            BatteryIndicator(batteryPercent = deviceStatus.batteryPercent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryIndicator(
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                batteryPercent > 80 -> Icons.Filled.BatteryFull
                batteryPercent > 50 -> Icons.Filled.Battery6Bar
                batteryPercent > 20 -> Icons.Filled.Battery3Bar
                else -> Icons.Filled.Battery1Bar
            },
            contentDescription = "Battery",
            tint = when {
                batteryPercent > 20 -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$batteryPercent%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutModeCard(
    option: WorkoutModeOption,
    onActionClick: (ModeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (option.isAvailable) 1f else 0.6f
    val containerColor = if (option.isRecommended) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = if (option.isRecommended) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        modifier = modifier.alpha(alpha),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(
            width = if (option.isRecommended) 2.dp else 1.dp,
            color = borderColor
        ),
        tonalElevation = if (option.isRecommended) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon with gradient background
                IconWithGradient(
                    icon = option.icon,
                    isRecommended = option.isRecommended
                )

                // Title and Badge
                TitleSection(
                    title = option.title,
                    description = option.description,
                    badge = option.badge
                )
            }

            // Features List
            FeaturesList(
                features = option.features,
                isEnabled = option.isAvailable
            )

            // Action Button
            ActionButton(
                action = option.primaryAction,
                isRecommended = option.isRecommended,
                onActionClick = onActionClick
            )
        }
    }
}

@Composable
private fun IconWithGradient(
    icon: ImageVector,
    isRecommended: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                brush = if (isRecommended) {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun RowScope.TitleSection(
    title: String,
    description: String,
    badge: String?
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (badge != null) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturesList(
    features: List<Feature>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        features.forEach { feature ->
            FeatureItem(
                icon = feature.icon,
                text = feature.text,
                enabled = isEnabled
            )
        }
    }
}

@Composable
private fun ActionButton(
    action: ModeAction,
    isRecommended: Boolean,
    onActionClick: (ModeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (action) {
        is ModeAction.ConnectDevice -> {
            OutlinedButton(
                onClick = { onActionClick(action) },
                modifier = modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.BluetoothSearching,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Device")
            }
        }
        is ModeAction.StartWorkout -> {
            Button(
                onClick = { onActionClick(action) },
                modifier = modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecommended) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            ) {
                Text(
                    text = "Start Workout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            }
        )
    }
}

@Composable
private fun InfoCard(
    text: String,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(
                onClick = onConnectClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.BluetoothSearching,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Device Now")
            }
        }
    }
}

// Preview Helper
private fun createPreviewModeOptions(isConnected: Boolean) = listOf(
    WorkoutModeOption(
        mode = WorkoutMode.SENSOR_AND_CAMERA,
        icon = Icons.Filled.Sensors,
        title = "Sensor + Camera",
        description = "Full velocity tracking with pose analysis",
        badge = "Recommended",
        features = listOf(
            Feature(Icons.Outlined.Speed, "Real-time velocity tracking"),
            Feature(Icons.Outlined.Analytics, "Concentric & eccentric analysis"),
            Feature(Icons.Outlined.Videocam, "Pose form analysis")
        ),
        isAvailable = isConnected,
        isRecommended = true,
        primaryAction = if (isConnected) {
            ModeAction.StartWorkout(WorkoutMode.SENSOR_AND_CAMERA)
        } else {
            ModeAction.ConnectDevice
        }
    ),
    WorkoutModeOption(
        mode = WorkoutMode.CAMERA_ONLY,
        icon = Icons.Filled.Videocam,
        title = "Camera Only",
        description = "Pose analysis without velocity sensor",
        badge = null,
        features = listOf(
            Feature(Icons.Outlined.Videocam, "Pose form analysis"),
            Feature(Icons.Outlined.GraphicEq, "Rep counting"),
            Feature(Icons.Outlined.Timer, "Time under tension")
        ),
        isAvailable = true,
        isRecommended = false,
        primaryAction = ModeAction.StartWorkout(WorkoutMode.CAMERA_ONLY)
    )
)

// Previews
@Preview(name = "Session Setup - Connected")
@Composable
private fun SessionSetupScreenPreviewConnected() {
    LiftrrTheme {
        SessionSetupContent(
            deviceStatus = DeviceStatus(
                isConnected = true,
                deviceName = "LIFTRR-001",
                batteryPercent = 87
            ),
            workoutModeOptions = createPreviewModeOptions(true),
            onNavigateToDeviceConnection = {},
            onStartWorkout = { _ -> }
        )
    }
}

@Preview(name = "Session Setup - Disconnected")
@Composable
private fun SessionSetupScreenPreviewDisconnected() {
    LiftrrTheme {
        SessionSetupContent(
            deviceStatus = DeviceStatus(
                isConnected = false,
                deviceName = null,
                batteryPercent = 0
            ),
            workoutModeOptions = createPreviewModeOptions(false),
            onNavigateToDeviceConnection = {},
            onStartWorkout = { _ -> }
        )
    }
}

@Preview(name = "Session Setup - Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SessionSetupScreenPreviewDark() {
    LiftrrTheme {
        SessionSetupContent(
            deviceStatus = DeviceStatus(
                isConnected = true,
                deviceName = "LIFTRR-001",
                batteryPercent = 45
            ),
            workoutModeOptions = createPreviewModeOptions(true),
            onNavigateToDeviceConnection = {},
            onStartWorkout = { _ -> }
        )
    }
}
