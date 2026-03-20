package org.liftrr.ui.screens.session

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.liftrr.domain.workout.WorkoutMode
import javax.inject.Inject

sealed class SessionSetupUiState {
    data object Loading : SessionSetupUiState()
    data class Ready(
        val workoutModeOptions: List<WorkoutModeOption> = emptyList()
    ) : SessionSetupUiState()
    data class Error(val message: String) : SessionSetupUiState()
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
}

@HiltViewModel
class SessionSetupViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<SessionSetupUiState>(SessionSetupUiState.Loading)
    val uiState: StateFlow<SessionSetupUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = SessionSetupUiState.Ready(
            workoutModeOptions = createWorkoutModeOptions()
        )
    }

    private fun createWorkoutModeOptions(): List<WorkoutModeOption> {
        return listOf(
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
                isRecommended = true,
                primaryAction = ModeAction.StartWorkout(WorkoutMode.CAMERA_ONLY)
            )
        )
    }
}
