package org.liftrr.domain

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow
import org.liftrr.bluetooth.ScannedDevice

// region Connection State

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val device: BluetoothDevice) : ConnectionState()
    data class Error(val message: String) : ConnectionState()

}

// endregion

// region Scan State

sealed class ScanState {
    data object Idle : ScanState()
    data class Scanning(val devices: List<ScannedDevice>) : ScanState()
    data class Error(val message: String) : ScanState()
    data class Complete (val devices : List<ScannedDevice>) : ScanState()
}

// endregion

// region Single Responsibility Interfaces (ISP)

/**
 * Handles Bluetooth service lifecycle.
 * Single Responsibility: Managing foreground service state.
 */
interface BluetoothServiceController {
    val isServiceRunning: StateFlow<Boolean>
    fun startService()
    fun stopService()
}

/**
 * Handles BLE device scanning.
 * Single Responsibility: Discovering nearby Bluetooth devices.
 */
interface BluetoothScanner {
    val scanState: StateFlow<ScanState>
    val devices : MutableSet<BluetoothDevice>
    fun startScan()
    fun stopScan()
}

/**
 * Handles BLE device connection.
 * Single Responsibility: Establishing and managing device connections.
 */
interface BluetoothConnector {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(device: BluetoothDevice): Result<Unit>
    suspend fun disconnect(): Result<Unit>
}

// endregion

// region Composite Interface

/**
 * Composite interface for clients that need full Bluetooth functionality.
 * Follows Interface Segregation: Clients can depend on specific interfaces
 * (BluetoothScanner, BluetoothConnector, BluetoothServiceController)
 * or this combined interface based on their needs.
 */
interface BluetoothConnectionManager : BluetoothServiceController, BluetoothScanner, BluetoothConnector

// endregion