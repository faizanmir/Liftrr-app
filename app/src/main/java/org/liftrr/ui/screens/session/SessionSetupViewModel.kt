package org.liftrr.ui.screens.session

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.liftrr.domain.BluetoothConnector
import org.liftrr.domain.ConnectionState
import javax.inject.Inject

sealed class SessionSetupUiState {
    data object Loading : SessionSetupUiState()
    data class Ready(
        val isDeviceConnected: Boolean,
        val deviceName: String?,
        val batteryPercent: Int = 0,
        val signalStrength: Int = 0,
        val workoutModeOptions: List<WorkoutModeOption> = emptyList()
    ) : SessionSetupUiState()
    data class Error(val message: String) : SessionSetupUiState()
}

enum class WorkoutMode {
    SENSOR_AND_CAMERA,  // Full tracking with velocity sensor + pose analysis
    CAMERA_ONLY         // Pose analysis only, no velocity sensor
}

data class Feature(
    val icon: ImageVector,
    val text: String
)

data class WorkoutModeOption(
    val mode: WorkoutMode,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val badge: String?,
    val features: List<Feature>,
    val isAvailable: Boolean,
    val isRecommended: Boolean,
    val primaryAction: ModeAction
)

sealed class ModeAction {
    data class StartWorkout(val mode: WorkoutMode) : ModeAction()
    data object ConnectDevice : ModeAction()
}

@HiltViewModel
class SessionSetupViewModel @Inject constructor(
    private val bluetoothConnector: BluetoothConnector
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionSetupUiState>(SessionSetupUiState.Loading)
    val uiState: StateFlow<SessionSetupUiState> = _uiState.asStateFlow()

    init {
        observeBluetoothConnection()
    }

    private fun observeBluetoothConnection() {
        bluetoothConnector.connectionState
            .onEach { connectionState ->
                _uiState.update { currentState ->
                    when (connectionState) {
                        is ConnectionState.Connected -> {
                            val isConnected = true
                            SessionSetupUiState.Ready(
                                isDeviceConnected = isConnected,
                                deviceName = connectionState.device.name ?: "Unknown Device",
                                batteryPercent = 85, // TODO: Get from device characteristics
                                signalStrength = -45,  // TODO: Get from RSSI
                                workoutModeOptions = createWorkoutModeOptions(isConnected)
                            )
                        }
                        is ConnectionState.Disconnected -> {
                            val isConnected = false
                            SessionSetupUiState.Ready(
                                isDeviceConnected = isConnected,
                                deviceName = null,
                                workoutModeOptions = createWorkoutModeOptions(isConnected)
                            )
                        }
                        is ConnectionState.Connecting -> {
                            SessionSetupUiState.Loading
                        }
                        is ConnectionState.Error -> {
                            SessionSetupUiState.Error(connectionState.message)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    @SuppressLint("MissingPermission")
    fun refreshConnectionStatus() {
        // Force refresh by re-checking current state
        val currentState = bluetoothConnector.connectionState.value
        _uiState.update {
            when (currentState) {
                is ConnectionState.Connected -> {
                    SessionSetupUiState.Ready(
                        isDeviceConnected = true,
                        deviceName = currentState.device.name ?: "Unknown Device",
                        batteryPercent = 85,
                        signalStrength = -45,
                        workoutModeOptions = createWorkoutModeOptions(true)
                    )
                }
                is ConnectionState.Disconnected -> {
                    SessionSetupUiState.Ready(
                        isDeviceConnected = false,
                        deviceName = null,
                        workoutModeOptions = createWorkoutModeOptions(false)
                    )
                }
                is ConnectionState.Connecting -> SessionSetupUiState.Loading
                is ConnectionState.Error -> SessionSetupUiState.Error(currentState.message)
            }
        }
    }

    private fun createWorkoutModeOptions(isDeviceConnected: Boolean): List<WorkoutModeOption> {
        return listOf(
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
                isAvailable = isDeviceConnected,
                isRecommended = true,
                primaryAction = if (isDeviceConnected) {
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
    }
}
