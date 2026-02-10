package org.liftrr.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import org.liftrr.ml.ExerciseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AnalyticsUiState.Success -> {
                if (state.overview.totalWorkouts == 0) {
                    EmptyAnalyticsState(
                        selectedTimeRange = state.selectedTimeRange,
                        onTimeRangeSelected = viewModel::setTimeRange,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    AnalyticsContent(
                        state = state,
                        onTimeRangeSelected = viewModel::setTimeRange,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Success,
    onTimeRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TimeRangeSelector(
            selectedRange = state.selectedTimeRange,
            onRangeSelected = onTimeRangeSelected
        )

        OverviewCards(overview = state.overview)

        if (state.formQualityTrend.size >= 2) {
            FormQualityTrendCard(trend = state.formQualityTrend)
        }

        if (state.volumeTrend.size >= 2) {
            VolumeTrendCard(trend = state.volumeTrend)
        }

        if (state.exerciseDistribution.isNotEmpty()) {
            ExerciseDistributionCard(distribution = state.exerciseDistribution)
        }

        if (state.gradeDistribution.values.any { it > 0 }) {
            GradeDistributionCard(distribution = state.gradeDistribution)
        }

        if (state.personalRecords.isNotEmpty()) {
            PersonalRecordsSection(records = state.personalRecords)
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ─── Time Range Selector ────────────────────────────────────────────────────

@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TimeRange.entries) { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

// ─── Overview Cards ─────────────────────────────────────────────────────────

@Composable
private fun OverviewCards(overview: OverviewStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewMetricCard(
            value = overview.totalWorkouts.toString(),
            label = "Workouts",
            icon = Icons.Outlined.FitnessCenter,
            modifier = Modifier.weight(1f)
        )
        OverviewMetricCard(
            value = overview.totalReps.toString(),
            label = "Total Reps",
            icon = Icons.Outlined.Repeat,
            modifier = Modifier.weight(1f)
        )
        OverviewMetricCard(
            value = "${(overview.avgFormQuality * 100).toInt()}%",
            label = "Avg Quality",
            icon = Icons.Outlined.Speed,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewMetricCard(
            value = "${overview.totalVolume.toInt()}",
            label = "Volume (${overview.volumeUnit})",
            icon = Icons.Outlined.FitnessCenter,
            modifier = Modifier.weight(1f)
        )
        OverviewMetricCard(
            value = overview.bestGrade ?: "–",
            label = "Best Grade",
            icon = Icons.Outlined.Star,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverviewMetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ─── Form Quality Trend ─────────────────────────────────────────────────────

@Composable
private fun FormQualityTrendCard(trend: List<FormQualityPoint>) {
    SectionCard(title = "Form Quality Trend", subtitle = "Average per workout") {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            data = listOf(
                Line(
                    label = "Quality",
                    values = trend.map { (it.quality * 100).toDouble() },
                    color = SolidColor(MaterialTheme.colorScheme.primary),
                    firstGradientFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    secondGradientFillColor = Color.Transparent,
                    drawStyle = DrawStyle.Stroke(width = 3.dp),
                    curvedEdges = true
                )
            ),
            animationMode = AnimationMode.Together(delayBuilder = { it * 100L })
        )
    }
}

// ─── Volume Trend ───────────────────────────────────────────────────────────

@Composable
private fun VolumeTrendCard(trend: List<VolumeTrendPoint>) {
    val primary = MaterialTheme.colorScheme.primary

    SectionCard(title = "Volume Trend", subtitle = "Per day") {
        ColumnChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            data = trend.map { point ->
                Bars(
                    label = point.label,
                    values = listOf(
                        Bars.Data(
                            label = "Volume",
                            value = point.volume.toDouble(),
                            color = if (point.isHighlighted) SolidColor(primary)
                            else SolidColor(primary.copy(alpha = 0.5f))
                        )
                    )
                )
            },
            animationMode = AnimationMode.Together(delayBuilder = { it * 100L })
        )
    }
}

// ─── Exercise Distribution ──────────────────────────────────────────────────

@Composable
private fun ExerciseDistributionCard(distribution: List<ExerciseDistributionItem>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )

    SectionCard(title = "Exercise Distribution", subtitle = "Workout breakdown") {
        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            distribution.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .weight(item.percentage.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(colors[index % colors.size])
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Legend
        distribution.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors[index % colors.size])
                    )
                    Text(
                        item.exerciseType.displayName(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    "${item.count} (${(item.percentage * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Grade Distribution ─────────────────────────────────────────────────────

@Composable
private fun GradeDistributionCard(distribution: Map<String, Int>) {
    val maxCount = distribution.values.maxOrNull()?.toFloat() ?: 1f

    SectionCard(title = "Grade Distribution", subtitle = "Form scores") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            distribution.forEach { (grade, count) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    if (count > 0) {
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    val barHeight = if (maxCount > 0) (count / maxCount * 80f).coerceAtLeast(4f) else 4f
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(getGradeColor(grade))
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        grade,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = getGradeColor(grade)
                    )
                }
            }
        }
    }
}

// ─── Personal Records ───────────────────────────────────────────────────────

@Composable
private fun PersonalRecordsSection(records: List<PersonalRecord>) {
    Text(
        "Personal Records",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    records.forEach { record ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        record.exerciseType.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RecordStat(
                        value = if (record.heaviestWeight != null) "${record.heaviestWeight.toInt()} kg" else "–",
                        label = "Heaviest"
                    )
                    RecordStat(
                        value = record.mostReps.toString(),
                        label = "Most Reps"
                    )
                    RecordStat(
                        value = "${record.bestScore.toInt()}%",
                        label = "Best Score"
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Empty State ────────────────────────────────────────────────────────────

@Composable
private fun EmptyAnalyticsState(
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        TimeRangeSelector(selectedRange = selectedTimeRange, onRangeSelected = onTimeRangeSelected)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No Data Yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Complete some workouts to see\nyour analytics and trends here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Shared Components ──────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            content()
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
