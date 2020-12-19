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

    public fun sendARGB(argb: ByteArray) {
        glassBlockManager.writeARGBValue(argb)
    }

    public fun sendLowMidHigh(lowMidHigh: ByteArray) {
        glassBlockManager.writeLowMidHighBeats(lowMidHigh)
    }

    public fun sendBitshiftEqualizer(eqValues: ByteArray) {
        glassBlockManager.writeBitshiftEqualizer(eqValues)
    }

    public fun sendEqualizer(eqValues: ByteArray) {
        glassBlockManager.writeEqualizer(eqValues)
    }

    public fun sendFunction(functionByte: Byte) {
        val functionByteArray = ByteArray(1) { functionByte }
        glassBlockManager.writeFunction(functionByteArray)
    }

    public fun sendGlobalBrightness(brightnessByte: Byte) {
        val brightnessByteArray = ByteArray(1) { brightnessByte }
        glassBlockManager.writeGlobalBrightness(brightnessByteArray)
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
    private fun disconnect() {
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
