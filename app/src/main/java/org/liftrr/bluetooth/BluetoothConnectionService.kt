package org.liftrr.bluetooth

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.liftrr.MainActivity
import org.liftrr.R
import org.liftrr.domain.BluetoothConnector
import org.liftrr.domain.BluetoothScanner
import org.liftrr.domain.ConnectionState
import org.liftrr.domain.ScanState
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothConnectionService : LifecycleService() {

    @Inject
    lateinit var scanner: BluetoothScanner

    @Inject
    lateinit var connector: BluetoothConnector

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val notification = createNotification(
                    contentText = "Ready to connect",
                    scanState = ScanState.Idle
                )
                startForeground(NOTIFICATION_ID, notification)
            }
            ACTION_STOP_SERVICE -> {
                lifecycleScope.launch {
                    scanner.stopScan()
                    connector.disconnect()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun observeState() {
        combine(
            connector.connectionState,
            scanner.scanState
        ) { connectionState, scanState ->
            Pair(connectionState, scanState)
        }
            .onEach { (connectionState, scanState) ->
                val message = buildStatusMessage(connectionState, scanState)
                updateNotification(message, scanState)
            }
            .launchIn(lifecycleScope)
    }

    private fun buildStatusMessage(connectionState: ConnectionState, scanState: ScanState): String {
        return when (connectionState) {
            is ConnectionState.Connected -> "Connected to ${getDeviceName(connectionState.device)}"
            is ConnectionState.Connecting -> "Connecting..."
            is ConnectionState.Error -> "Error: ${connectionState.message}"
            is ConnectionState.Disconnected -> when (scanState) {
                is ScanState.Scanning -> "Scanning... (${scanState.devices.size} found)"
                is ScanState.Error -> "Scan error: ${scanState.message}"
                is ScanState.Idle -> "Ready to connect"
                is ScanState.Complete -> "Completed scan"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice): String {
        return device.name ?: device.address
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bluetooth Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains Bluetooth device connection"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String, scanState: ScanState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BluetoothConnectionService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Liftrr")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "Stop", stopIntent)
            .setOngoing(true)
            .apply {
                if (scanState is ScanState.Scanning) {
                    setProgress(0, 0, true)
                    setSubText("${scanState.devices.size} devices")
                }
            }
            .build()
    }

    private fun updateNotification(contentText: String, scanState: ScanState) {
        val notification = createNotification(contentText, scanState)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "bluetooth_connection_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVICE = "org.liftrr.bluetooth.START_SERVICE"
        const val ACTION_STOP_SERVICE = "org.liftrr.bluetooth.STOP_SERVICE"
    }
}
