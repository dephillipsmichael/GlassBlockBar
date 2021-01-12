package com.sdpdigital.glassblockbar.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.sdpdigital.glassblockbar.ble.GlassBlockBleManager
import com.sdpdigital.glassblockbar.ble.DiscoveredBluetoothDevice
import no.nordicsemi.android.ble.livedata.state.BondState
import no.nordicsemi.android.ble.livedata.state.ConnectionState

class GlassBlockBarViewModel(application: Application) : AndroidViewModel(application) {
    private val glassBlockManager: GlassBlockBleManager = GlassBlockBleManager(getApplication())
    private var device: BluetoothDevice? = null

    val connectionState: LiveData<ConnectionState>
        get() = glassBlockManager.state

    val bondingState: LiveData<BondState>
        get() = glassBlockManager.bondingState

    /**
     * Connect to the given peripheral.
     *
     * @param target the target device.
     */
    fun connect(target: DiscoveredBluetoothDevice) {
        // Prevent from calling again when called again (screen orientation changed).
        device = target.device
        reconnect()
    }

    public fun clearDeviceCache() {
        glassBlockManager.clearDeviceCache()
    }

    public fun sendARGB(argb: ByteArray) {
        glassBlockManager.writeCommunicationCharMessage(argb)
    }

    public fun sendLowMidHigh(lowMidHigh: ByteArray) {
        glassBlockManager.writeCommunicationCharMessage(lowMidHigh)
    }

    public fun sendBpmInfo(bpmValues: ByteArray) {
        glassBlockManager.writeCommunicationCharMessage(bpmValues)
    }

    public fun sendEqualizer(eqValues: ByteArray) {
        glassBlockManager.writeCommunicationCharMessage(eqValues)
    }

    public fun sendBeatSequence(beatSeq: ByteArray) {
        glassBlockManager.writeCommunicationCharMessage(beatSeq, true)
    }

    /**
     * Reconnects to previously connected device.
     * If this device was not supported, its services were cleared on disconnection, so
     * reconnection may help.
     */
    fun reconnect() {
        if (glassBlockManager.isConnected) {
            return
        }
        device?.let {
            glassBlockManager.connect(it)
                .retry(3, 10000)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    /**
     * Disconnect from peripheral.
     */
    public fun disconnect() {
        device = null
        glassBlockManager.disconnect().enqueue()
    }

    override fun onCleared() {
        super.onCleared()
        if (glassBlockManager.isConnected) {
            disconnect()
        }
    }
}
