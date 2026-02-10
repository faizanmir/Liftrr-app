package org.liftrr.ui.screens.user.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.liftrr.data.models.UserDto
import org.liftrr.data.preferences.ThemePreferences
import org.liftrr.data.repository.AuthRepository
import org.liftrr.utils.DispatcherProvider
import javax.inject.Inject

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(
        val user: UserDto,
        val isDarkMode: Boolean,
        val isDynamicColorEnabled: Boolean
    ) : ProfileUiState()
    data class NotLoggedIn(
        val isDarkMode: Boolean,
        val isDynamicColorEnabled: Boolean
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themePreferences: ThemePreferences,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun refreshProfile() {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch(dispatchers.io) {
            try {
                val user = authRepository.getCurrentUserOnce()
                val isDarkMode = themePreferences.isDarkModeEnabled() ?: false
                val isDynamicColor = themePreferences.isDynamicColorEnabled()

                if (user != null) {
                    _uiState.update {
                        ProfileUiState.Success(
                            user = user,
                            isDarkMode = isDarkMode,
                            isDynamicColorEnabled = isDynamicColor
                        )
                    }
                } else {
                    _uiState.update {
                        ProfileUiState.NotLoggedIn(
                            isDarkMode = isDarkMode,
                            isDynamicColorEnabled = isDynamicColor
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    ProfileUiState.Error(e.message ?: "Failed to load profile")
                }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            themePreferences.setDarkMode(enabled)
            _uiState.update { currentState ->
                when (currentState) {
                    is ProfileUiState.Success -> currentState.copy(isDarkMode = enabled)
                    is ProfileUiState.NotLoggedIn -> currentState.copy(isDarkMode = enabled)
                    else -> currentState
                }
            }
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch(dispatchers.io) {
            themePreferences.setDynamicColor(enabled)
            _uiState.update { currentState ->
                when (currentState) {
                    is ProfileUiState.Success -> currentState.copy(isDynamicColorEnabled = enabled)
                    is ProfileUiState.NotLoggedIn -> currentState.copy(isDynamicColorEnabled = enabled)
                    else -> currentState
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(dispatchers.io) {
            try {
                _logoutState.update { LogoutState.Loading }
                authRepository.signOut()
                _logoutState.update { LogoutState.Idle }
                loadUserProfile()
            } catch (e: Exception) {
                _logoutState.update {
                    LogoutState.Error(e.message ?: "Failed to logout")
                }
            }
        }
    }

    fun resetLogoutState() {
        _logoutState.update { LogoutState.Idle }
    }
}

sealed class LogoutState {
    data object Idle : LogoutState()
    data object Loading : LogoutState()
    data object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}
