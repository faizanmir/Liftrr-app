package org.liftrr.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.liftrr.domain.BluetoothScanner
import org.liftrr.domain.ScanState
import org.liftrr.utils.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton


data class ScannedDevice(
    val name: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val address : String = ""
)

@SuppressLint("MissingPermission")
@Singleton
class BluetoothScannerImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider
) : BluetoothScanner {

    companion object {
        private const val SCAN_DURATION_MS = 5000L
        private const val TAG = "BluetoothScannerImpl"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private val bleScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    override val devices: MutableSet<BluetoothDevice> = mutableSetOf()

    private var onDeviceFound: ((ScannedDevice) -> Unit)? = null
    private var onScanError: ((Int) -> Unit)? = null

    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scannedDevice = ScannedDevice(
                name = result.device.name ?: "Unknown Device",
                rssi = result.rssi,
                address = result.device.address
            )
            devices.add(result.device)
            onDeviceFound?.invoke(scannedDevice)
        }

        override fun onScanFailed(errorCode: Int) {
            onScanError?.invoke(errorCode)
        }
    }


    private var scanJob: Job? = null

    override fun startScan() {
        if (_scanState.value is ScanState.Scanning) return
        stopScan()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanJob = coroutineScope.launch {
            try {
                withTimeout(SCAN_DURATION_MS) {
                    scanDevicesFlow(settings)
                        .onStart {
                            _scanState.value = ScanState.Scanning(emptyList())
                        }
                        .catch { e ->
                            val msg = e.message ?: "Unknown Scan Error"
                            _scanState.value = ScanState.Error(msg)
                        }
                        .collect { devices ->
                            _scanState.value = ScanState.Scanning(devices)
                        }
                }
            } catch (e : TimeoutCancellationException) {
                val finalDevices = (_scanState.value as? ScanState.Scanning)?.devices ?: emptyList()
                Log.e(TAG, "startScan: device scan complete",e)
                _scanState.value = ScanState.Complete(finalDevices)
            }
        }
    }

    override fun stopScan() {
        val currentDevices = (_scanState.value as? ScanState.Scanning)?.devices ?: emptyList()
        scanJob?.cancel()
        scanJob = null
        _scanState.value = ScanState.Complete(currentDevices)
        devices.clear()
        bleScanner?.stopScan(callback)
    }

    private fun scanDevicesFlow(settings: ScanSettings): Flow<List<ScannedDevice>> = callbackFlow {
        val scanner = bleScanner

        if (scanner == null) {
            close(ScanException(errorCode = -1))
            return@callbackFlow
        }

        val foundDevices = mutableSetOf<ScannedDevice>()

        onDeviceFound = { device ->
            if (foundDevices.add(device)) {
                trySend(foundDevices.toList())
            }
        }
        onScanError = { errorCode ->
            close(ScanException(errorCode))
        }

        try {
            scanner.startScan(null, settings, callback)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (e: Exception) {
                Log.e(TAG, "scanDevicesFlow: ", e)
            }
            onDeviceFound = null
            onScanError = null
        }
    }.flowOn(dispatcherProvider.io)
}

class ScanException(val errorCode: Int) : Exception("BLE scan failed with error code: $errorCode")