package org.liftrr.ui.screens.connection

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.liftrr.domain.BluetoothConnector
import org.liftrr.domain.BluetoothScanner
import org.liftrr.domain.BluetoothServiceController
import org.liftrr.domain.ConnectionState
import org.liftrr.domain.ScanState
import javax.inject.Inject

data class DeviceConnectionUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isServiceRunning: Boolean = false,
    val scanState : ScanState = ScanState.Idle,
    val errorMessage: String? = null,
    val connectedDevice: BluetoothDevice? = null,
)

sealed class BluetoothDeviceEvent {
    data object StartService : BluetoothDeviceEvent()
    data object StopService : BluetoothDeviceEvent()
    data object StartScan : BluetoothDeviceEvent()
    data object StopScan : BluetoothDeviceEvent()
    data class ConnectToDevice(val address: String) : BluetoothDeviceEvent()
    data object Disconnect : BluetoothDeviceEvent()
    data object ClearError : BluetoothDeviceEvent()
}

/**
 * ViewModel following Interface Segregation Principle.
 * Depends on specific interfaces rather than a monolithic one:
 * - BluetoothServiceController: For service lifecycle
 * - BluetoothScanner: For device discovery
 * - BluetoothConnector: For connection management
 */
@HiltViewModel
class DeviceConnectionViewModel @Inject constructor(
    private val serviceController: BluetoothServiceController,
    private val scanner: BluetoothScanner,
    private val connector: BluetoothConnector
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceConnectionUiState())
    val uiState = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        observeBluetoothState()
    }

    private fun observeBluetoothState() {
        connector.connectionState
            .onEach { state ->
                _uiState.update { currentState ->
                    when (state) {
                        is ConnectionState.Disconnected -> currentState.copy(
                            isConnected = false,
                            isConnecting = false,
                            connectedDevice = null
                        )
                        is ConnectionState.Connecting -> currentState.copy(
                            isConnecting = true,
                            isConnected = false
                        )
                        is ConnectionState.Connected -> currentState.copy(
                            isConnected = true,
                            isConnecting = false,
                            connectedDevice = state.device
                        )
                        is ConnectionState.Error -> currentState.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = state.message
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        serviceController.isServiceRunning
            .onEach { isRunning ->
                _uiState.update { it.copy(isServiceRunning = isRunning) }
            }
            .launchIn(viewModelScope)

        scanJob = scanner.scanState
            .onEach { scanState ->
                _uiState.update { it.copy(scanState = scanState) }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = e.message) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BluetoothDeviceEvent) {
        when (event) {
            is BluetoothDeviceEvent.StartService -> serviceController.startService()
            is BluetoothDeviceEvent.StopService -> serviceController.stopService()
            is BluetoothDeviceEvent.StartScan -> startScan()
            is BluetoothDeviceEvent.StopScan -> stopScan()
            is BluetoothDeviceEvent.ConnectToDevice -> connectToDevice(scanner.devices.first { it.address == event.address })
            is BluetoothDeviceEvent.Disconnect -> disconnect()
            is BluetoothDeviceEvent.ClearError -> clearError()
        }
    }

    private fun startScan() {
        _uiState.update { it.copy(scanState = ScanState.Idle) }
        scanner.startScan()
    }

    private fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        scanner.stopScan()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        viewModelScope.launch {
            connector.connect(device)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            connector.disconnect()
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
