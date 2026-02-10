package org.liftrr.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.liftrr.domain.BluetoothConnector
import org.liftrr.domain.ConnectionState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of BluetoothConnector.
 * Single Responsibility: Establishing and managing BLE device connections.
 */
@SuppressLint("MissingPermission")
@Singleton
class BluetoothConnectorImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BluetoothConnector {

    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            if (_connectionState.value is ConnectionState.Connected) {
                continuation.resume(Result.failure(IllegalStateException("Already connected")))
                return@suspendCancellableCoroutine
            }

            _connectionState.value = ConnectionState.Connecting

            val connectionCallback = object : BluetoothGattCallback() {

                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    when {
                        status != BluetoothGatt.GATT_SUCCESS -> {
                            _connectionState.value = ConnectionState.Error("Connection failed: $status")
                            gatt.close()
                            bluetoothGatt = null
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(ConnectionException(status)))
                            }
                        }
                        newState == BluetoothProfile.STATE_CONNECTED -> {
                            _connectionState.value = ConnectionState.Connected(device)
                            gatt.discoverServices()
                            if (continuation.isActive) {
                                continuation.resume(Result.success(Unit))
                            }
                        }
                        newState == BluetoothProfile.STATE_DISCONNECTED -> {
                            _connectionState.value = ConnectionState.Disconnected
                            gatt.close()
                            bluetoothGatt = null
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(ConnectionException(status)))
                            }
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        _connectionState.value = ConnectionState.Error("Service discovery failed: $status")
                    }
                }
            }

            bluetoothGatt = device.connectGatt(context, false, connectionCallback)

            continuation.invokeOnCancellation {
                bluetoothGatt?.close()
                bluetoothGatt = null
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            bluetoothGatt?.let { gatt ->
                gatt.disconnect()
                gatt.close()
            }
            bluetoothGatt = null
            _connectionState.value = ConnectionState.Disconnected
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sets the connection state to error.
     * Used by other components (e.g., scanner) to report errors that affect connection state.
     */
    fun setError(message: String) {
        _connectionState.value = ConnectionState.Error(message)
    }
}

class ConnectionException(val status: Int) : Exception("BLE connection failed with status: $status")
