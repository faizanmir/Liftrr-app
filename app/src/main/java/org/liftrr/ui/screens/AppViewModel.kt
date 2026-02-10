package org.liftrr.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.liftrr.data.repository.AuthRepository
import javax.inject.Inject

/**
 * Application-level ViewModel
 * Manages app-wide state like authentication status
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    val workoutReportHolder: org.liftrr.domain.workout.WorkoutReportHolder
) : ViewModel() {

    val isUserLoggedIn: StateFlow<Boolean> = authRepository.getCurrentUser()
        .map { user -> user != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
}