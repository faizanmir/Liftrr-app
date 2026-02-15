package org.liftrr.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.liftrr.data.models.WorkoutSessionEntity
import org.liftrr.data.repository.WorkoutRepository
import org.liftrr.ml.ExerciseType
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

enum class TimeRange(val label: String) {
    WEEK("Week"), MONTH("Month"), ALL_TIME("All Time")
}

sealed class AnalyticsUiState {
    data object Loading : AnalyticsUiState()
    data class Success(
        val selectedTimeRange: TimeRange,
        val overview: OverviewStats,
        val formQualityTrend: List<FormQualityPoint>,
        val volumeTrend: List<VolumeTrendPoint>,
        val exerciseDistribution: List<ExerciseDistributionItem>,
        val gradeDistribution: Map<String, Int>,
        val personalRecords: List<PersonalRecord>
    ) : AnalyticsUiState()
}

data class OverviewStats(
    val totalWorkouts: Int,
    val totalReps: Int,
    val avgFormQuality: Float,
    val totalVolume: Float,
    val volumeUnit: String,
    val bestGrade: String?
)

data class FormQualityPoint(
    val timestamp: Long, val quality: Float, val label: String
)

data class VolumeTrendPoint(
    val label: String, val volume: Float, val isHighlighted: Boolean
)

data class ExerciseDistributionItem(
    val exerciseType: ExerciseType, val count: Int, val percentage: Float
)

data class PersonalRecord(
    val exerciseType: ExerciseType,
    val heaviestWeight: Float?,
    val mostReps: Int,
    val bestScore: Float
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)

    val uiState: StateFlow<AnalyticsUiState> = _selectedTimeRange.flatMapLatest { range ->
            when (range) {
                TimeRange.WEEK -> workoutRepository.getWeekWorkouts()
                TimeRange.MONTH -> workoutRepository.getMonthWorkouts()
                TimeRange.ALL_TIME -> workoutRepository.getAllWorkouts()
            }.map { workouts -> buildState(workouts, range) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState.Loading)

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }

    private fun buildState(
        workouts: List<WorkoutSessionEntity>, range: TimeRange
    ): AnalyticsUiState.Success {
        return AnalyticsUiState.Success(
            selectedTimeRange = range,
            overview = calculateOverview(workouts),
            formQualityTrend = calculateFormQualityTrend(workouts, range),
            volumeTrend = calculateVolumeTrend(workouts, range),
            exerciseDistribution = calculateExerciseDistribution(workouts),
            gradeDistribution = calculateGradeDistribution(workouts),
            personalRecords = calculatePersonalRecords(workouts)
        )
    }

    private fun calculateOverview(workouts: List<WorkoutSessionEntity>): OverviewStats {
        if (workouts.isEmpty()) {
            return OverviewStats(0, 0, 0f, 0f, "reps", null)
        }
        val totalReps = workouts.sumOf { it.totalReps }
        val avgQuality = workouts.map { it.averageQuality }.average().toFloat()
        val withWeight = workouts.filter { it.weight != null && it.weight > 0 }
        val hasWeight = withWeight.isNotEmpty()

        val volume: Float
        val unit: String
        if (hasWeight) {
            volume = withWeight.sumOf { (it.weight ?: 0f).toDouble() * it.totalReps }.toFloat()
            unit = "kg"
        } else {
            volume = totalReps.toFloat()
            unit = "reps"
        }

        val bestGrade = workouts.minByOrNull { gradeToScore(it.grade) }?.grade

        return OverviewStats(
            totalWorkouts = workouts.size,
            totalReps = totalReps,
            avgFormQuality = avgQuality,
            totalVolume = volume,
            volumeUnit = unit,
            bestGrade = bestGrade
        )
    }

    private fun calculateFormQualityTrend(
        workouts: List<WorkoutSessionEntity>, range: TimeRange
    ): List<FormQualityPoint> {
        val sorted = workouts.sortedBy { it.timestamp }
        val dateFormat = when (range) {
            TimeRange.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
            TimeRange.MONTH -> SimpleDateFormat("MMM d", Locale.getDefault())
            TimeRange.ALL_TIME -> SimpleDateFormat("MMM d", Locale.getDefault())
        }
        return sorted.map { workout ->
            FormQualityPoint(
                timestamp = workout.timestamp,
                quality = workout.averageQuality,
                label = dateFormat.format(workout.timestamp)
            )
        }
    }

    private fun calculateVolumeTrend(
        workouts: List<WorkoutSessionEntity>, range: TimeRange
    ): List<VolumeTrendPoint> {
        if (workouts.isEmpty()) return emptyList()

        val dateFormat = when (range) {
            TimeRange.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
            TimeRange.MONTH -> SimpleDateFormat("MMM d", Locale.getDefault())
            TimeRange.ALL_TIME -> SimpleDateFormat("MMM d", Locale.getDefault())
        }

        val dayFormat = SimpleDateFormat("yyyyDDD", Locale.getDefault())
        val grouped = workouts.groupBy { dayFormat.format(it.timestamp) }.toSortedMap()

        val points = grouped.map { (_, dayWorkouts) ->
            val volume = dayWorkouts.sumOf { workout ->
                if (workout.weight != null && workout.weight > 0) {
                    (workout.weight * workout.totalReps).toDouble()
                } else {
                    workout.totalReps.toDouble()
                }
            }.toFloat()
            val label = dateFormat.format(dayWorkouts.first().timestamp)
            VolumeTrendPoint(label = label, volume = volume, isHighlighted = false)
        }

        return if (points.isNotEmpty()) {
            points.mapIndexed { index, point ->
                point.copy(isHighlighted = index == points.lastIndex)
            }
        } else points
    }

    private fun calculateExerciseDistribution(
        workouts: List<WorkoutSessionEntity>
    ): List<ExerciseDistributionItem> {
        if (workouts.isEmpty()) return emptyList()
        val total = workouts.size.toFloat()
        return workouts.groupBy { it.exerciseType }.map { (type, list) ->
                ExerciseDistributionItem(
                    exerciseType = ExerciseType.valueOf(type),
                    count = list.size,
                    percentage = list.size / total
                )
            }.sortedByDescending { it.count }
    }

    private fun calculateGradeDistribution(
        workouts: List<WorkoutSessionEntity>
    ): Map<String, Int> {
        val counts = workouts.groupingBy { it.grade.uppercase() }.eachCount()
        val ordered = linkedMapOf<String, Int>()
        listOf("A", "B", "C", "D", "F").forEach { grade ->
            ordered[grade] = counts[grade] ?: 0
        }
        return ordered
    }

    private fun calculatePersonalRecords(
        workouts: List<WorkoutSessionEntity>
    ): List<PersonalRecord> {
        return workouts.groupBy { it.exerciseType }.mapNotNull { (type, list) ->
                try {
                    PersonalRecord(
                        exerciseType = ExerciseType.valueOf(type),
                        heaviestWeight = list.mapNotNull { it.weight }.filter { it > 0 }
                            .maxOrNull(),
                        mostReps = list.maxOf { it.totalReps },
                        bestScore = list.maxOf { it.overallScore })
                } catch (_: Exception) {
                    null
                }
            }.sortedBy { it.exerciseType.ordinal }
    }

    private fun gradeToScore(grade: String): Int = when (grade.uppercase()) {
        "A" -> 1; "B" -> 2; "C" -> 3; "D" -> 4; else -> 5
    }
}
