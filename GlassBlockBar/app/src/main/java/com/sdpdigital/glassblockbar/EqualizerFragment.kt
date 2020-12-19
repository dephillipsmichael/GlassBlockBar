package com.sdpdigital.glassblockbar

import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.sdpdigital.glassblockbar.beatdetection.InstrumentPanel
import com.sdpdigital.glassblockbar.ble.Utils
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import org.hermit.android.instruments.SpectrumGauge
import org.hermit.android.instruments.SpectrumGauge.OnBeatDetectedListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [LEDFunctionListActivity]
 * in two-pane mode (on tablets) or a [LEDFunctionDetailActivity]
 * on handsets.
 */
class EqualizerFragment : Fragment() {

    val LOG_TAG: String? = (EqualizerFragment::class).simpleName

    // Main thread handler
    val mainHandler = Handler()

    // View for recording and analyzing audio
    var instrumentPanel: InstrumentPanel? = null

    // Contains the equalizer views to show on android device
    var eqViews = Array<Array<View?>>(9) { Array(5) { null } }
    var eqRoot: LinearLayout? = null

    val EQ_ROWS = 5
    val EQ_COLS = 9

    // Set of current low, mid, high values
    var intensityFrameSum = Array<Double>(EQ_COLS) { 0.0 } // Initialize all to 0
    var maxFrameIntensities = Array<Double>(EQ_COLS) { 0.0 } // Initialize all to 0

    // Glass Blocks only get 80 fps, so only update at about 30 fps
    var lastFpsTime = System.currentTimeMillis()
    var frameTimeInMillis = 1000f / 20f
    var forceBeatSend = false

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    val clearColor = Color.parseColor("#00ffffff")
    val blueColor = Color.parseColor("#0000ff")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_equalizer, container, false)

        // Get root layout
        val relativeRoot = rootView.findViewById<RelativeLayout>(R.id.item_detail_root_layout)
        eqRoot = rootView.findViewById<LinearLayout>(R.id.equalizer_root)

        // Let the eq draw its full width/height first
        eqRoot?.post() {
            eqRoot?.let {
                val blockWidth = it.width / EQ_COLS
                val blockHeight = it.height / EQ_ROWS
                for (col in 0 until EQ_COLS) {
                    val linearLayout = LinearLayout(activity)
                    linearLayout.orientation = LinearLayout.VERTICAL
                    linearLayout.layoutParams = LinearLayout.LayoutParams(
                            blockWidth,
                            LinearLayout.LayoutParams.MATCH_PARENT)
                    it.addView(linearLayout)
                    for (row in 0 until EQ_ROWS) {
                        val eqView = View(activity)
                        eqView.layoutParams = LinearLayout.LayoutParams(blockWidth, blockHeight)
                        if (row == 4) {
                            eqView.setBackgroundColor(
                                    Color.argb(255, 158, 0, 255))
                        } else if (row == 3) {
                            eqView.setBackgroundColor(
                                    Color.argb(255, 0, 255, 198))
                        } else if (row == 2) {
                            eqView.setBackgroundColor(
                                    Color.argb(255, 255, 194, 9))
                        } else if (row == 1) {
                            eqView.setBackgroundColor(
                                    Color.argb(255, 255, 0, 127))
                        } else if (row == 0) {
                            eqView.setBackgroundColor(
                                    Color.argb(255, 255, 255, 255))
                        }
                        eqViews[col][row] = eqView
                        linearLayout.addView(eqView)
                    }
                }
            }
        }

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

        setupGlassBlockViewModel()

        return rootView
    }

    fun randomColor(): Int {
        val rnd = Random()
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    }

    override fun onStart() {
        super.onStart()

        // Initialize the microphone
        instrumentPanel?.setInstruments(InstrumentPanel.Instruments.SPECTRUM) // Initialize the microphone
        // Start audio processing
        instrumentPanel?.setOnFftResultListener(onFftResultListener)
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

    private fun setupGlassBlockViewModel() {
        val app = activity?.application as? GlassBlockBarApplication
        app?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
        }
    }

    private val onBeatDetectedListener: SpectrumGauge.OnBeatDetectedListener = object : SpectrumGauge.OnBeatDetectedListener {
        override fun onHighBeatDetectedOn(intensity: Float) {
            mainHandler.postDelayed({
                forceBeatSend = true
            }, 5) // 10 ms delay to get big intensities
        }

        override fun onBeatDetectedOff() {
            forceBeatSend = true
        }

        override fun onLowBeatDetectedOn(intensity: Float) {
            mainHandler.postDelayed({
                forceBeatSend = true
            }, 5) // 10 ms delay to get big intensities
        }

        override fun onHighBeatDetectedOff() {
            forceBeatSend = true
        }

        override fun onLowBeatDetectedOff() {
            forceBeatSend = true
        }

        override fun onMidBeatDetectedOn(intensity: Float) {
            mainHandler.postDelayed({
                forceBeatSend = true
            }, 5) // 10 ms delay to get big intensities
        }

        override fun onMidBeatDetectedOff() {
            forceBeatSend = true
        }

        override fun onBeatDetectedOn(intensity: Float) {
            mainHandler.postDelayed({
                forceBeatSend = true
            }, 5) // 10 ms delay to get big intensities
        }
    }

    /*
     * Listener for when beats occur in the audio analyser
     */
    private val onFftResultListener: SpectrumGauge.OnFFTResultListener = SpectrumGauge.OnFFTResultListener { fft ->
        mainHandler.post {
            //eqDrawing(fft)
            fft?.let {
                for ((idx, intensity) in fft.withIndex()) {
                    intensityFrameSum[idx] += intensity
                }
            }

            //val logVals = fft.map { "$it, " }
            //Log.d(LOG_TAG, "FFT: $logVals")

            // Check if a frame has gone by and we can send to the glass wall
            val now = System.currentTimeMillis()
            if (((now - lastFpsTime) > frameTimeInMillis) || forceBeatSend) {
                lastFpsTime = now

                var normalized = Array(intensityFrameSum.size) { 0 }

                // Reset max intensities to base eq height off of
                for ((col, intensity) in intensityFrameSum.withIndex()) {
                    maxFrameIntensities[col] = max(intensity, maxFrameIntensities[col])
                    // Normalize intensities to 0...4 inclusive
                    normalized[col] = (((intensity.toFloat() / (maxFrameIntensities[col] / 1.2)).toFloat()) * 6).toInt()

                    // Draw a frame of the EQ on android device
                    for (row in 0 until EQ_ROWS) {
                        val rowAdjusted = (EQ_ROWS - 1) - row
                        if (normalized[col] > row) {
                            eqViews[col][rowAdjusted]?.visibility = View.VISIBLE
                        } else {
                            eqViews[col][rowAdjusted]?.visibility = View.INVISIBLE
                        }
                    }
                }

                // Send data to glass block wall
                // We need to convert the data a minimal format, 3 bits per signal (0 to 4)
//                Utils.convertEqBytesToMinimalBytes(normalized)?.let {
//
////                    val debug2 = normalized.map { "$it, " }
////                    Log.d("BYTE_TEST", "Normalized sent $debug2")
////
////                    val debug = it.map { "${it.toUByte()}, " }
////                    Log.d("BYTE_TEST", "Bytes sent $debug")
//
//                    glassBlockViewModel?.sendBitshiftEqualizer(
//                            ByteArray(4) { i -> it[i].toByte() })
//                }

                glassBlockViewModel?.sendEqualizer(
                        ByteArray(9) { i -> normalized[i].toByte() })

                Arrays.fill(intensityFrameSum, 0.0)
            }
        }
    }


    // Attempt at a logorithmic EQ
//    fun eqDrawing(fft: FloatArray) {
//
//        var xVals = ArrayList<Float>()
//        var yVals = ArrayList<Float>()
//
//        eqRoot?.let {
//            var spectGraphWidth = it.width.toFloat()
//            var spectGraphHeight = it.height.toFloat()
//            var spectGraphY = it.y.toFloat()
//            var spectGraphX = it.x.toFloat()
//
//            instrumentPanel?.nyquestFreq?.let {
//                val nyquistFreq = it
//
//                val len: Int = fft.size
//                val bw = (spectGraphWidth - 2) as Float / len.toFloat()
//                val bh: Float = spectGraphHeight - 2
//                val be: Float = spectGraphY + spectGraphHeight - 1
//
//                // Determine the first and last frequencies we have.
//
//                // Determine the first and last frequencies we have.
//                val lf: Float = nyquistFreq / len.toFloat()
//                val rf: Float = nyquistFreq.toFloat()
//
//                // Now, how many octaves is that.  Round down.  Calculate pixels/oct.
//
//                // Now, how many octaves is that.  Round down.  Calculate pixels/oct.
//                val octaves = floor(log2(rf / lf.toDouble())).toInt() - 2
//                val octWidth = (spectGraphWidth - 2) as Float / octaves.toFloat()
//
//                // Calculate the base frequency for the graph, which isn't lf.
//
//                // Calculate the base frequency for the graph, which isn't lf.
//                val bf = rf / 2.0.pow(octaves.toDouble()).toFloat()
//
//                // Element 0 isn't a frequency bucket; skip it.
//
//                // Element 0 isn't a frequency bucket; skip it.
//                for (i in 1 until len) {
//                    // What frequency bucket are we in.
//                    val f = lf * i
//
//                    // For freq f, calculate x.
//                    val x: Float = spectGraphX + (log2(f.toDouble()) -
//                            log2(bf.toDouble())).toFloat() * octWidth
//
//                    // Draw the bar.
//                    var y = be - (log10(fft[i]) / RANGE_BELS + 1f).toFloat() * bh
//                    if (y > be) {
//                        y = be
//                    } else if (y < spectGraphY) {
//                        y = spectGraphY
//                    }
//
//                    xVals.add(x)
//                    yVals.add(y)
//                }
//            }
//
//            val desiredBuckets = 9
//            val xBucketWidth = (xVals.size / desiredBuckets)
//            val yBuckets = Array<ArrayList<Float>>(desiredBuckets) { ArrayList<Float>() }
//            for (x in xVals.withIndex()) {
//                val idx = min((x.index / xBucketWidth), desiredBuckets-1)
//                yBuckets[idx].add(yVals[x.index])
//            }
//
//            val normalized = Array<Int>(desiredBuckets) { 0 }
//            for (i in 0 until desiredBuckets) {
//                val avg = yBuckets[i].sum() / yBuckets[i].size
//                // Normalize intensities to 0...4 inclusive
//                normalized[i] = (((avg / spectGraphHeight)) * 6).toInt()
//            }
//
//            // Draw a frame of the EQ on android device
//            for ((col, intensity) in normalized.withIndex()) {
//                for (row in 0 until EQ_ROWS) {
//                    val rowAdjusted = (EQ_ROWS - 1) - row
//                    if (normalized[col] > row) {
//                        eqViews[col][rowAdjusted]?.setBackgroundColor(randomColor())
//                    } else {
//                        eqViews[col][rowAdjusted]?.setBackgroundColor(clearColor)
//                    }
//                }
//            }
//
//            val debugStr = normalized.map{ " $it, " }
//            Log.d("TODO_REMOVE", debugStr.joinToString())
//        }
//    }

    // Log of 2.
    private val LOG2 = Math.log(2.0)
    // Vertical range of the graph in bels.
    private val RANGE_BELS = 6f
    private fun log2(x: Double): Double {
        return Math.log(x) / LOG2
    }
}