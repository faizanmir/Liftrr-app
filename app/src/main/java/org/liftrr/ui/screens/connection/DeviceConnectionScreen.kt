package org.liftrr.ui.screens.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.liftrr.domain.ScanState
import org.liftrr.bluetooth.ScannedDevice
import org.liftrr.ui.components.CircleShapeWithIcon
import org.liftrr.ui.components.LoaderWithTextSubtitle
import org.liftrr.ui.components.OnBoardingScreenHeadline
import org.liftrr.ui.components.OnBoardingScreenHeadlineDescription
import org.liftrr.ui.theme.LiftrrTheme

@Composable
fun DeviceConnectionScreen(
    viewModel: DeviceConnectionViewModel = hiltViewModel(),
    onSkip: () -> Unit,
    onConnectionSuccess: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected && onConnectionSuccess != null) {
            onConnectionSuccess()
        }
    }

    DeviceConnectionScreenContent(
        uiState = uiState, onEvent = viewModel::onEvent, onSkip = onSkip
    )
}

@Composable
fun DeviceConnectionScreenContent(
    uiState: DeviceConnectionUiState,
    onEvent: (BluetoothDeviceEvent) -> Unit,
    onSkip: () -> Unit = {}
) {

    LaunchedEffect(Unit) {
        onEvent(BluetoothDeviceEvent.StartScan)
    }

    Surface(
        modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(50.dp))
            BluetoothIcon()
            Spacer(modifier = Modifier.height(20.dp))
            OnBoardingScreenHeadline("Connect Your Device", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            OnBoardingScreenHeadlineDescription("Turn on your LIFTRR sensor and make sure it's nearby")
            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.scanState is ScanState.Scanning) {
                LoaderWithTextSubtitle("Scanning for devices...")
            } else {
                ScanForDeviceButton {
                    onEvent(BluetoothDeviceEvent.StartScan)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.weight(1f)) {
                val devices = when (uiState.scanState) {
                    is ScanState.Complete -> uiState.scanState.devices
                    is ScanState.Scanning -> uiState.scanState.devices
                    else -> emptyList()
                }
                BluetoothDeviceList(devices = devices, onDeviceSelected = { scannedDevice ->
                    onEvent(BluetoothDeviceEvent.ConnectToDevice(scannedDevice.address))
                })
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSkip, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary // Changed tint to primary for better visibility
                ), modifier = Modifier.padding(bottom = 30.dp) // Add padding from bottom edge
            ) {
                Text("Skip for now")
            }
        }
    }
}

/**
 * Displays a signal strength icon based on the RSSI value.
 *
 * - Excellent (>-60 dBm): Strong signal, 4 bars.
 * - Good (-60 to -85 dBm): Moderate signal, 3 bars.
 * - Weak (< -85 dBm): Weak signal, 1 bar.
 */
@Composable
fun RssiSignalStrengthIcon(rssi: Int) {
    val signalIcon = when {
        rssi > -60 -> Icons.Filled.SignalCellularAlt
        rssi > -85 -> Icons.Filled.SignalCellularAlt2Bar
        else -> Icons.Filled.SignalCellularAlt1Bar
    }
    Icon(imageVector = signalIcon, contentDescription = "Signal Strength")
}

@Composable
fun BluetoothDeviceList(
    devices: List<ScannedDevice>, onDeviceSelected: (ScannedDevice) -> Unit = {}
) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(devices) { scannedDevice ->
            Card(onClick = { onDeviceSelected(scannedDevice) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Spacer(Modifier.width(30.dp))
                        Column {
                            Text(scannedDevice.name)
                            Spacer(Modifier.height(5.dp))
                            Text(scannedDevice.address)
                        }
                    }
                    RssiSignalStrengthIcon(rssi = scannedDevice.rssi)
                }
            }
        }
    }
}


@Composable
fun BluetoothIcon() {
    CircleShapeWithIcon(Icons.Default.Bluetooth, iconSize = 60.dp)
}

@Composable
fun ScanForDeviceButton(onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(0.5f).height(40.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 5.dp, horizontal = 20.dp)
        ) {
            Icon(Icons.Outlined.BluetoothSearching, contentDescription = "Scan for devices")
            Text("Scan for devices", style = MaterialTheme.typography.titleMedium)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DeviceConnectionScreenCompletePreview() {

    LiftrrTheme {
        DeviceConnectionScreenContent(uiState = DeviceConnectionUiState(
            scanState = ScanState.Complete(
                devices = listOf(
                    ScannedDevice("Device 1", -55, address = "1234567890"), // Excellent
                    ScannedDevice("Device 2", -75, address = "0987654321"), // Good
                    ScannedDevice("Device 3", -95, address = "1122334455")  // Weak
                )
            )
        ), onEvent = {}, onSkip = {})
    }
}
