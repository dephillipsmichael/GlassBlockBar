package com.sdpdigital.glassblockbar

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sdpdigital.glassblockbar.ble.DiscoveredBluetoothDevice
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import java.util.*

/**
 * An activity that handles connection with the bar, and updates
 * the user to state changes.
 */
class BleConnectionActivity : AppCompatActivity(), BleEventListener {

    private var isScanning = false

    private fun createScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!isScanning) {
                    return
                }

                result.device?.let {
                    if (it.name == null) {
                        return@let
                    }
                    if (it.name.startsWith("Glass Block")) {
                        Log.d(
                                LOG_TAG,
                                "Found BLE device! Name: ${it.name ?: "Unnamed"}, address: $it.address"
                        )
                        stopScan()
                        onDeviceConnecting()
                        app?.bleListener = this@BleConnectionActivity
                        app?.connect(it.address)
                    }
                }
            }
        }
    }
    private var scanCallback: ScanCallback? = null

    val ENABLE_BLUETOOTH_REQUEST_CODE = 1523

    val LOG_TAG = (BleConnectionActivity::class).simpleName

    var connectionStateText: TextView? = null
    var progressBar: ProgressBar? = null

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    val mainHandler = Handler()

    val SKIP_CONNECTION = false

    private val REQUEST_PERMISSION_REQ_CODE = 34 // any 8-bit number

    val LED_SERVICE_UUID =
        UUID.fromString("6e400010-b5a3-f393-e0a9-e50e24dcca9e")

    private val app: GlassBlockBarApplication? get() {
        return (application as? GlassBlockBarApplication)
    }

    private val bluetoothAdapter: BluetoothAdapter? get() {
        return app?.bluetoothAdapter
    }

    private val bleScanner: BluetoothLeScanner? get() {
        return app?.bleScanner
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_connection)

        connectionStateText = findViewById(R.id.text_connection_state)
        progressBar = findViewById(R.id.progress_bar)

        // Let user know we are starting scanning
        connectionStateText?.text = "Scanning..."

        setupGlassBlockViewModel()
    }

    override fun onResume() {
        super.onResume()

        app?.bleListener = this

        if (bluetoothAdapter?.isEnabled != true) {
            promptEnableBluetooth()
        } else {
            startScanning()
        }
    }

    override fun onPause() {
        super.onPause()
        app?.bleListener = null
    }

    private fun startScanning() {
        if (bluetoothAdapter?.isEnabled != true) {
            return
        }

        if (SKIP_CONNECTION) {
            onDeviceReady()
        } else {
            startScan()
        }
    }

    private fun promptEnableBluetooth() {
        if (bluetoothAdapter?.isEnabled != true) {
            progressBar?.visibility = View.INVISIBLE
            connectionStateText?.text = "Bluetooth disabled"

            bluetoothAdapter?.let {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_REQ_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We have been granted the Manifest.permission.ACCESS_FINE_LOCATION permission. Now we may proceed with scanning.
                    startScan()
                }
            }
        }
    }


    /**
     *
     * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then lEScanCallback
     *
     * is activated This will perform regular scan for custom BLE Service UUID and then filter out.
     *
     * using class ScannerServiceParser
     *
     */
    private fun startScan() {

        // Since Android 6.0 we need to obtain Manifest.permission.ACCESS_FINE_LOCATION to be able to scan for

        // Bluetooth LE devices. This is related to beacons as proximity devices.

        // On API older than Marshmallow the following code does nothing.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // When user pressed Deny and still wants to use this functionality, show the rationale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_REQ_CODE)
            }
            return
        }
        mainHandler.post {
            if (isScanning) {
                return@post
            }
            Log.d(LOG_TAG, "Started scanning...")
            isScanning = true
            val scanSettings = app?.scanSettings ?: run { return@post }
            scanCallback = createScanCallback()
            bleScanner?.startScan(null, scanSettings, scanCallback)
        }
    }

    private fun setupGlassBlockViewModel() {
        (application as? GlassBlockBarApplication)?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
            // Connection state observer
            val connectionObserver = Observer<ConnectionState> { connectionState  ->
                // Update the UI, in this case, a TextView.
                Log.d(LOG_TAG, "New connection state $connectionState")
                when(connectionState) {
                    ConnectionState.Connecting -> onDeviceConnecting()
                    ConnectionState.Ready -> onDeviceReady()
                    ConnectionState.Initializing -> onDeviceInitializing()
                    ConnectionState.Disconnecting -> onDeviceDisconnecting()
                    else -> {
                        // no-op
                    }
                }
            }
            glassBlockViewModel?.connectionState?.observe(this, connectionObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        if (isScanning) {
            Log.d(LOG_TAG, "Stopped scanning...")
            bleScanner?.stopScan(scanCallback)
        }
        isScanning = false
    }

    fun onDeviceDiscoveringServicesFailed() {
        Log.d(LOG_TAG, "Discovering Services Failed...")
        progressBar?.visibility = View.VISIBLE
        connectionStateText?.text = "Discovering Services Failed"
    }

    fun onDeviceDiscoveringServices() {
        Log.d(LOG_TAG, "Discovering Services...")
        progressBar?.visibility = View.VISIBLE
        connectionStateText?.text = "Discovering Services..."
    }

    fun onDeviceRequestingMTU() {
        Log.d(LOG_TAG, "Negotiating MTU...")
        progressBar?.visibility = View.VISIBLE
        connectionStateText?.text = "Negotiating MTU..."
    }

    fun onDeviceDisconnecting() {
        Log.d(LOG_TAG, "Glass Block disconnecting...")
        startScanning()
        progressBar?.visibility = View.VISIBLE
        connectionStateText?.text = "Scanning..."
    }

    fun onDeviceInitializing() {
        Log.d(LOG_TAG, "Glass Block initializing")
        progressBar?.visibility = View.VISIBLE
        connectionStateText?.text = "Initializing..."
    }

    fun onDeviceReady() {
        Log.d(LOG_TAG, "Glass Block device ready")
        val intent = Intent(applicationContext, LEDFunctionListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(intent)
    }

    fun onDeviceConnecting() {
        Log.d(LOG_TAG, "Glass Block connecting...")
        progressBar?.visibility = View.VISIBLE
        connectionStateText?.text = "Connecting."
    }

    override fun connectionStateChanged(userMessaging: String) {
        Log.d(LOG_TAG, userMessaging)
        progressBar?.visibility = View.VISIBLE
        connectionStateText?.text = userMessaging
    }

    override fun disconnected() {
        startScanning()
    }

    override fun connectedAndReady() {
        onDeviceReady()
    }
}