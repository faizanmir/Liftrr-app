package org.liftrr.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.liftrr.domain.BluetoothConnectionManager
import org.liftrr.bluetooth.BluetoothConnectionManagerImpl
import org.liftrr.domain.BluetoothConnector
import org.liftrr.bluetooth.BluetoothConnectorImpl
import org.liftrr.domain.BluetoothScanner
import org.liftrr.bluetooth.BluetoothScannerImpl
import org.liftrr.domain.BluetoothServiceController
import org.liftrr.bluetooth.BluetoothServiceControllerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {

    @Binds
    @Singleton
    abstract fun bindBluetoothServiceController(
        impl: BluetoothServiceControllerImpl
    ): BluetoothServiceController

    @Binds
    @Singleton
    abstract fun bindBluetoothScanner(
        impl: BluetoothScannerImpl
    ): BluetoothScanner

    @Binds
    @Singleton
    abstract fun bindBluetoothConnector(
        impl: BluetoothConnectorImpl
    ): BluetoothConnector

    @Binds
    @Singleton
    abstract fun bindBluetoothConnectionManager(
        impl: BluetoothConnectionManagerImpl
    ): BluetoothConnectionManager
}
