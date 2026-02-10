package org.liftrr.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Loading : SplashDestination()
    object Onboarding : SplashDestination()
    object Home : SplashDestination()
    data class ResumeSession(val sessionId: Long) : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            val minSplashJob = launch { delay(2000L) }

            val hasCompletedOnboarding = false

            val activeSession = false

            minSplashJob.join()

            // Determine destination
            _destination.value = when {
                !hasCompletedOnboarding -> SplashDestination.Onboarding
                activeSession != null -> SplashDestination.ResumeSession(1)
                else -> SplashDestination.Home
            }

            _isLoading.value = false
        }
    }
}