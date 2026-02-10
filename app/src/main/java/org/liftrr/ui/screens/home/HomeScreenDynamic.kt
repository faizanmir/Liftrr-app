package org.liftrr.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import org.liftrr.ui.theme.LiftrrTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onStartWorkout: () -> Unit = {},
    onWorkoutClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        uiState = uiState,
        onStartWorkout = onStartWorkout,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToAnalytics = onNavigateToAnalytics,
        onNavigateToProfile = onNavigateToProfile,
        onWorkoutClick = onWorkoutClick
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onStartWorkout: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onWorkoutClick: (String) -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartWorkout,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Start Workout",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            BottomNav(
                onHistoryClick = onNavigateToHistory,
                onAnalyticsClick = onNavigateToAnalytics,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Success -> {
                if (state.todayStats == null && state.recentActivity.isEmpty()) {
                    EmptyStateView(
                        deviceStatus = state.deviceStatus,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    HomeContent(
                        uiState = state,
                        modifier = Modifier.padding(padding),
                        onWorkoutClick = onWorkoutClick
                    )
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    deviceStatus: DeviceStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Empty state illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "No Workout Data Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Tap the + button to start your first workout\nand see your stats, velocity curves, and progress!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Device status
        if (deviceStatus.isConnected) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7CB342))
                    )
                    Text(
                        "${deviceStatus.deviceName} Connected • ${deviceStatus.batteryPercent}% Battery",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "No Device Connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState.Success,
    modifier: Modifier = Modifier,
    onWorkoutClick: (String) -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Section
        HeroSection(
            deviceStatus = uiState.deviceStatus
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Today's Session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Key Metrics
            uiState.todayStats?.let { stats ->
                KeyMetricsRow(stats = stats, isVisible = isVisible)
            }

            // Form Quality Score Card
            uiState.todayStats?.let { stats ->
                FormQualityCard(
                    formQuality = stats.formQuality,
                    isVisible = isVisible
                )
            }

            // Weekly Progress
            uiState.weeklyProgress?.let { progress ->
                WeeklyProgressCard(progress = progress, isVisible = isVisible)
            }

            // Recent Activity
            if (uiState.recentActivity.isNotEmpty()) {
                RecentActivitySection(
                    activities = uiState.recentActivity,
                    onWorkoutClick = onWorkoutClick
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HeroSection(
    deviceStatus: DeviceStatus
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        // Animated gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        0f to primaryColor.copy(alpha = 0.2f + animatedOffset * 0.1f),
                        0.5f to tertiaryColor.copy(alpha = 0.15f + animatedOffset * 0.1f),
                        1f to MaterialTheme.colorScheme.surface,
                        start = Offset(0f, 0f),
                        end = Offset(1000f * (1 + animatedOffset * 0.2f), 800f)
                    )
                )
        )

        // Blur circles
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .blur(50.dp)
                .background(primaryColor.copy(alpha = 0.25f), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 20.dp)
                .blur(40.dp)
                .background(tertiaryColor.copy(alpha = 0.2f), CircleShape)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "LIFTRR Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (deviceStatus.isConnected) Color(0xFF7CB342)
                            else Color(0xFFFF5252)
                        )
                )
                Text(
                    if (deviceStatus.isConnected) {
                        "${deviceStatus.deviceName} • ${deviceStatus.batteryPercent}% Battery"
                    } else {
                        "No Device Connected"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KeyMetricsRow(
    stats: TodayStats,
    isVisible: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompactMetricCard(
            value = stats.volume.toInt().toString(),
            unit = stats.volumeUnit,
            label = stats.volumeLabel,
            trend = "${stats.totalExercises} exercise${if (stats.totalExercises != 1) "s" else ""}",
            icon = Icons.Outlined.FitnessCenter,
            modifier = Modifier.weight(1f),
            isVisible = isVisible
        )
        CompactMetricCard(
            value = "${(stats.formQuality * 100).toInt()}",
            unit = "%",
            label = "Form Quality",
            trend = "${stats.goodReps}/${stats.totalReps} good",
            icon = Icons.Outlined.Speed,
            modifier = Modifier.weight(1f),
            isVisible = isVisible,
            delay = 80
        )
        CompactMetricCard(
            value = stats.totalSets.toString(),
            unit = "sets",
            label = "Workouts",
            trend = formatDuration(stats.totalDurationMs),
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            modifier = Modifier.weight(1f),
            isVisible = isVisible,
            delay = 160
        )
    }
}

@Composable
private fun CompactMetricCard(
    value: String,
    unit: String,
    label: String,
    trend: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    delay: Int = 0
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = delay),
        label = "alpha"
    )

    Card(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                trend,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7CB342),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FormQualityCard(
    formQuality: Float,
    isVisible: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 250),
        label = "form_quality_alpha"
    )

    // Calculate quality percentage and color
    val qualityPercent = (formQuality * 100).toInt()
    val qualityColor = when {
        formQuality >= 0.8f -> Color(0xFF7CB342) // Green
        formQuality >= 0.6f -> Color(0xFFFFB300) // Amber
        else -> Color(0xFFE53935) // Red
    }
    val qualityLabel = when {
        formQuality >= 0.9f -> "Excellent"
        formQuality >= 0.8f -> "Great"
        formQuality >= 0.7f -> "Good"
        formQuality >= 0.6f -> "Fair"
        else -> "Needs Work"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Today's Form Quality",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Overall technique score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = qualityColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        qualityLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = qualityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Large quality score display
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        qualityColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(70.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$qualityPercent%",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor
                    )
                    Text(
                        text = "Form Score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when {
                    formQuality >= 0.8f -> "Keep up the great work! Your form is on point."
                    formQuality >= 0.6f -> "Good effort! Focus on maintaining technique."
                    else -> "Room for improvement. Consider reviewing your form."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WeeklyProgressCard(
    progress: WeeklyProgress,
    isVisible: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 400),
        label = "weekly_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Weekly Volume",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Last 7 days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF7CB342).copy(alpha = 0.15f)
                ) {
                    Text(
                        progress.changePercent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF7CB342),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            ColumnChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                data = progress.volumeData.mapIndexed { index, value ->
                    Bars(
                        label = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[index],
                        values = listOf(
                            Bars.Data(
                                label = "Volume",
                                value = value.toDouble(),
                                color = if (index == progress.volumeData.lastIndex) {
                                    SolidColor(MaterialTheme.colorScheme.primary)
                                } else {
                                    SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                }
                            )
                        )
                    )
                },
                animationMode = AnimationMode.Together(delayBuilder = {
                    it * 100L
                })
            )
        }
    }
}

@Composable
private fun RecentActivitySection(
    activities: List<ActivityItem>,
    onWorkoutClick: (String) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Recent Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        activities.forEach { activity ->
            ActivityCard(
                activity = activity,
                onWorkoutClick = onWorkoutClick
            )
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityItem,
    onWorkoutClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { onWorkoutClick(activity.sessionId) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        activity.exercise,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getGradeColor(activity.grade).copy(alpha = 0.15f)
                    ) {
                        Text(
                            activity.grade,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = getGradeColor(activity.grade)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${activity.weight} • ${activity.reps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    activity.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                activity.timeAgo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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

private data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun BottomNav(
    onHistoryClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val items = remember {
        listOf(
            NavItem("Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
            NavItem("History", Icons.Filled.History, Icons.Outlined.History),
            NavItem("Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
            NavItem("Profile", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    val callbacks = remember(onHistoryClick, onAnalyticsClick, onProfileClick) {
        listOf<() -> Unit>({}, onHistoryClick, onAnalyticsClick, onProfileClick)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                AnimatedNavItem(
                    item = item,
                    isSelected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        callbacks[index]()
                    }
                )
            }
        }
    }
}

@Composable
private fun AnimatedNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = animationSpec,
        label = "iconScale"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "pillAlpha"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 56.dp else 0.dp,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "pillWidth"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "contentColor"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Animated pill background behind icon
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(32.dp)
                    .graphicsLayer { alpha = pillAlpha }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale),
                tint = contentColor
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)) + expandVertically(
                animationSpec = tween(200),
                expandFrom = Alignment.Top
            ),
            exit = fadeOut(tween(150)) + shrinkVertically(
                animationSpec = tween(150),
                shrinkTowards = Alignment.Top
            )
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

// Helpers
private fun getGradeColor(grade: String): Color {
    return when (grade.uppercase()) {
        "A+", "A" -> Color(0xFF4CAF50)
        "B+", "B" -> Color(0xFF8BC34A)
        "C+", "C" -> Color(0xFFFFEB3B)
        "D+", "D" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

// Preview Data
private val sampleTodayStats = TodayStats(
    volume = 2450f,
    volumeUnit = "kg",
    volumeLabel = "Volume",
    formQuality = 0.82f,
    totalReps = 45,
    goodReps = 37,
    totalSets = 6,
    totalExercises = 3,
    totalDurationMs = 2_340_000L
)

private val sampleWeeklyProgress = WeeklyProgress(
    volumeData = listOf(75f, 82f, 68f, 88f, 79f, 92f, 100f),
    changePercent = "+83 avg/day"
)

private val sampleRecentActivity = listOf(
    ActivityItem(
        sessionId = "session-1",
        exercise = "Bench Press",
        weight = "100 kg",
        reps = "4/5 good reps",
        grade = "B",
        duration = "3m 24s",
        timeAgo = "12 min ago"
    ),
    ActivityItem(
        sessionId = "session-2",
        exercise = "Squat",
        weight = "140 kg",
        reps = "3/3 good reps",
        grade = "A",
        duration = "2m 48s",
        timeAgo = "32 min ago"
    )
)

private val sampleDeviceStatusConnected = DeviceStatus(
    isConnected = true,
    deviceName = "LIFTRR-001",
    batteryPercent = 87
)

private val sampleDeviceStatusDisconnected = DeviceStatus(
    isConnected = false,
    deviceName = null,
    batteryPercent = 0
)

@Preview(showBackground = true, name = "Empty State")
@Composable
private fun HomeScreenEmptyPreview() {
    LiftrrTheme {
        HomeScreenContent(
            uiState = HomeUiState.Success(
                todayStats = null,
                weeklyProgress = null,
                recentActivity = emptyList(),
                deviceStatus = sampleDeviceStatusDisconnected
            ),
            onStartWorkout = {}
        )
    }
}

@Preview(showBackground = true, name = "With Data")
@Composable
private fun HomeScreenWithDataPreview() {
    LiftrrTheme {
        HomeScreenContent(
            uiState = HomeUiState.Success(
                todayStats = sampleTodayStats,
                weeklyProgress = sampleWeeklyProgress,
                recentActivity = sampleRecentActivity,
                deviceStatus = sampleDeviceStatusConnected
            ),
            onStartWorkout = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun HomeScreenLoadingPreview() {
    LiftrrTheme {
        HomeScreenContent(
            uiState = HomeUiState.Loading,
            onStartWorkout = {}
        )
    }
}
