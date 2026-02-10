package org.liftrr.ui.screens.user.profile

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.liftrr.data.models.AuthResult
import org.liftrr.data.models.UserDto
import org.liftrr.data.repository.AuthRepository
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: UserDto) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserOnce()
            _currentUser.value = user
        }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            when (val result = authRepository.signInWithEmailPassword(email, password)) {
                is AuthResult.Success -> {
                    Log.d(TAG, "Sign in successful: ${result.user}")
                    _currentUser.value = result.user
                    _uiState.value = AuthUiState.Success(result.user)
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "Sign in error: ${result.message}")
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun signUpWithEmailPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            when (val result = authRepository.signUpWithEmailPassword(
                email = email,
                password = password,
                firstName = firstName.ifBlank { null },
                lastName = lastName.ifBlank { null }
            )) {
                is AuthResult.Success -> {
                    Log.d(TAG, "Sign up successful: ${result.user}")
                    _currentUser.value = result.user
                    _uiState.value = AuthUiState.Success(result.user)
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "Sign up error: ${result.message}")
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            when (val result = authRepository.signInWithGoogle(context)) {
                is AuthResult.Success -> {
                    Log.d(TAG, "Google sign in successful: ${result.user}")
                    _currentUser.value = result.user
                    _uiState.value = AuthUiState.Success(result.user)
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "Google sign in error: ${result.message}")
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
