package org.liftrr.ui.screens.session

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import org.liftrr.domain.workout.WorkoutMode
import org.liftrr.ui.theme.LiftrrTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupScreen(
    viewModel: SessionSetupViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onStartWorkout: (WorkoutMode) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    workoutModeOptions = state.workoutModeOptions,
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

@Composable
private fun SessionSetupContent(
    workoutModeOptions: List<WorkoutModeOption>,
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

        // Mode Selection Cards (State hoisted from ViewModel)
        workoutModeOptions.forEach { option ->
            WorkoutModeCard(
                option = option,
                onActionClick = { action ->
                    when (action) {
                        is ModeAction.StartWorkout -> onStartWorkout(action.mode)
                    }
                },
                modifier = Modifier.fillMaxWidth()
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
        border = androidx.compose.foundation.BorderStroke(
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

// Previews
@Preview(name = "Session Setup")
@Composable
private fun SessionSetupScreenPreview() {
    LiftrrTheme {
        SessionSetupContent(
            workoutModeOptions = listOf(
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
            ),
            onStartWorkout = { _ -> }
        )
    }
}
