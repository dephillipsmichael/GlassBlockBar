package com.sdpdigital.glassblockbar

import android.app.Activity
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import java.util.*
import kotlin.collections.ArrayList

class GlassBlockLEDApplication : Application(), ViewModelStoreOwner {

    val LOG_TAG = (GlassBlockLEDApplication::class).simpleName

    val bleNamePrefix = "Glass Block Bar"

    public val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    public val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    public val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

    val LED_SERVICE_UUID = UUID.fromString("6e400010-b5a3-f393-e0a9-e50e24dcca9e")
    val COMMUNICATION_CHAR_UUID = UUID.fromString("6e400007-b5a3-f393-e0a9-e50e24dcca9e")
    var communicationChar: BluetoothGattCharacteristic? = null

    private var connectedGatt: BluetoothGatt? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    public var bleListener: BleEventListener? = null

    private var gattCallback: BluetoothGattCallback? = null

    private var currentActivityClassType = BleConnectionActivity::class.java.simpleName

    private val lifeCycleCallback = object : ActivityLifecycleCallbacks {
        override fun onActivityPaused(p0: Activity) {}
        override fun onActivityStarted(p0: Activity) {}
        override fun onActivityDestroyed(p0: Activity) {}
        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
        override fun onActivityStopped(p0: Activity) {}
        override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {
            if (activity.javaClass.simpleName == BleConnectionActivity::class.java.simpleName &&
                    currentActivityClassType != BleConnectionActivity::class.java.simpleName) {
                disconnectBluetoothDevice()
            }

            currentActivityClassType = activity.javaClass.simpleName
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(lifeCycleCallback)
    }

    // Allows the app to share instances of the ViewModel
    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    override fun getViewModelStore(): ViewModelStore {
        return appViewModelStore
    }

    public fun connect(deviceAddress: String) {
        bluetoothAdapter.getRemoteDevice(deviceAddress)?.let {
            gattCallback = createGattCallback()
            connectedGatt = it.connectGatt(this, false, gattCallback)
        }
    }

    public fun disconnectBluetoothDevice() {
        connectedGatt?.let {
            Log.d(LOG_TAG, "Disconnected from BLE device")
            it.close()
        }
        connectedGatt = null
    }

    public fun goBackToConnectionScreen() {
        Log.d(LOG_TAG, "Sending user back to connection screen")
        val intent = Intent(applicationContext, BleConnectionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private var readyToSend = true
    private var queuedMessages = ArrayList<ByteArray>()
    public fun writeBleMessage(msg: ByteArray) {
        var bytesTosend = msg

        // For commands that don't use all the bytes, write them as 0s
        if (msg.size < 20) {
            bytesTosend = ByteArray(20) {
                if (it < bytesTosend.size) {
                    return@ByteArray bytesTosend[it]
                }
                return@ByteArray 0
            }
        }

        queuedMessages.add(bytesTosend)
        writeNextMessage()
    }

    // Keep writing through the message queue
    private fun writeNextMessage() {
        // Sending too quickly will lock the queue
        if (!readyToSend || queuedMessages.isEmpty()) {
            return
        }
        val device = connectedGatt ?: run { return }
        val char = communicationChar ?: run { return }

        queuedMessages.firstOrNull()?.let {
            queuedMessages.removeAt(0)
            readyToSend = false
            // Sending too quickly will lock the queue
            char.value = it
            device.writeCharacteristic(communicationChar)

            var strOutput = ""
            for (i in 0 until 20) {
                strOutput += it[i].toInt()
                if (i < 19) {
                    strOutput += ", "
                }
            }
            Log.d(LOG_TAG, strOutput)
        }
    }

    private fun updateConnectedListener(msg: String) {
        mainHandler.post {
            bleListener?.connectionStateChanged(msg)
        }
    }

    private fun updateConnectedAndReadyStatus() {
        mainHandler.post {
            bleListener?.connectedAndReady()
        }
    }

    private fun updateDisconnectedStatus() {
        mainHandler.post {
            bleListener?.disconnected()
        }
    }

    private fun createGattCallback(): BluetoothGattCallback {
        return object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                val deviceAddress = gatt.device.address
                mainHandler.post {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.d(LOG_TAG, "Successfully connected to $deviceAddress")

                                val debug = gatt.device.name
                                Log.d(LOG_TAG, "Discovering services $debug")
                                //?.requestMtu(23)
                                connectedGatt?.discoverServices()
                                updateConnectedListener("Discovering Services...")
                                //updateConnectedListener("Requesting MTU...")

                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d(LOG_TAG, "Successfully disconnected from $deviceAddress")
                            gatt.close()

                            if (currentActivityClassType != BleConnectionActivity::class.java.simpleName) {
                                goBackToConnectionScreen()
                                updateDisconnectedStatus()
                            }
                        }
                    } else {
                        Log.d(LOG_TAG, "Error $status encountered for $deviceAddress! Disconnecting...")
                        gatt.close()
                        updateDisconnectedStatus()
                    }
                }
            }

            override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?) {
                Log.d(LOG_TAG,  "On characteristic changed ${characteristic?.uuid ?: ""}")
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    if (gatt?.services?.size == 0) {
                        Log.d(LOG_TAG,  "No services found")
                        updateConnectedListener("Service Discovery Failed")
                        disconnectBluetoothDevice()
                        return
                    }
                    for (gattService in gatt?.services ?: listOf()) {
                        Log.d(LOG_TAG,  "Service UUID Found: " + gattService.uuid.toString())
                        val char = gattService.getCharacteristic(COMMUNICATION_CHAR_UUID)
                        if (char != null) {
                            communicationChar = char
                            Log.d(LOG_TAG, "Write type " + communicationChar!!.writeType)
                        }
                        for (char in gattService?.characteristics ?: listOf()) {
                            Log.d(LOG_TAG,  "Char UUID Found: " + char.uuid.toString())
                        }
                    }

                    if (communicationChar != null) {
                        readyToSend = true
                        queuedMessages.clear()
                        updateConnectedListener("Connected")
                        updateConnectedAndReadyStatus()
                    } else {
                        updateConnectedListener("Couldn't find comm char")
                        disconnectBluetoothDevice()
                        return
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                Log.d(LOG_TAG,"ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mainHandler.post {
                        updateConnectedListener("Discovering Services...")
                        connectedGatt?.discoverServices()
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?,
                                               characteristic: BluetoothGattCharacteristic?,
                                               status: Int) {
                mainHandler.post {
                    readyToSend = true
                    writeNextMessage()
                }
            }
        }
    }
}

public class AppViewModelFactory(val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java)
            .newInstance(app)
    }
}

public interface BleEventListener {
    public fun connectionStateChanged(userMessaging: String)
    public fun disconnected()
    public fun connectedAndReady()
}