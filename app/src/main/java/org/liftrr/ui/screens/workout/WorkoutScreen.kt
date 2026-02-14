package org.liftrr.ui.screens.workout

import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.domain.workout.RepData
import org.liftrr.ml.ExerciseType
import org.liftrr.ml.FramingFeedback
import org.liftrr.ml.PoseDetectionResult
import org.liftrr.ui.components.PoseCameraWithRecording
import org.liftrr.ui.components.PoseSkeletonOverlay
import org.liftrr.ui.components.PositioningGuideOverlay
import org.liftrr.ui.screens.session.WorkoutMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    workoutMode: WorkoutMode = WorkoutMode.SENSOR_AND_CAMERA,
    exerciseType: ExerciseType = ExerciseType.SQUAT,
    weight: Float? = null,
    onNavigateBack: () -> Unit = {},
    onWorkoutComplete: (WorkoutReport) -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    val coroutineScope = rememberCoroutineScope()
    var showPositioningGuide by remember { mutableStateOf(true) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Intercept back gesture during recording to show confirmation
    BackHandler(enabled = uiState.isRecording || uiState.repCount > 0) {
        showExitConfirmation = true
    }

    // Auto-hide positioning guide when well-framed for 2 seconds
    LaunchedEffect(uiState.poseQuality?.framingFeedback) {
        if (uiState.poseQuality?.framingFeedback == FramingFeedback.WELL_FRAMED) {
            delay(2000)
            showPositioningGuide = false
        }
    }

    // Hard timeout: hide guide after 10 seconds regardless
    LaunchedEffect(Unit) {
        delay(10_000)
        showPositioningGuide = false
    }

    // Set exercise type and weight when screen loads
    LaunchedEffect(exerciseType, weight) {
        viewModel.setExerciseType(exerciseType)
        weight?.let { viewModel.setWeight(it) }
    }

    // Pre-initialize pose detector to minimize delay when recording starts
    LaunchedEffect(Unit) {
        viewModel.preInitializePoseDetector()
    }

    // Auto-start recording when entering the workout screen
    // Wait for pose detector initialization to complete
    LaunchedEffect(uiState.isPoseDetectionInitializing) {
        if (!uiState.isPoseDetectionInitializing && !uiState.isRecording) {
            // Give camera a moment to stabilize
            delay(300)
            viewModel.startRecording()
            viewModel.observePoseDetection()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                Text(
                    text = when (workoutMode) {
                        WorkoutMode.SENSOR_AND_CAMERA -> "Workout - Sensor + Camera"
                        WorkoutMode.CAMERA_ONLY -> "Workout - Camera Only"
                    }
                )
            }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, contentDescription = "Back"
                    )
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
            )
        }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera Preview with Pose Detection and Video Recording
            key(cameraSelector) {
                PoseCameraWithRecording(
                    onFrameCaptured = { bitmap, timestamp, release ->
                        viewModel.processFrame(bitmap, timestamp, release)
                    }, isRecording = uiState.isRecording, onRecordingStarted = { file ->
                    // Video recording started successfully
                }, onRecordingStopped = { uri ->
                    // Save video URI to ViewModel
                    viewModel.setVideoUri(uri)
                }, onRecordingError = { error ->
                    // Handle recording error
                    // TODO: Show error to user
                }, cameraSelector = cameraSelector, dispatchers = viewModel.dispatchers, modifier = Modifier.fillMaxSize()
                )
            }

            // Pose skeleton overlay
            PoseSkeletonOverlay(
                pose = uiState.currentPose,
                isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA,
                modifier = Modifier.fillMaxSize()
            )

            // Positioning guide overlay (shows before recording starts)
            PositioningGuideOverlay(
                exerciseType = exerciseType,
                isVisible = showPositioningGuide && !uiState.isRecording,
                modifier = Modifier.fillMaxSize()
            )

            // Dark overlay gradient for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Top Controls Row
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Exercise Display (read-only)
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.exerciseType.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Stats Overlay
                WorkoutStatsOverlay(
                    repCount = uiState.repCount,
                    goodReps = uiState.goodReps,
                    badReps = uiState.badReps
                )

                // Camera Flip Button
                FilledTonalIconButton(
                    onClick = {
                        cameraSelector =
                            if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }
                    }, colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch, contentDescription = "Flip Camera"
                    )
                }
            }

            // Form feedback removed for cleaner preview

            // Bottom Feedback Banner
            BottomFeedbackBanner(
                currentPose = uiState.currentPose,
                formFeedback = uiState.formFeedback,
                lastRep = uiState.reps.lastOrNull(),
                poseQuality = uiState.poseQuality,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp)
            )

            // Bottom Controls
            WorkoutControls(
                isRecording = uiState.isRecording,
                onStartPause = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                        viewModel.observePoseDetection()
                    }
                },
                onStop = {
                    viewModel.stopRecording()
                    // Give video time to finish saving before finishing workout
                    coroutineScope.launch {
                        // Wait for video to finish saving (typical finalization time)
                        delay(1500)
                        // Generate workout report and navigate to summary
                        val report = viewModel.finishWorkout()
                        if (report != null) {
                            onWorkoutComplete(report)
                        }
                    }
                },
                onReset = { viewModel.resetReps() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            )

            // Pose status indicator removed for cleaner preview

            // Loading Overlay - shown while pose detection is initializing
            if (uiState.isPoseDetectionInitializing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Prepping...",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Exit confirmation dialog
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Leave Workout?") },
            text = {
                Text(
                    if (uiState.isRecording)
                        "You have an active recording with ${uiState.repCount} reps. Leaving will discard your workout data."
                    else
                        "You have ${uiState.repCount} reps recorded. Leaving will discard your workout data."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    if (uiState.isRecording) viewModel.stopRecording()
                    onNavigateBack()
                }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Stay")
                }
            }
        )
    }
}

@Composable
private fun WorkoutStatsOverlay(
    repCount: Int, goodReps: Int, badReps: Int, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Total Rep Counter
        StatItem(
            label = "Total",
            value = repCount.toString(),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Good Reps
        StatItem(
            label = "Good", value = goodReps.toString(), color = MaterialTheme.colorScheme.tertiary
        )

        // Bad Reps
        StatItem(
            label = "Bad", value = badReps.toString(), color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun StatItem(
    label: String, value: String, color: Color, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}


@Composable
private fun WorkoutControls(
    isRecording: Boolean,
    onStartPause: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start/Pause Button
        FloatingActionButton(
            onClick = onStartPause, containerColor = if (isRecording) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            }, modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRecording) "Pause" else "Start",
                modifier = Modifier.size(32.dp)
            )
        }

        // Stop Button
        if (isRecording) {
            FilledTonalIconButton(
                onClick = onStop, modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop, contentDescription = "Stop"
                )
            }
        }

        // Reset Button
        FilledTonalIconButton(
            onClick = onReset, modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter, contentDescription = "Reset Reps"
            )
        }
    }
}

@Composable
private fun BottomFeedbackBanner(
    currentPose: PoseDetectionResult,
    formFeedback: String,
    lastRep: RepData?,
    poseQuality: org.liftrr.ml.PoseQuality?,
    modifier: Modifier = Modifier
) {
    // Determine message and color based on current state
    val (message, backgroundColor, textColor) = when {
        // Error state - pose detection not available
        currentPose is PoseDetectionResult.Error -> {
            Triple(
                "Pose detection unavailable - ${currentPose.message}",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        }
        // No pose detected
        currentPose is PoseDetectionResult.NoPoseDetected -> {
            Triple(
                "Position yourself in frame",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        }
        // Last rep was bad (show for 2 seconds)
        lastRep != null && !lastRep.isGoodForm && (System.currentTimeMillis() - lastRep.timestamp) < 2000 -> {
            Triple(
                "Bad form - focus on technique",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        }
        // Last rep was good (show for 2 seconds)
        lastRep != null && lastRep.isGoodForm && (System.currentTimeMillis() - lastRep.timestamp) < 2000 -> {
            Triple(
                "Good rep! Keep it up",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        // Show actionable feedback if available
        poseQuality != null && poseQuality.actionableFeedback.isNotEmpty() -> {
            Triple(
                poseQuality.actionableFeedback.first(),
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        // Default: Show form feedback
        else -> {
            Triple(
                formFeedback.ifEmpty { "Ready to go" },
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    AnimatedVisibility(
        visible = message.isNotEmpty(), enter = fadeIn(), exit = fadeOut(), modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor.copy(alpha = 0.9f),
            tonalElevation = 4.dp
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}
