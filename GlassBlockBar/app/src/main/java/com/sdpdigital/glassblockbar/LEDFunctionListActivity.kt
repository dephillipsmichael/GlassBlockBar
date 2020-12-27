package com.sdpdigital.glassblockbar

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.view.BaseListRecyclerAdapter
import com.sdpdigital.glassblockbar.view.VerticalSeekBar
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
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
        val colorPickerTitle = "Color Picker"
        val animationPickerTitle = "Animation Picker"
        val beatDetectorTitle = "Beat Detector"
        val equalizerTitle = "Equalizer"
        val lightComposerTitle = "Light Composer"
    }
    private val featureList = listOf(
            colorPickerTitle, animationPickerTitle, beatDetectorTitle,
            equalizerTitle, lightComposerTitle)

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        setupRecyclerView(findViewById(R.id.item_list))
        setupBrightnessSlider(findViewById(R.id.seek_bar_brightness))

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

    private fun setupBrightnessSlider(brightnessSlider: VerticalSeekBar) {
        brightnessSlider.setVerticalListener(object : VerticalSeekBar.VerticalProgressChangedListener {
            override fun onTouchUp(seekbar: VerticalSeekBar?, progress: Int) {
                seekbar?.progress?.let { progress ->
                    Log.d(LOG_TAG, "Sent new brightness $progress")
                    val brightnessOnlyARGB = ByteArray(4)
                    { i -> arrayOf(progress, 0, 0, 0)[i].toByte() }
                    glassBlockViewModel?.sendARGB(brightnessOnlyARGB)
                }
            }
            override fun progressChanged(seekbar: VerticalSeekBar?, progress: Int) {}
        })
        brightnessSlider.progress = brightnessSlider.max - 1
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
                    animationPickerTitle -> AnimationPickerFragment()
                    beatDetectorTitle -> BeatDetectorFragment()
                    equalizerTitle -> EqualizerFragment()
                    lightComposerTitle -> LightComposerFragment()
                    else -> AnimationPickerFragment()
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
        bleDisconnecting()
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