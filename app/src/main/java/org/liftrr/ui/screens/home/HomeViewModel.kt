package org.liftrr.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.liftrr.data.models.WorkoutSessionEntity
import org.liftrr.data.repository.WorkoutRepository
import org.liftrr.ml.ExerciseType
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for Home Screen
 * Manages workout data and device status
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRealData()
    }

    private fun loadRealData() {
        viewModelScope.launch {
            combine(
                workoutRepository.getTodayWorkouts(),
                workoutRepository.getWeekWorkouts(),
                workoutRepository.getRecentWorkouts(5)
            ) { todayWorkouts, weekWorkouts, recentWorkouts ->
                val todayStats = if (todayWorkouts.isNotEmpty()) {
                    calculateTodayStats(todayWorkouts)
                } else null

                val weeklyProgress = calculateWeeklyProgress(weekWorkouts)

                val recentActivity = recentWorkouts.map { workout ->
                    mapToActivityItem(workout)
                }

                HomeUiState.Success(
                    todayStats = todayStats,
                    weeklyProgress = weeklyProgress,
                    recentActivity = recentActivity,
                    deviceStatus = DeviceStatus(
                        isConnected = false,
                        deviceName = null,
                        batteryPercent = 0
                    )
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun calculateTodayStats(workouts: List<WorkoutSessionEntity>): TodayStats {
        val totalReps = workouts.sumOf { it.totalReps }
        val goodReps = workouts.sumOf { it.goodReps }
        val avgQuality = workouts.map { it.averageQuality }.average().toFloat()
        val totalSets = workouts.size
        val exerciseCount = workouts.map { it.exerciseType }.distinct().size
        val totalDurationMs = workouts.sumOf { it.durationMs }

        val workoutsWithWeight = workouts.filter { it.weight != null && it.weight > 0 }
        val hasWeight = workoutsWithWeight.isNotEmpty()

        val volume: Float
        val volumeUnit: String
        val volumeLabel: String

        if (hasWeight) {
            volume = workoutsWithWeight.sumOf { (it.weight ?: 0f).toDouble() * it.totalReps }.toFloat()
            volumeUnit = "kg"
            volumeLabel = "Volume"
        } else {
            volume = totalReps.toFloat()
            volumeUnit = "reps"
            volumeLabel = "Total Reps"
        }

        return TodayStats(
            volume = volume,
            volumeUnit = volumeUnit,
            volumeLabel = volumeLabel,
            formQuality = avgQuality,
            totalReps = totalReps,
            goodReps = goodReps,
            totalSets = totalSets,
            totalExercises = exerciseCount,
            totalDurationMs = totalDurationMs
        )
    }


    private fun calculateWeeklyProgress(weekWorkouts: List<WorkoutSessionEntity>): WeeklyProgress? {
        if (weekWorkouts.isEmpty()) return null

        val today = Calendar.getInstance()
        val dayOfWeek = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0..Sun=6

        // Build daily volumes for Mon-Sun
        val dailyVolumes = FloatArray(7)
        for (workout in weekWorkouts) {
            val cal = Calendar.getInstance().apply { timeInMillis = workout.timestamp }
            val idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val volume = if (workout.weight != null && workout.weight > 0) {
                workout.weight * workout.totalReps
            } else {
                workout.totalReps.toFloat()
            }
            dailyVolumes[idx] += volume
        }

        // Calculate change: this week so far vs. same period concept (first half vs second half if enough data)
        val totalVolume = dailyVolumes.sum()
        val activeDays = dailyVolumes.count { it > 0f }
        val changePercent = if (activeDays > 1) {
            val avgPerDay = totalVolume / activeDays
            "+${avgPerDay.toInt()} avg/day"
        } else {
            "$activeDays day${if (activeDays != 1) "s" else ""}"
        }

        return WeeklyProgress(
            volumeData = dailyVolumes.toList(),
            changePercent = changePercent
        )
    }

    private fun mapToActivityItem(workout: WorkoutSessionEntity): ActivityItem {
        val weightStr = if (workout.weight != null && workout.weight > 0) {
            "${workout.weight.toInt()} kg"
        } else {
            "Bodyweight"
        }
        val durationMin = TimeUnit.MILLISECONDS.toMinutes(workout.durationMs)
        val durationSec = TimeUnit.MILLISECONDS.toSeconds(workout.durationMs) % 60
        val durationStr = if (durationMin > 0) "${durationMin}m ${durationSec}s" else "${durationSec}s"

        return ActivityItem(
            sessionId = workout.sessionId,
            exercise = ExerciseType.valueOf(workout.exerciseType).displayName(),
            weight = weightStr,
            reps = "${workout.goodReps}/${workout.totalReps} good reps",
            grade = workout.grade,
            duration = durationStr,
            timeAgo = getTimeAgo(workout.timestamp)
        )
    }


    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            days == 1L -> "Yesterday"
            else -> "$days days ago"
        }
    }

    fun startWorkout() {
        // TODO: Navigate to workout screen
    }
}

// UI State
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val todayStats: TodayStats?,
        val weeklyProgress: WeeklyProgress?,
        val recentActivity: List<ActivityItem>,
        val deviceStatus: DeviceStatus
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

// Data models
data class TodayStats(
    val volume: Float,
    val volumeUnit: String,
    val volumeLabel: String,
    val formQuality: Float,   // 0.0 - 1.0
    val totalReps: Int,
    val goodReps: Int,
    val totalSets: Int,
    val totalExercises: Int,
    val totalDurationMs: Long
)

data class WeeklyProgress(
    val volumeData: List<Float>,
    val changePercent: String
)

data class ActivityItem(
    val sessionId: String,
    val exercise: String,
    val weight: String,
    val reps: String,
    val grade: String,
    val duration: String,
    val timeAgo: String
)

data class DeviceStatus(
    val isConnected: Boolean,
    val deviceName: String?,
    val batteryPercent: Int
)
