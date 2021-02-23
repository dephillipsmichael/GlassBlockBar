package com.sdpdigital.glassblockbar

import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import com.sdpdigital.glassblockbar.beatdetection.InstrumentPanel
import org.hermit.android.instruments.SpectrumGauge
import kotlin.math.max
import kotlin.math.min

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [LEDFunctionListActivity]
 * in two-pane mode (on tablets) or a [LEDFunctionDetailActivity]
 * on handsets.
 */
class BeatDetectorFragment : Fragment() {

    val LOG_TAG: String? = (BeatDetectorFragment::class).simpleName

    /**
     * The dummy content this fragment is presenting.
     */
    private var item: String? = null

    // Main thread handler
    val mainHandler = Handler()

    // View for recording and analyzing audio
    var instrumentPanel: InstrumentPanel? = null

    // Beat indicator views
    var lowBeatView: View? = null
    var midBeatView: View? = null
    var highBeatView: View? = null

    // Set of current low, mid, high values
    val LOW_IDX = 0
    val MID_IDX = 1
    val HIGH_IDX = 2

    // Current beat intensities in low, mid, and high freq bands.
    var lowMidHighValues = arrayOf(0, 0, 0);
    var highestLowMidHighIntensities = arrayOf(0f, 0f, 0f)

    // Used to throttle beats
    var canSendBeat = arrayOf(true, true, true)

    var lastResetOfHighs = System.currentTimeMillis()
    val resetDuration = 2000 // every 2 seconds
    var runningWindowHighestLowMidHighIntensities = arrayOf(0f, 0f, 0f)

    private val app: GlassBlockLEDApplication? get() {
        return activity?.application as? GlassBlockLEDApplication
    }

    val seekBarProgress = arrayOf(0f, 0f, 0f)
    var lowsSeekBar: AppCompatSeekBar? = null
    var midsSeekBar: AppCompatSeekBar? = null
    var highsSeekBar: AppCompatSeekBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_beat_detector, container, false)

        // Get root layout
        val relativeRoot = rootView.findViewById<RelativeLayout>(R.id.item_detail_root_layout)

        val progressListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, p2: Boolean) {
                (seekbar?.tag as? Int)?.let {
                    val newProgress = (progress.toFloat() / (seekbar.max.toFloat()))
                    seekBarProgress[it] = newProgress
                    Log.d(LOG_TAG,"Seek bar $it changed to $newProgress")
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?)  { /** no-op */ }
            override fun onStopTrackingTouch(p0: SeekBar?) { /** no-op */ }
        }

        lowsSeekBar = rootView.findViewById(R.id.seek_bar_lows)
        lowsSeekBar?.tag = LOW_IDX
        lowsSeekBar?.setOnSeekBarChangeListener(progressListener)
        midsSeekBar = rootView.findViewById(R.id.seek_bar_mids)
        midsSeekBar?.tag = MID_IDX
        midsSeekBar?.setOnSeekBarChangeListener(progressListener)
        highsSeekBar = rootView.findViewById(R.id.seek_bar_highs)
        highsSeekBar?.tag = HIGH_IDX
        highsSeekBar?.setOnSeekBarChangeListener(progressListener)

        // We want the audio controls to control our sound volume.
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC

        activity?.let {
            // Creating a LinearLayout.LayoutParams object for text view
            val layout = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT)
            instrumentPanel = InstrumentPanel(it)
            instrumentPanel?.layoutParams = layout
            relativeRoot.addView(instrumentPanel, 0)
        }

        lowBeatView = rootView.findViewById(R.id.low_beat_view)
        midBeatView = rootView.findViewById(R.id.mid_beat_view)
        highBeatView = rootView.findViewById(R.id.high_beat_view)

        return rootView
    }

    override fun onStart() {
        super.onStart()

        // Initialize the microphone
        instrumentPanel?.setInstruments(InstrumentPanel.Instruments.SPECTRUM) // Initialize the microphone
        // Start audio processing
        instrumentPanel?.setOnBeatDetectedListener(onBeatDetectedListener)
        instrumentPanel?.onStart()
    }

    override fun onResume() {
        super.onResume()

        instrumentPanel?.onResume()
        instrumentPanel?.surfaceStart()
    }


    override fun onPause() {
        super.onPause()

        instrumentPanel?.onPause()
    }

    override fun onStop() {
        super.onStop()
        // Stop audio analysis
        instrumentPanel?.onStop()
    }

    // Assess the new beat intensity,
    // to possibly set a new max,
    // @return a value [0...255] to send to BLE device
    fun convertIntensityTo255(intensity: Float, maxIdx: Int): Int {
        if (intensity <= 0) {
            return 0
        }

        if (intensity > runningWindowHighestLowMidHighIntensities[maxIdx]) {
            runningWindowHighestLowMidHighIntensities[maxIdx] = intensity
        }

        // Reset to relate 4 window high intensities
        val now = System.currentTimeMillis()
        if ((now - lastResetOfHighs) > resetDuration) {
            for (i in 0 .. HIGH_IDX) {
                highestLowMidHighIntensities[i] = runningWindowHighestLowMidHighIntensities[i]
                runningWindowHighestLowMidHighIntensities[i] = 0f
            }
            lastResetOfHighs = now
        }

        val normalizedFactor = intensity / highestLowMidHighIntensities[maxIdx]
        val byteScaled = max(min((normalizedFactor * 255).toInt(), 255), 0)

        return byteScaled
    }

    fun processIntensity(intensity: Float, lowMidHighIdx: Int): Boolean {
        val byteIntensity = convertIntensityTo255(intensity, lowMidHighIdx)
        lowMidHighValues[lowMidHighIdx] = byteIntensity

        if (canSendBeat[lowMidHighIdx]) {
            canSendBeat[lowMidHighIdx] = false
            val newLowMidHigh = ByteArray(3) { idx -> lowMidHighValues[idx].toByte() }
            app?.writeBleMessage(newLowMidHigh)
            return true
        }
        return false
    }

    /*
     * Listener for when beats occur in the audio analyser
     */
    private val onBeatDetectedListener: SpectrumGauge.OnBeatDetectedListener = object : SpectrumGauge.OnBeatDetectedListener {

        // This is total volume intensity beats
        override fun onBeatDetectedOn(intensity: Float) {
//            if (intensity > mThresholdValue) {
//                mHandler.post(Runnable {
//                    mAvgDelayCtr++
//                    if (mAvgDelayCtr >= 8) {
//                        if ((System.currentTimeMillis() - mAvgDelayStartTime) as Int > 1000) {
//                            mAvgDelay = (System.currentTimeMillis() - mAvgDelayStartTime) as Int
//                        }
//                        mAvgDelayStartTime = System.currentTimeMillis()
//                        mAvgDelayCtr = 0
//                    }
//                    (findViewById<View>(R.id.txt_all) as TextView).text = "All: $intensity"
//                    if (mReadyToStart && mReadyForNextMove) {
//                        cycleDanceMoves()
//                        mReadyForNextMove = false
//                    }
//                })
//            }
        }

        // This is total volume intensity beats
        override fun onBeatDetectedOff() {

        }

        override fun onLowBeatDetectedOn(intensity: Float) {
            mainHandler.post {
                val threshold = seekBarProgress[LOW_IDX] *
                        highestLowMidHighIntensities[LOW_IDX]
                if (intensity > threshold ) {
                    if (processIntensity(intensity, LOW_IDX)) {
                        lowBeatView?.visibility = View.VISIBLE
                        mainHandler.postDelayed({
                            lowBeatView?.visibility = View.INVISIBLE
                            canSendBeat[LOW_IDX] = true
                        }, 200)
                    }
                }
                Log.d(LOG_TAG, "Low: $intensity")
            }

//            if (intensity * 10000 > mThresholdValue) {
//                mainHandler.post({
//                    (findViewById<View>(R.id.txt_low) as TextView).text = "Low: " + intensity * 10000
//                })
//            }
        }

        override fun onLowBeatDetectedOff() {
            mainHandler.post {
                processIntensity(0f, LOW_IDX)
                lowBeatView?.visibility = View.INVISIBLE
            }
        }

        override fun onMidBeatDetectedOn(intensity: Float) {
            mainHandler.post {
                Log.d(LOG_TAG, "Mid: $intensity")
                val threshold = seekBarProgress[MID_IDX] *
                        highestLowMidHighIntensities[MID_IDX]
                if (intensity > threshold ) {
                    if (processIntensity(intensity, MID_IDX)) {
                        midBeatView?.visibility = View.VISIBLE
                        mainHandler.postDelayed({
                            midBeatView?.visibility = View.INVISIBLE
                            canSendBeat[MID_IDX] = true
                        }, 200)
                    }
                }
            }
//            if (intensity * 10000 > mThresholdValue) {
//                mHandler.post(Runnable { (findViewById<View>(R.id.txt_mid) as TextView).text = "Mid: " + intensity * 10000 })
//            }
        }

        override fun onMidBeatDetectedOff() {
            mainHandler.post {
                processIntensity(0f, MID_IDX)
                midBeatView?.visibility = View.INVISIBLE
            }
        }

        override fun onHighBeatDetectedOn(intensity: Float) {
            mainHandler.post {
                Log.d(LOG_TAG, "High: $intensity")
                val threshold = seekBarProgress[HIGH_IDX] *
                        highestLowMidHighIntensities[HIGH_IDX]
                if (intensity > threshold ) {
                    if (canSendBeat[HIGH_IDX]) {
                        if (processIntensity(intensity, HIGH_IDX)) {
                            highBeatView?.visibility = View.VISIBLE
                            mainHandler.postDelayed({
                                highBeatView?.visibility = View.INVISIBLE
                                canSendBeat[HIGH_IDX] = true
                            }, 200)
                        }
                    }
                }
            }
//            if (intensity * 10000 > mThresholdValue) {
//                mHandler.post(Runnable { (findViewById<View>(R.id.txt_hi) as TextView).text = "High: " + intensity * 10000 })
//            }
        }

        override fun onHighBeatDetectedOff() {
            mainHandler.post {
                processIntensity(0f, HIGH_IDX)
                highBeatView?.visibility = View.INVISIBLE
            }
        }
    }
}