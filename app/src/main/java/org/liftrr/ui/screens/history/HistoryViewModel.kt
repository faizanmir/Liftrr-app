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
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(ExerciseFilter.ALL)

    private val _workoutToDelete = MutableStateFlow<HistoryWorkoutItem?>(null)
    val workoutToDelete: StateFlow<HistoryWorkoutItem?> = _workoutToDelete.asStateFlow()

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

    fun requestDelete(item: HistoryWorkoutItem) {
        _workoutToDelete.value = item
    }

    fun cancelDelete() {
        _workoutToDelete.value = null
    }

    fun confirmDelete() {
        val item = _workoutToDelete.value ?: return
        _workoutToDelete.value = null
        viewModelScope.launch(dispatchers.io) {
            val entity = workoutRepository.getWorkoutById(item.sessionId)
            if (entity != null) {
                workoutRepository.deleteWorkout(entity)
            }
        }
    }

    private fun groupByDate(workouts: List<WorkoutSessionEntity>): List<HistoryListItem> {
        if (workouts.isEmpty()) return emptyList()

        val now = Calendar.getInstance()
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
