package org.liftrr.bluetooth

import org.liftrr.domain.BluetoothConnectionManager
import org.liftrr.domain.BluetoothConnector
import org.liftrr.domain.BluetoothScanner
import org.liftrr.domain.BluetoothServiceController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite implementation of BluetoothConnectionManager.
 *
 * Follows SOLID principles:
 * - Single Responsibility: Delegates to specialized implementations
 * - Open/Closed: New behavior added via new implementations, not modification
 * - Liskov Substitution: Each delegate is substitutable
 * - Interface Segregation: Composed of segregated interfaces
 * - Dependency Inversion: Depends on abstractions injected via constructor
 *
 * Uses Kotlin delegation to compose functionality from:
 * - BluetoothServiceControllerImpl: Service lifecycle
 * - BluetoothScannerImpl: Device discovery
 * - BluetoothConnectorImpl: Connection management
 */
@Singleton
class BluetoothConnectionManagerImpl @Inject constructor(
    private val serviceController: BluetoothServiceControllerImpl,
    private val scanner: BluetoothScannerImpl,
    private val connector: BluetoothConnectorImpl
) : BluetoothConnectionManager,
    BluetoothServiceController by serviceController,
    BluetoothScanner by scanner,
    BluetoothConnector by connector {

    override fun stopService() {
        scanner.stopScan()
        serviceController.stopService()
    }
}
