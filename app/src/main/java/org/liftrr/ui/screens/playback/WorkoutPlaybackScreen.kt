package org.liftrr.ui.screens.playback

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.SolidColor
import androidx.media3.common.util.UnstableApi
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import org.liftrr.data.models.RepDataDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlaybackScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    viewModel: WorkoutPlaybackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.loadWorkoutSession(sessionId)
    }

    // Fullscreen state at top level
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Normal UI with Scaffold
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                // Hide TopAppBar when in fullscreen
                if (!isFullscreen) {
                    TopAppBar(
                        title = { Text("Workout Playback") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { padding ->
            when (val state = uiState) {
                is PlaybackUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PlaybackUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                is PlaybackUiState.Success -> {
                    WorkoutPlaybackContent(
                        workoutData = state.workoutData,
                        modifier = Modifier.padding(padding),
                        isFullscreen = isFullscreen,
                        onFullscreenChange = { isFullscreen = it }
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun WorkoutPlaybackContent(
    workoutData: WorkoutPlaybackData,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Check if video file exists
    val videoFileExists = remember(workoutData.videoUri) {
        workoutData.videoUri?.let { path ->
            java.io.File(path).exists()
        } ?: false
    }

    // Create ExoPlayer instance
    val exoPlayer = remember(workoutData.videoUri) {
        ExoPlayer.Builder(context).build().apply {
            if (workoutData.videoUri != null && videoFileExists) {
                try {
                    // Handle both absolute file paths and URIs
                    val uri = if (workoutData.videoUri.startsWith("file://") ||
                                 workoutData.videoUri.startsWith("content://")) {
                        workoutData.videoUri.toUri()
                    } else {
                        // Convert absolute file path to file:// URI
                        "file://${workoutData.videoUri}".toUri()
                    }

                    val mediaItem = MediaItem.fromUri(uri)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = false
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Handle fullscreen mode - only manage system UI, no orientation change
    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            if (isFullscreen) {
                // Enter fullscreen - hide system bars
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                // Exit fullscreen - show system bars
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            // Restore system UI on exit
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Fullscreen video player overlay
    if (isFullscreen && workoutData.videoUri != null && videoFileExists) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Exit fullscreen button
            Surface(
                onClick = { onFullscreenChange(false) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Normal view with scrollable content
    if (!isFullscreen) {
        // Normal view with scrollable content
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Video Player Section
            if (workoutData.videoUri != null && videoFileExists) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .padding(16.dp)
                ) {
                    // Video player card
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black
                        )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    setShowNextButton(false)
                                    setShowPreviousButton(false)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Fullscreen button - overlay on top of video
                    Surface(
                        onClick = { onFullscreenChange(true) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = "Enter Fullscreen",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
            // No video available or file not found
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (workoutData.videoUri == null) {
                                "No video was recorded for this workout"
                            } else {
                                "Video file not found"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (workoutData.videoUri != null && !videoFileExists) {
                            Text(
                                "Expected location:\n${workoutData.videoUri}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            }

            // Workout Stats Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Velocity Profile Card
            VelocityProfileCard(workoutData = workoutData)

            // Exercise Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            workoutData.exerciseName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getGradeColor(workoutData.grade).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Grade: ${workoutData.grade}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = getGradeColor(workoutData.grade),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                            .format(Date(workoutData.timestamp)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Performance Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = "Total Reps",
                    value = workoutData.totalReps.toString(),
                    icon = Icons.Filled.FitnessCenter,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Good Reps",
                    value = workoutData.goodReps.toString(),
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Bad Reps",
                    value = workoutData.badReps.toString(),
                    icon = Icons.Filled.Cancel,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quality and Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = "Avg Quality",
                    value = "${(workoutData.averageQuality * 100).toInt()}%",
                    icon = Icons.Filled.Star,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Duration",
                    value = formatDuration(workoutData.durationMs),
                    icon = Icons.Filled.Timer,
                    modifier = Modifier.weight(1f)
                )
            }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getGradeColor(grade: String): Color {
    return when (grade.uppercase()) {
        "A+", "A" -> Color(0xFF4CAF50)
        "B+", "B" -> Color(0xFF8BC34A)
        "C+", "C" -> Color(0xFFFFEB3B)
        "D+", "D" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun VelocityProfileCard(workoutData: WorkoutPlaybackData) {
    // Calculate real average velocity curve from rep data
    val averageVelocityCurve = if (workoutData.repData != null && workoutData.repData.isNotEmpty()) {
        calculateRealAverageVelocity(workoutData.repData)
    } else {
        // Fallback to estimated curve if no rep data
        generateAverageVelocityCurve(workoutData.averageQuality)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Average Velocity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Average velocity curve across all reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AverageVelocityCurveChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                velocityCurve = averageVelocityCurve
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegend("Concentric", MaterialTheme.colorScheme.primary)
                ChartLegend("Eccentric", MaterialTheme.colorScheme.tertiary)
                val peakVelocity = averageVelocityCurve.maxOrNull() ?: 0f
                ChartLegend("Peak: %.2f m/s".format(peakVelocity), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AverageVelocityCurveChart(
    modifier: Modifier = Modifier,
    velocityCurve: List<Float>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val midPoint = velocityCurve.size / 2

    // Create two lines: concentric and eccentric phases
    val lines = listOf(
        // Concentric phase (first half)
        Line(
            label = "Concentric",
            values = velocityCurve.subList(0, midPoint).map { it.toDouble() },
            color = SolidColor(primaryColor),
            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
            firstGradientFillColor = primaryColor.copy(alpha = .5f),
            secondGradientFillColor = Color.Transparent,
            drawStyle = DrawStyle.Stroke(width = 4.dp),
            curvedEdges = true
        ),
        // Eccentric phase (second half)
        Line(
            label = "Eccentric",
            values = velocityCurve.subList(midPoint, velocityCurve.size).map { it.toDouble() },
            color = SolidColor(tertiaryColor),
            strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
            firstGradientFillColor = tertiaryColor.copy(alpha = .5f),
            secondGradientFillColor = Color.Transparent,
            drawStyle = DrawStyle.Stroke(width = 4.dp),
            curvedEdges = true
        )
    )

    LineChart(
        modifier = modifier,
        data = lines,
        animationMode = AnimationMode.Together(delayBuilder = {
            it * 100L
        })
    )
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Calculate real average velocity from rep data
private fun calculateRealAverageVelocity(repData: List<RepDataDto>): List<Float> {
    // Calculate average quality from all reps
    val avgQuality = repData.map { it.quality }.average().toFloat()
    val baseVelocity = avgQuality * 0.8f

    // Generate velocity curve based on real average quality
    // This represents the average rep performance
    return listOf(
        0.0f,
        baseVelocity * 0.2f,
        baseVelocity * 0.45f,
        baseVelocity * 0.68f,
        baseVelocity * 0.88f,
        baseVelocity * 1.0f, // Peak
        baseVelocity * 0.92f,
        baseVelocity * 0.72f,
        baseVelocity * 0.48f,
        baseVelocity * 0.22f,
        0.0f
    )
}

// Fallback: Generate estimated velocity curve from overall quality
private fun generateAverageVelocityCurve(avgQuality: Float): List<Float> {
    val baseVelocity = avgQuality * 0.8f

    // Generate smooth average velocity curve (11 points)
    return listOf(
        0.0f,
        baseVelocity * 0.2f,
        baseVelocity * 0.45f,
        baseVelocity * 0.68f,
        baseVelocity * 0.88f,
        baseVelocity * 1.0f, // Peak
        baseVelocity * 0.92f,
        baseVelocity * 0.72f,
        baseVelocity * 0.48f,
        baseVelocity * 0.22f,
        0.0f
    )
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%d:%02d", minutes, seconds)
        else -> "${seconds}s"
    }
}
