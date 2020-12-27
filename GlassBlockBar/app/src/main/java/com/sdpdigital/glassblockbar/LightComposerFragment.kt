package com.sdpdigital.glassblockbar

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import kotlin.math.roundToInt


/**
 * A fragment that allows the user to choose a color from the color wheel
 * to set a solid color on the wall
 */
class LightComposerFragment : Fragment() {

    val LOG_TAG: String? = (LightComposerFragment::class).simpleName

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    // One minute in milliseconds
    val perMinuteMillis = (1000L * 60L).toDouble()

    // Vars used for calculating bpm from user taps
    var lastTapTime: Long? = null
    var sum = 0L
    var tapCount = 0

    // UI
    var bpmButton: ImageButton? = null
    var bpmDelaySeekBar: AppCompatSeekBar? = null
    var bpmFineTuneSeekBar: AppCompatSeekBar? = null
    var bpmSeekBarTextView: TextView? = null
    var bpmDelaySeekBarTextView: TextView? = null
    var bpmResetButton: Button? = null

    // Main thread handler
    val mainHandler = Handler()
    val bpmTapStateRunnable = Runnable {
        bpmButton?.setImageResource(R.drawable.glass_block_big_clicked)
        mainHandler.postDelayed({
            bpmButton?.setImageResource(R.drawable.glass_block_big)
        }, 100)
    }
    val resetBpmTapCountRunnable = Runnable {
        resetBpmCounter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    // Ignore view override for accessibility for setting bpm button
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_light_composer, container, false)

        bpmSeekBarTextView = rootView.findViewById(R.id.bpm_fine_tune_text)
        bpmDelaySeekBarTextView = rootView.findViewById(R.id.bpm_delay_text)

        bpmButton = rootView.findViewById(R.id.bpm_button)
        bpmButton?.setOnTouchListener { view, event ->
            if (event.action == ACTION_DOWN) {
                val now = System.currentTimeMillis()
                lastTapTime?.let {
                    val diff = now - it
                    sum += diff
                    tapCount++

                    // Calculate new BPM estimate
                    val newBpm = (perMinuteMillis / (sum.toDouble() / tapCount.toDouble())).toInt()
                    bpmFineTuneSeekBar?.progress = newBpm

                    // Send BPM after a full measure of 4/4 music
                    if (tapCount % 4 == 0) {
                        sendBpm(newBpm)
                    }
                }
                lastTapTime = now

                // Run tap state animation
                mainHandler.post(bpmTapStateRunnable)
                // Reset tap info after 3 seconds of inactivity
                mainHandler.removeCallbacks(resetBpmTapCountRunnable)
                mainHandler.postDelayed(resetBpmTapCountRunnable, 3000)
            }
            return@setOnTouchListener true
        }

        bpmResetButton = rootView.findViewById(R.id.bpm_reset_button)
        bpmResetButton?.setOnClickListener {
            resetBpmCounter(true)
        }

        bpmFineTuneSeekBar = rootView.findViewById(R.id.seek_bar_bpm_fine_tune)
        bpmFineTuneSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                refreshUi()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let {
                    if (it < 20) {
                        resetBpmCounter()
                    } else {
                        sendBpm(it)
                    }
                }
            }
        })

        bpmDelaySeekBar = rootView.findViewById(R.id.seek_bar_bpm_delay)
        bpmDelaySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                refreshUi()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let {
                    val bpmOffsetMillis = seekBar.progress
                    Log.d(LOG_TAG, "Sent new bpm offset $bpmOffsetMillis")
                    val setBpmBytes = ByteArray(4)
                        { i -> arrayOf(1, 0, 0, bpmOffsetMillis)[i].toByte() }
                    glassBlockViewModel?.sendBpmInfo(setBpmBytes)
                }
            }
        })

        setupGlassBlockViewModel()

        return rootView
    }

    private fun refreshUi() {
        val delay = bpmDelaySeekBar?.progress
        bpmDelaySeekBarTextView?.text = "$delay ms BPM delay"
        val bpm = bpmFineTuneSeekBar?.progress
        bpmSeekBarTextView?.text = "$bpm BPM"
    }

    private fun resetBpmCounter(resetToZero: Boolean = false) {
        lastTapTime = null
        sum = 0L
        tapCount = 0
        if (resetToZero) {
            bpmDelaySeekBar?.progress = 0
            bpmFineTuneSeekBar?.progress = 0
            refreshUi()
        }
    }

    private fun sendBpmDelay(bpmOffsetMillis: Int) {
        Log.d(LOG_TAG, "Sent new bpm offset $bpmOffsetMillis")
        val setBpmBytes = ByteArray(4)
        { i -> arrayOf(1, 0, 0, bpmOffsetMillis)[i].toByte() }
        glassBlockViewModel?.sendBpmInfo(setBpmBytes)
    }

    private fun sendBpm(newBpm: Int) {
        Log.d(LOG_TAG, "Sent new bpm $newBpm")
        val setBpmBytes = ByteArray(4)
            { i -> arrayOf(0, 0, 0, newBpm)[i].toByte() }
        glassBlockViewModel?.sendBpmInfo(setBpmBytes)
    }

    private fun setupGlassBlockViewModel() {
        val app = activity?.application as? GlassBlockBarApplication
        app?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
            // Connection state observer
            val connectionObserver = Observer<ConnectionState> { connectionState  ->
                // Update the UI, in this case, a TextView.
                when(connectionState) {
                    ConnectionState.Disconnecting -> {
                        // TODO: go back to connection screen
                    }
                    else -> {
                        // no-op
                    }
                }
            }
            glassBlockViewModel?.connectionState?.observe(viewLifecycleOwner, connectionObserver)
        }
    }
}