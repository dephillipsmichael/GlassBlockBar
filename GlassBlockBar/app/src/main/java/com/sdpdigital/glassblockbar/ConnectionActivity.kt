package com.sdpdigital.glassblockbar

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
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
import com.sdpdigital.glassblockbar.viewmodel.ScannerViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState



/**
 * An activity that handles connection with the bar, and updates
 * the user to state changes.
 */
class ConnectionActivity : AppCompatActivity() {

    val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    val ENABLE_BLUETOOTH_REQUEST_CODE = 1523

    val LOG_TAG = (ConnectionActivity::class).simpleName

    val bleNamePrefix = "Glass Block Bar"

    var connectionStateText: TextView? = null
    var progressBar: ProgressBar? = null

    var scannerViewModel: ScannerViewModel? = null
    var glassBlockViewModel: GlassBlockBarViewModel? = null

    val mainHandler = Handler()

    val SKIP_CONNECTION = false

    private val REQUEST_PERMISSION_REQ_CODE = 34 // any 8-bit number


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

        setupScannerViewModel()
        setupGlassBlockViewModel()
    }

    override fun onResume() {
        super.onResume()

        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        if (!bluetoothAdapter.isEnabled) {
            return
        }

        if (SKIP_CONNECTION) {
            onDeviceReady()
            scannerViewModel?.stopScan()
        } else {
            startScan()
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            progressBar?.visibility = View.INVISIBLE
            connectionStateText?.text = "Bluetooth disabled"

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
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
            scannerViewModel?.startScan()
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

    private fun setupScannerViewModel() {
        // Create view model containing utility methods for scanning
        scannerViewModel = ViewModelProvider(this).get(ScannerViewModel::class.java)

        // Create the observer which updates the UI.
        val deviceObserver = Observer<List<DiscoveredBluetoothDevice>> { deviceList  ->
            mainHandler.post {
                // Update the UI, in this case, a TextView.
                deviceList?.let { devices ->
                    Log.d(LOG_TAG, "New device list " + devices.map { it.name })
                }
                deviceList?.filter { device ->
                    device.name?.let {
                        return@filter it.startsWith(bleNamePrefix)
                    }
                    return@filter false
                }?.first()?.let {
                    connect(it)
                    stopScan()
                }
            }
        }

        scannerViewModel?.devices?.observe(this, deviceObserver)
        startScanning()
    }

    override fun onRestart() {
        super.onRestart()
        clear()
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private fun stopScan() {
        scannerViewModel?.stopScan()
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private fun clear() {
        scannerViewModel?.devices?.clear()
        scannerViewModel?.scannerState?.clearRecords()
    }

    public fun connect(device: DiscoveredBluetoothDevice) {
        progressBar?.visibility = View.VISIBLE
        if (connectionStateText?.text != "Connecting...") {
            connectionStateText?.text != "Connecting..."
            glassBlockViewModel?.connect(device)
        }
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
}