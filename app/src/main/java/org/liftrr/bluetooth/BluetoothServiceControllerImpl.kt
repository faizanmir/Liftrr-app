package org.liftrr.bluetooth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.liftrr.domain.BluetoothServiceController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BluetoothServiceController {

    companion object {
        private const val TAG = "BluetoothServiceControl"
    }

    private val _isServiceRunning = MutableStateFlow(false)
    override val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            _isServiceRunning.value = true
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _isServiceRunning.value = false
            isBound = false
        }
    }

    override fun startService() {
        if (_isServiceRunning.value) return

        val intent = Intent(context, BluetoothConnectionService::class.java).apply {
            action = BluetoothConnectionService.ACTION_START_SERVICE
        }
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun stopService() {
        val intent = Intent(context, BluetoothConnectionService::class.java).apply {
            action = BluetoothConnectionService.ACTION_STOP_SERVICE
        }

        if (isBound) {
            try {
                context.unbindService(serviceConnection)
                isBound = false
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "stopService: ",e)
            }
        }

        context.startService(intent)
        _isServiceRunning.value = false
    }
}