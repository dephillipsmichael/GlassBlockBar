package com.sdpdigital.glassblockbar

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sdpdigital.glassblockbar.ble.DiscoveredBluetoothDevice
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import com.sdpdigital.glassblockbar.viewmodel.ScannerViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState;


/**
 * An activity that handles connection with the bar, and updates
 * the user to state changes.
 */
class ConnectionActivity : AppCompatActivity() {

    val LOG_TAG = (ConnectionActivity::class).simpleName

    val bleNamePrefix = "Glass Block Bar"

    var connectionStateText: TextView? = null

    var scannerViewModel: ScannerViewModel? = null
    var glassBlockViewModel: GlassBlockBarViewModel? = null

    val mainHandler = Handler()

    val SKIP_CONNECTION = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_connection)

        connectionStateText = findViewById(R.id.text_connection_state)

        // Let user know we are starting scanning
        connectionStateText?.text = "Scanning..."

        setupScannerViewModel()
        setupGlassBlockViewModel()
    }

    override fun onResume() {
        super.onResume()

        if (SKIP_CONNECTION) {
            onDeviceReady()
            scannerViewModel?.stopScan()
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
        scannerViewModel?.startScan()
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
        if (connectionStateText?.text != "Connecting...") {
            connectionStateText?.text != "Connecting..."
            glassBlockViewModel?.connect(device)
        }
    }

    fun onDeviceDisconnecting() {
        Log.d(LOG_TAG, "Glass Block disconnecting...")
        scannerViewModel?.startScan()
    }

    fun onDeviceInitializing() {
        Log.d(LOG_TAG, "Glass Block initializing")
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
        connectionStateText?.text = "Connecting."
    }
}