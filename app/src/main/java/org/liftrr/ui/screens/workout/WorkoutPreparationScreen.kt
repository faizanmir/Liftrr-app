package org.liftrr.ui.screens.workout

import androidx.camera.core.CameraSelector
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.liftrr.ml.ExerciseType
import org.liftrr.ui.components.PoseCameraWithRecording
import org.liftrr.ui.components.PoseSkeletonOverlay
import org.liftrr.ui.screens.session.WorkoutMode

private val android.content.Context.weightDataStore by preferencesDataStore(name = "workout_weight")
private val LAST_WEIGHT_KEY = floatPreferencesKey("last_weight")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPreparationScreen(
    workoutMode: WorkoutMode,
    exerciseType: ExerciseType,
    onNavigateBack: () -> Unit = {},
    onStartRecording: (Float?) -> Unit = {},
    viewModel: CameraReadinessViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val readinessState by viewModel.readinessState.collectAsState()
    val latestPose by viewModel.latestPose.collectAsState()

    var weightText by remember { mutableStateOf("") }
    var showWarningDialog by remember { mutableStateOf(false) }

    // Load last used weight
    LaunchedEffect(Unit) {
        val lastWeight = context.weightDataStore.data.map { prefs ->
            prefs[LAST_WEIGHT_KEY]
        }.first()
        lastWeight?.let { weightText = it.toString() }
    }

    // Start readiness check
    LaunchedEffect(exerciseType) {
        viewModel.setExerciseType(exerciseType)
        viewModel.startReadinessCheck()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopReadinessCheck() }
    }

    val startWorkout: () -> Unit = {
        val weight = weightText.toFloatOrNull()
        if (weight != null && weight > 0) {
            scope.launch {
                context.weightDataStore.edit { prefs ->
                    prefs[LAST_WEIGHT_KEY] = weight
                }
            }
        }
        onStartRecording(weight)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Get Ready") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top: Live Camera Preview with Readiness Overlay
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                PoseCameraWithRecording(
                    onFrameCaptured = { bitmap, timestamp, release ->
                        viewModel.processFrame(bitmap, timestamp, release)
                    },
                    isRecording = false,
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                    modifier = Modifier.fillMaxSize()
                )

                // Pose skeleton overlay
                PoseSkeletonOverlay(
                    pose = latestPose,
                    isFrontCamera = false,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                                ),
                                startY = 0.5f
                            )
                        )
                )

                // Actionable feedback banner (top center)
                readinessState.poseQuality?.actionableFeedback?.firstOrNull()?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Readiness checklist overlay (bottom start)
                ReadinessChecklist(
                    state = readinessState,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            // Bottom: Workout details + start button
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Compact workout details
                WorkoutDetailsCard(
                    workoutMode = workoutMode,
                    exerciseType = exerciseType,
                    weightText = weightText,
                    onWeightChange = { weightText = it }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Readiness-aware start button
                ReadinessAwareStartButton(
                    readinessState = readinessState,
                    onStart = startWorkout,
                    onStartAnyway = { showWarningDialog = true }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showWarningDialog) {
        ReadinessWarningDialog(
            state = readinessState,
            onConfirm = {
                showWarningDialog = false
                startWorkout()
            },
            onDismiss = { showWarningDialog = false }
        )
    }
}

@Composable
private fun ReadinessChecklist(
    state: ReadinessState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReadinessCheckItem("Framing", state.isFramingGood)
            ReadinessCheckItem("Side view", state.isCameraAngleCorrect)
            ReadinessCheckItem("Body visible", state.areLandmarksVisible)
            ReadinessCheckItem("Lighting", state.isLightingAdequate)
        }
    }
}

@Composable
private fun ReadinessCheckItem(label: String, passed: Boolean) {
    val iconColor by animateColorAsState(
        targetValue = if (passed) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.error,
        animationSpec = tween(300),
        label = "check_color"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = iconColor
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (passed) FontWeight.Medium else FontWeight.Normal,
            color = if (passed) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ReadinessAwareStartButton(
    readinessState: ReadinessState,
    onStart: () -> Unit,
    onStartAnyway: () -> Unit
) {
    val allPassed = readinessState.allChecksPassed
    val containerColor by animateColorAsState(
        targetValue = if (allPassed) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.secondary,
        animationSpec = tween(300),
        label = "button_color"
    )

    Button(
        onClick = if (allPassed) onStart else onStartAnyway,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        Icon(
            imageVector = Icons.Default.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (allPassed) "Start Workout"
            else "Start Anyway (${readinessState.passedCount}/4)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReadinessWarningDialog(
    state: ReadinessState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val failedChecks = buildList {
        if (!state.isFramingGood) add("Framing — adjust your distance from camera")
        if (!state.isCameraAngleCorrect) add("Camera angle — position camera to your side")
        if (!state.areLandmarksVisible) add("Body visibility — ensure full body is in frame")
        if (!state.isLightingAdequate) add("Lighting — move to a brighter area")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup Not Optimal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("The following checks haven't passed:")
                failedChecks.forEach { check ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.error)
                        Text(check, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Form analysis may be less accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Start Anyway") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fix Setup") }
        }
    )
}

@Composable
private fun WorkoutDetailsCard(
    workoutMode: WorkoutMode,
    exerciseType: ExerciseType,
    weightText: String,
    onWeightChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Exercise",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = exerciseType.displayName(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider()

            // Mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = when (workoutMode) {
                            WorkoutMode.SENSOR_AND_CAMERA -> "Sensor + Camera"
                            WorkoutMode.CAMERA_ONLY -> "Camera Only"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider()

            // Weight (Optional)
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(top = 8.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Weight (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = onWeightChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter weight in kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        suffix = { Text("kg", style = MaterialTheme.typography.bodyMedium) }
                    )
                    Text(
                        text = "Leave empty to track reps only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
