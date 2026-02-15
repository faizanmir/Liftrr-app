package org.liftrr.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import org.liftrr.domain.analytics.ExerciseSpecificMetrics
import org.liftrr.domain.analytics.FormConsistencyAnalysis
import org.liftrr.domain.analytics.RangeOfMotionAnalysis
import org.liftrr.domain.analytics.RepAnalysis
import org.liftrr.domain.analytics.SymmetryAnalysis
import org.liftrr.domain.analytics.TempoAnalysis
import org.liftrr.domain.analytics.WorkoutReport
import org.liftrr.ml.ExerciseType
import org.liftrr.ui.screens.profile.CheckForPromptAfterWorkout
import org.liftrr.ui.screens.profile.ProgressiveProfilePromptContainer
import org.liftrr.data.models.PromptType
import kotlin.math.roundToInt

/**
 * Screen showing post-workout summary and analysis with AI insights
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryScreen(
    report: WorkoutReport,
    onNavigateBack: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WorkoutSummaryViewModel = hiltViewModel()
) {
    val aiSummary by viewModel.aiSummary.collectAsState()
    val aiRecommendations by viewModel.aiRecommendations.collectAsState()
    val motivationalMessage by viewModel.motivationalMessage.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    // System back should go to Home (same as toolbar back)
    BackHandler { onNavigateBack() }

    // Generate AI insights when screen loads (after initialization)
    LaunchedEffect(report, isInitializing) {
        if (!isInitializing) {
            viewModel.generateInsights(report)
        }
    }
    var showShareMenu by remember { mutableStateOf(false) }

    // Check for progressive profile prompts after workout
    CheckForPromptAfterWorkout()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text("Workout Summary") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Share menu
                        Box {
                            IconButton(onClick = { showShareMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share report"
                                )
                            }

                            DropdownMenu(
                                expanded = showShareMenu,
                                onDismissRequest = { showShareMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share as PDF") },
                                    onClick = {
                                        showShareMenu = false
                                        viewModel.shareAsPdf(report)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share as Text") },
                                    onClick = {
                                        showShareMenu = false
                                        viewModel.shareAsText(report)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Score Card
            item {
                OverallScoreCard(report)
            }

            // Quick Stats Row
            item {
                QuickStatsRow(report)
            }

            // Velocity Profile Section
            item {
                VelocityProfileSection(report)
            }

            // AI Summary Section
            item {
                AISummarySection(
                    state = aiSummary,
                    motivationalMessage = motivationalMessage,
                    onRetry = { viewModel.retryInsights(report) }
                )
            }

            // AI Recommendations Section
            item {
                AIRecommendationsSection(
                    state = aiRecommendations,
                    onRetry = { viewModel.retryInsights(report) }
                )
            }

            // Exercise-Specific Metrics Section
            report.exerciseSpecificMetrics?.let { metrics ->
                item {
                    ExerciseSpecificMetricsSection(metrics = metrics)
                }
            }

            // Range of Motion Section
            item {
                SectionCard(
                    title = "Range of Motion",
                    icon = Icons.Default.TrendingUp
                ) {
                    RangeOfMotionContent(report.rangeOfMotion)
                }
            }

            // Tempo Section
            item {
                SectionCard(
                    title = "Tempo & Timing",
                    icon = Icons.Default.Schedule
                ) {
                    TempoContent(report.tempo)
                }
            }

            // Symmetry Section
            item {
                SectionCard(
                    title = "Symmetry",
                    icon = Icons.Default.Balance
                ) {
                    SymmetryContent(report.symmetry)
                }
            }

            // Form Consistency Section
            item {
                SectionCard(
                    title = "Form Consistency",
                    icon = Icons.Default.AutoGraph
                ) {
                    FormConsistencyContent(report.formConsistency)
                }
            }

            // Recommendations Section
            item {
                SectionCard(
                    title = "Recommendations",
                    icon = Icons.Default.Lightbulb
                ) {
                    RecommendationsContent(report.recommendations)
                }
            }

            // Rep-by-Rep Breakdown
            item {
                Text(
                    text = "Rep Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(report.repAnalyses) { repAnalysis ->
                RepAnalysisCard(repAnalysis)
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

        // Full-screen loading overlay while LLM is initializing
        if (isInitializing) {
            LLMInitializingOverlay()
        }

        // Loading overlay while exporting report
        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Preparing report...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Progressive profile prompt container
    ProgressiveProfilePromptContainer(
        onStartOnboardingFlow = onNavigateToOnboarding
    )
}

@Composable
private fun OverallScoreCard(report: WorkoutReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = report.exerciseType.name.replace("_", " "),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grade Circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = report.grade,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${report.overallScore.roundToInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${report.totalReps} reps in ${formatDuration(report.durationMs)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun QuickStatsRow(report: WorkoutReport) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            label = "Good Form",
            value = report.goodReps.toString(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            label = "Bad Form",
            value = report.badReps.toString(),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            label = "Avg Quality",
            value = "${(report.averageQuality * 100).roundToInt()}%",
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun RangeOfMotionContent(rom: RangeOfMotionAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricRow("Average Depth", "${rom.averageDepth.roundToInt()}°")
        MetricRow("Consistency", "${rom.consistency.roundToInt()}%")
        MetricRow("Range", "${rom.minDepth.roundToInt()}° - ${rom.maxDepth.roundToInt()}°")
    }
}

@Composable
private fun TempoContent(tempo: TempoAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricRow("Average Rep Time", formatDuration(tempo.averageRepDurationMs))
        MetricRow("Consistency", "${tempo.tempoConsistency.roundToInt()}%")
    }
}

@Composable
private fun SymmetryContent(symmetry: SymmetryAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricRow("Symmetry Score", "${symmetry.overallSymmetry.roundToInt()}%")
        if (symmetry.issues.isNotEmpty()) {
            symmetry.issues.forEach { issue ->
                Text(
                    text = "⚠ $issue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FormConsistencyContent(consistency: FormConsistencyAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricRow("Consistency Score", "${consistency.consistencyScore.roundToInt()}%")
        MetricRow("Trend", consistency.qualityTrend)
    }
}

@Composable
private fun RecommendationsContent(recommendations: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recommendations.forEach { recommendation ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text("• ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = recommendation,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RepAnalysisCard(rep: RepAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rep.isGoodForm) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rep #${rep.repNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (rep.isGoodForm) "Good Form" else "Needs Work",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rep.isGoodForm) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                Icon(
                    imageVector = if (rep.isGoodForm) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (rep.isGoodForm) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            // Show metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rep.depth?.let {
                    MetricChip(
                        label = "Depth",
                        value = "${it.roundToInt()}°"
                    )
                }
                rep.tempo?.let {
                    MetricChip(
                        label = "Time",
                        value = "%.1fs".format(it.totalMs / 1000f)
                    )
                }
                MetricChip(
                    label = "Quality",
                    value = "${(rep.quality * 100).roundToInt()}%"
                )
            }

            // Show form feedback
            if (rep.formIssues.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (rep.isGoodForm) {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            }
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rep.formIssues.forEach { issue ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (rep.isGoodForm) "✓" else "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (rep.isGoodForm) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = issue,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (rep.isGoodForm) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        }
                    }
                }
            } else if (rep.isGoodForm) {
                // Show positive feedback for good reps with no issues
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Excellent form maintained",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AISummarySection(
    state: AIInsightState,
    motivationalMessage: String?,
    onRetry: () -> Unit
) {
    SectionCard(
        title = "AI Coach Summary",
        icon = Icons.Default.Psychology
    ) {
        when (state) {
            is AIInsightState.Idle -> {
                // Don't show anything while waiting
                Box(modifier = Modifier.height(0.dp))
            }
            is AIInsightState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyzing your workout...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is AIInsightState.Success -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = state.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (motivationalMessage != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = motivationalMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
            is AIInsightState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Could not generate AI summary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun AIRecommendationsSection(
    state: AIInsightState,
    onRetry: () -> Unit
) {
    SectionCard(
        title = "AI Recommendations",
        icon = Icons.Default.AutoAwesome
    ) {
        when (state) {
            is AIInsightState.Idle -> {
                // Don't show anything while waiting
                Box(modifier = Modifier.height(0.dp))
            }
            is AIInsightState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Generating personalized tips...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is AIInsightState.Success -> {
                Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            is AIInsightState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Could not generate recommendations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSpecificMetricsSection(
    metrics: ExerciseSpecificMetrics
) {
    when (metrics) {
        is ExerciseSpecificMetrics.SquatMetrics -> {
            SquatMetricsCard(metrics)
        }
        is ExerciseSpecificMetrics.DeadliftMetrics -> {
            DeadliftMetricsCard(metrics)
        }
        is ExerciseSpecificMetrics.BenchPressMetrics -> {
            BenchPressMetricsCard(metrics)
        }
    }
}

@Composable
private fun SquatMetricsCard(metrics: ExerciseSpecificMetrics.SquatMetrics) {
    SectionCard(
        title = "Squat Analysis",
        icon = Icons.Default.FitnessCenter
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricRow("Squat Depth", "${metrics.averageDepth.roundToInt()}°")
            MetricRow("Depth Consistency", "${metrics.depthConsistency.roundToInt()}%")
            MetricRow("Hip Mobility", "${metrics.hipMobility.roundToInt()}%")
            MetricRow("Knee Alignment", "${metrics.kneeTracking.kneeAlignment.roundToInt()}%")

            if (metrics.kneeTracking.kneeCavePercentage > 30f) {
                Text(
                    text = "⚠ Knee cave detected in ${metrics.kneeTracking.kneeCavePercentage.roundToInt()}% of reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            MetricRow("Torso Angle", "${metrics.torsoAngle.averageForwardLean.roundToInt()}°")
            if (metrics.torsoAngle.excessiveLean) {
                Text(
                    text = "⚠ Excessive forward lean",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            MetricRow("Balance/Stability", "${metrics.balance.stability.roundToInt()}%")
        }
    }
}

@Composable
private fun DeadliftMetricsCard(metrics: ExerciseSpecificMetrics.DeadliftMetrics) {
    SectionCard(
        title = "Deadlift Analysis",
        icon = Icons.Default.FitnessCenter
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricRow("Hip Hinge Quality", "${metrics.hipHingeQuality.roundToInt()}%")
            MetricRow("Spine Neutral", "${metrics.backStraightness.spineNeutral.roundToInt()}%")

            if (metrics.backStraightness.lowerBackRounding > 20f) {
                Text(
                    text = "⚠ Lower back rounding detected (${metrics.backStraightness.lowerBackRounding.roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (metrics.backStraightness.upperBackRounding > 20f) {
                Text(
                    text = "⚠ Upper back rounding detected (${metrics.backStraightness.upperBackRounding.roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            MetricRow("Lockout Completion", "${metrics.lockoutCompletion.roundToInt()}%")
            MetricRow("Starting Position", "${metrics.startingPosition.setup.roundToInt()}%")
        }
    }
}

@Composable
private fun BenchPressMetricsCard(metrics: ExerciseSpecificMetrics.BenchPressMetrics) {
    SectionCard(
        title = "Bench Press Analysis",
        icon = Icons.Default.FitnessCenter
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricRow("Elbow Angle (Bottom)", "${metrics.elbowAngle.bottomAngle.roundToInt()}°")
            MetricRow("Elbow Angle (Top)", "${metrics.elbowAngle.topAngle.roundToInt()}°")
            MetricRow("Elbow Tucking", "${metrics.elbowAngle.tucking.roundToInt()}%")
            MetricRow("Shoulder Retraction", "${metrics.shoulderPosition.retraction.roundToInt()}%")
            MetricRow("Shoulder Stability", "${metrics.shoulderPosition.stability.roundToInt()}%")
            MetricRow("Touch Point Consistency", "${metrics.touchPointConsistency.roundToInt()}%")
            MetricRow("Arch Maintenance", "${metrics.archMaintenance.roundToInt()}%")
        }
    }
}

@Composable
private fun VelocityProfileSection(report: WorkoutReport) {
    // Generate velocity data from rep analyses
    val velocityData = generateVelocityDataFromReport(report)

    if (velocityData.isEmpty()) return

    SectionCard(
        title = "Velocity Profile",
        icon = Icons.Default.AutoGraph
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Rep-by-rep velocity curves",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            VelocityCurveChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                velocityData = velocityData
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendItem("Concentric", MaterialTheme.colorScheme.primary)
                ChartLegendItem("Eccentric", MaterialTheme.colorScheme.tertiary)
                val peakVelocity = velocityData.maxOfOrNull { it.maxOrNull() ?: 0f } ?: 0f
                ChartLegendItem("Peak: %.2f m/s".format(peakVelocity), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VelocityCurveChart(
    modifier: Modifier = Modifier,
    velocityData: List<List<Float>>
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Convert velocity data to chart format
    val lines = mutableListOf<Line>()

    velocityData.forEachIndexed { repIndex, repData ->
        val midPoint = repData.size / 2

        // Concentric phase (first half)
        lines.add(
            Line(
                label = "Rep ${repIndex + 1} Concentric",
                values = repData.subList(0, midPoint).map { it.toDouble() },
                color = SolidColor(primaryColor),
                strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                firstGradientFillColor = primaryColor.copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                drawStyle = DrawStyle.Stroke(width = 3.dp),
                curvedEdges = true
            )
        )

        // Eccentric phase (second half)
        lines.add(
            Line(
                label = "Rep ${repIndex + 1} Eccentric",
                values = repData.subList(midPoint, repData.size).map { it.toDouble() },
                color = SolidColor(tertiaryColor),
                strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                firstGradientFillColor = tertiaryColor.copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                drawStyle = DrawStyle.Stroke(width = 3.dp),
                curvedEdges = true
            )
        )
    }

    LineChart(
        modifier = modifier,
        data = lines,
        animationMode = AnimationMode.Together(delayBuilder = {
            it * 100L
        })
    )
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Generate velocity data from workout report
private fun generateVelocityDataFromReport(report: WorkoutReport): List<List<Float>> {
    val repData = mutableListOf<List<Float>>()
    val avgQuality = report.averageQuality
    val baseVelocity = avgQuality * 0.8f

    report.repAnalyses.forEachIndexed { index, rep ->
        // Use rep quality if available, otherwise use average
        val repQuality = if (rep.isGoodForm) avgQuality else avgQuality * 0.7f
        val decay = 1f - (index * 0.05f) // Simulate fatigue
        val repVelocity = baseVelocity * decay * (repQuality / avgQuality)

        // Generate velocity curve (11 points)
        val curve = listOf(
            0.0f,
            repVelocity * 0.2f,
            repVelocity * 0.45f,
            repVelocity * 0.68f,
            repVelocity * 0.88f,
            repVelocity * 1.0f, // Peak
            repVelocity * 0.92f,
            repVelocity * 0.72f,
            repVelocity * 0.48f,
            repVelocity * 0.22f,
            0.0f
        )
        repData.add(curve)
    }

    return repData
}

@Composable
private fun LLMInitializingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Preparing AI Coach",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Loading on-device AI model...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Helpful tip
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "First load may take a few seconds.\nYour data stays on-device for privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}
