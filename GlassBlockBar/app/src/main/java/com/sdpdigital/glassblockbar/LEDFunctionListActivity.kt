package com.sdpdigital.glassblockbar

import android.bluetooth.BluetoothDevice
import android.content.pm.ActivityInfo
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

    val durationBetweenSends = 60;  // 60 millis
    var lastColorSendTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

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

    private fun setupBrightnessSlider(brightnessSlider: AppCompatSeekBar) {

        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, p1: Int, p2: Boolean) {
                sendGlobalBrightness(seekbar?.progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                // No-op needed
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                // No-op needed
            }
        })

        brightnessSlider.progress = brightnessSlider.max - 1
    }

    private fun bleDisconnecting() {
        finish()  // goes back to connection screen
    }

    private fun sendGlobalBrightness(brightness: Int?) {
        if (brightness == null) {
            return
        }

        val now = System.currentTimeMillis()
        if ((now - lastColorSendTime) < durationBetweenSends) {
            return
        }
        lastColorSendTime = now

        Log.d(LOG_TAG, "Sent new brightness $brightness")
        val brightnessOnlyARGB = ByteArray(4)
        { i -> arrayOf(0, brightness, 0, 0, 0)[i].toByte() }
        glassBlockViewModel?.sendARGB(brightnessOnlyARGB)
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