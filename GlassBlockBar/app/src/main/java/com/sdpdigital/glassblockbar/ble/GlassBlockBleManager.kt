package com.sdpdigital.glassblockbar.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH
import no.nordicsemi.android.ble.PhyRequest
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import java.util.*

// Observable BleManager subscribe to connection and bonding state live data
public class GlassBlockBleManager(context: Context) : ObservableBleManager(context) {

    val LOG_TAG = GlassBlockBleManager::class.java.simpleName

    // Characteristic for writing value for all the glass blocks
    private var communicationCharacteristic: BluetoothGattCharacteristic? = null

    override fun getGattCallback(): BleManagerGattCallback {
        return MyManagerGattCallback()
    }

    override fun shouldAutoConnect(): Boolean {
        return true
    }

    override fun log(priority: Int, message: String) {
        Log.println(priority, "GlassBlockBleManager", message)
    }

    public fun clearDeviceCache() {
        refreshDeviceCache().enqueue()
    }

    public fun writeCommunicationCharMessage(beatSeqBytes: ByteArray, cancelAll: Boolean = false) {

        var bytesTosend = beatSeqBytes

        // For commands that don't use all the bytes, write them as 0s
        if (beatSeqBytes.size < 20) {
            bytesTosend = ByteArray(20) {
                if (it < bytesTosend.size) {
                    return@ByteArray bytesTosend[it]
                }
                return@ByteArray 0
            }
        }

//        This is causing BLE to become unresponsive
//        if (cancelAll) {
//            super.cancelQueue()
//        }

        // We do not want any commands queueing up
        // cancelQueue()
        // You may easily enqueue more operations here like such:
        writeCharacteristic(communicationCharacteristic, bytesTosend)
                .done { device: BluetoothDevice? ->
                    //Log.d("EQ_DEBUG", "Eq sent $debug")
                }
                .enqueue()


        val debug = beatSeqBytes.map { "$it, " }
        Log.d("EQ_DEBUG", "Beat Sequence sending $debug")
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private inner class MyManagerGattCallback : BleManagerGattCallback() {
        // This method will be called when the device is connected and services are discovered.
        // You need to obtain references to the characteristics and descriptors that you will use.
        // Return true if all required services are found, false otherwise.
        public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service =
                gatt.getService(LED_SERVICE_UUID)

            if (service != null) {
                communicationCharacteristic = service.getCharacteristic(COMMUNICATION_CHAR_UUID);
                communicationCharacteristic?.writeType = WRITE_TYPE_NO_RESPONSE
            }
            // Return true if all required services have been found
            return communicationCharacteristic != null
        }

        // If you have any optional services, allocate them here. Return true only if
        // they are found.
        override fun isOptionalServiceSupported(gatt: BluetoothGatt): Boolean {
            return super.isOptionalServiceSupported(gatt)
        }

        // Initialize your device here. Often you need to enable notifications and set required
        // MTU or write some initial data. Do it here.
        override fun initialize() {
            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            beginAtomicRequestQueue()
                // The best we can get with BT 4.0 (the tablet I'm using) is 23
                .add(requestMtu(247) // Remember, GATT needs 3 bytes extra. This will allow packet size of 244 bytes.
                    .with { device: BluetoothDevice?, mtu: Int ->
                        log(
                            Log.INFO,
                            "MTU set to $mtu"
                        )
                    }
                    .fail { device: BluetoothDevice?, status: Int ->
                        log(
                            Log.WARN,
                            "Requested MTU not supported: $status"
                        )
                    }
                )
                .add(setPreferredPhy(
                    PhyRequest.PHY_LE_1M_MASK, PhyRequest.PHY_LE_1M_MASK, PhyRequest.PHY_OPTION_NO_PREFERRED
                )
                    .fail { device: BluetoothDevice?, status: Int ->
                        log(
                            Log.WARN,
                            "Requested PHY not supported: $status"
                        )
                    }
                )
                // This CONNECTION_PRIORITY_HIGH makes a huge difference in
                // The latency of BLE.  For instance, setting this was the
                // difference in getting 13 msgs/sec vs 60 msgs/sec
                // or a factor of about 5x better with high priority
                .add(requestConnectionPriority(CONNECTION_PRIORITY_HIGH))
                .done { device: BluetoothDevice? ->
                    log(
                        Log.INFO,
                        "Target initialized"
                    )
                }
                .enqueue()
        }

        override fun onDeviceDisconnected() {
            // Device disconnected. Release your references here.
            communicationCharacteristic = null
        }
    }

    /**
     * Aborts time travel. Call during 3 sec after enabling Flux Capacitor and only if you don't
     * like 2020.
     */
    fun abort() {
        cancelQueue()
    }

    companion object {
        val LED_SERVICE_UUID =
                UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val COMMUNICATION_CHAR_UUID =
                UUID.fromString("6e400005-b5a3-f393-e0a9-e50e24dcca9e")
    }
}