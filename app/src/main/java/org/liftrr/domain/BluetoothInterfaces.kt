package org.liftrr.domain

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow
import org.liftrr.bluetooth.ScannedDevice

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val device: BluetoothDevice) : ConnectionState()
    data class Error(val message: String) : ConnectionState()

}

sealed class ScanState {
    data object Idle : ScanState()
    data class Scanning(val devices: List<ScannedDevice>) : ScanState()
    data class Error(val message: String) : ScanState()
    data class Complete (val devices : List<ScannedDevice>) : ScanState()
}

interface BluetoothServiceController {
    val isServiceRunning: StateFlow<Boolean>
    fun startService()
    fun stopService()
}

interface BluetoothScanner {
    val scanState: StateFlow<ScanState>
    val devices : MutableSet<BluetoothDevice>
    fun startScan()
    fun stopScan()
}

interface BluetoothConnector {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(device: BluetoothDevice): Result<Unit>
    suspend fun disconnect(): Result<Unit>
}

interface BluetoothConnectionManager : BluetoothServiceController, BluetoothScanner, BluetoothConnector