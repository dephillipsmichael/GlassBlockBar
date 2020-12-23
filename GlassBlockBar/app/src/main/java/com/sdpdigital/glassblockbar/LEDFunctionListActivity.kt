package com.sdpdigital.glassblockbar

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.view.BaseListRecyclerAdapter
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver


/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [LEDFunctionDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class LEDFunctionListActivity : AppCompatActivity(), ConnectionObserver {

    val LOG_TAG = (LEDFunctionListActivity::class).simpleName

    companion object {
        val colorPickerTitle = "Animation Picker"
        val beatDetectorTitle = "Beat Detector"
        val equalizerTitle = "Equalizer"
    }
    private val featureList = listOf(colorPickerTitle, beatDetectorTitle, equalizerTitle)

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        setupRecyclerView(findViewById(R.id.item_list))

        setupGlassBlockViewModel()
    }

    override fun onStop() {
        super.onStop()
        glassBlockViewModel?.disconnect()
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
                    ConnectionState.Disconnecting -> bleDisconnecting()
                    else -> {
                        // no-op
                    }
                }
            }
            glassBlockViewModel?.connectionState?.observe(this, connectionObserver)
        }
    }

    private fun bleDisconnecting() {
        finish()  // goes back to connection screen
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView?.layoutManager = LinearLayoutManager(this)
        val adapter = BaseListRecyclerAdapter(this, featureList)
        recyclerView?.adapter = adapter
        adapter.setClickListener(object: BaseListRecyclerAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                val fragment = when (featureList[position]) {
                    colorPickerTitle -> ColorPickerFragment()
                    beatDetectorTitle -> BeatDetectorFragment()
                    equalizerTitle -> EqualizerFragment()
                    else -> ColorPickerFragment()
                }
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.item_detail_container, fragment)
                        .commit()
            }
        })
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.d(LOG_TAG, "Glass Block disconnecting...")
    }

    override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
        Log.d(LOG_TAG, "Glass Block disconnected")
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        Log.d(LOG_TAG, "Glass Block device ready")
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        Log.d(LOG_TAG, "Glass Block connected")
    }

    override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
        Log.d(LOG_TAG, "Glass Block failed to connected, reason $reason")
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        Log.d(LOG_TAG, "Glass Block connecting...")
    }
}