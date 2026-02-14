package org.liftrr.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.liftrr.data.models.WorkoutSessionEntity
import org.liftrr.data.repository.WorkoutRepository
import org.liftrr.domain.workmanager.WorkoutCleanupScheduler
import org.liftrr.ml.ExerciseType
import org.liftrr.utils.DispatcherProvider
import java.util.Calendar
import javax.inject.Inject

enum class ExerciseFilter(val label: String) {
    ALL("All"),
    SQUAT("Squat"),
    DEADLIFT("Deadlift"),
    BENCH_PRESS("Bench Press")
}

enum class DateGroup(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    EARLIER("Earlier")
}

data class HistoryWorkoutItem(
    val sessionId: String,
    val exerciseType: ExerciseType,
    val totalReps: Int,
    val goodReps: Int,
    val badReps: Int,
    val grade: String,
    val overallScore: Float,
    val weight: Float?,
    val durationMs: Long,
    val timestamp: Long
)

sealed class HistoryListItem {
    data class Header(val group: DateGroup) : HistoryListItem()
    data class WorkoutCard(val item: HistoryWorkoutItem) : HistoryListItem()
}

sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class Success(
        val items: List<HistoryListItem>,
        val selectedFilter: ExerciseFilter,
        val isEmpty: Boolean
    ) : HistoryUiState()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val dispatchers: DispatcherProvider,
    private val cleanupScheduler: WorkoutCleanupScheduler
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(ExerciseFilter.ALL)

    private val _showUndoSnackbar = MutableStateFlow<String?>(null)
    val showUndoSnackbar: StateFlow<String?> = _showUndoSnackbar.asStateFlow()

    private var pendingDeletionSessionId: String? = null

    val uiState: StateFlow<HistoryUiState> = combine(
        workoutRepository.getAllWorkouts(),
        _selectedFilter
    ) { workouts, filter ->
        val filtered = when (filter) {
            ExerciseFilter.ALL -> workouts
            else -> workouts.filter { it.exerciseType == filter.name }
        }
        val items = groupByDate(filtered)
        HistoryUiState.Success(
            items = items,
            selectedFilter = filter,
            isEmpty = filtered.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState.Loading)

    fun setFilter(filter: ExerciseFilter) {
        _selectedFilter.value = filter
    }

    fun deleteWorkout(item: HistoryWorkoutItem) {
        viewModelScope.launch(dispatchers.io) {
            // Mark workout as deleted (soft delete)
            workoutRepository.markWorkoutAsDeleted(item.sessionId)

            // Store the session ID for potential undo
            pendingDeletionSessionId = item.sessionId

            // Show snackbar
            _showUndoSnackbar.value = "Workout deleted"
        }
    }

    fun undoDelete() {
        val sessionId = pendingDeletionSessionId ?: return

        viewModelScope.launch(dispatchers.io) {
            // Restore the workout (unmark as deleted)
            workoutRepository.restoreWorkout(sessionId)
            // Note: We don't cancel the cleanup job here.
            // The job will run and query for isDeleted=1 workouts.
            // Since this workout is now isDeleted=0, it won't be cleaned up.
        }

        pendingDeletionSessionId = null
        _showUndoSnackbar.value = null
    }

    fun dismissSnackbar() {
        // When snackbar is dismissed, schedule WorkManager to clean up deleted workouts
        if (pendingDeletionSessionId != null) {
            cleanupScheduler.scheduleCleanup()
        }

        pendingDeletionSessionId = null
        _showUndoSnackbar.value = null
    }

    private fun groupByDate(workouts: List<WorkoutSessionEntity>): List<HistoryListItem> {
        if (workouts.isEmpty()) return emptyList()

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterdayStart = todayStart - 86_400_000L
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis

        val grouped = workouts
            .map { entity ->
                val group = when {
                    entity.timestamp >= todayStart -> DateGroup.TODAY
                    entity.timestamp >= yesterdayStart -> DateGroup.YESTERDAY
                    entity.timestamp >= weekStart -> DateGroup.THIS_WEEK
                    else -> DateGroup.EARLIER
                }
                group to HistoryWorkoutItem(
                    sessionId = entity.sessionId,
                    exerciseType = ExerciseType.valueOf(entity.exerciseType),
                    totalReps = entity.totalReps,
                    goodReps = entity.goodReps,
                    badReps = entity.badReps,
                    grade = entity.grade,
                    overallScore = entity.overallScore,
                    weight = entity.weight,
                    durationMs = entity.durationMs,
                    timestamp = entity.timestamp
                )
            }
            .groupBy({ it.first }, { it.second })

        val result = mutableListOf<HistoryListItem>()
        DateGroup.entries.forEach { group ->
            val items = grouped[group]
            if (!items.isNullOrEmpty()) {
                result.add(HistoryListItem.Header(group))
                items.forEach { result.add(HistoryListItem.WorkoutCard(it)) }
            }
        }
        return result
    }
}
