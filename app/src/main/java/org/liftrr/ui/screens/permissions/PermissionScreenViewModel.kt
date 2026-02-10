package org.liftrr.ui.screens.permissions

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PermissionsUiState(
    val cameraGranted: Boolean = false,
    val locationGranted: Boolean = false,
    val bluetoothGranted: Boolean = false,
)

@HiltViewModel
class PermissionScreenViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState = _uiState.asStateFlow()

    fun onPermissionResult(permission: PermissionId, isGranted: Boolean) {
        _uiState.update { currentState ->
            when (permission) {
                PermissionId.CAMERA -> currentState.copy(cameraGranted = isGranted)
                PermissionId.LOCATION -> currentState.copy(locationGranted = isGranted)
                PermissionId.BLUETOOTH -> currentState.copy(bluetoothGranted = isGranted)
            }
        }
    }
}
