package com.sdpdigital.glassblockbar

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.sdpdigital.glassblockbar.ble.Utils
import com.sdpdigital.glassblockbar.view.BeatSequenceView
import com.sdpdigital.glassblockbar.viewmodel.*
import kotlin.math.round

/**
 * A fragment that allows the user to choose a color from the color wheel
 * to set a solid color on the wall
 */
class LightComposerFragment : Fragment() {

    val LOG_TAG: String? = (LightComposerFragment::class).simpleName

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
    var bpmRecalibrateButton: Button? = null
    var timeSignatureTabs: TabLayout? = null
    var bpmIndicatorTextView: TextView? = null
    var addBeatSequenceButton: Button? = null
    var beatSequenceView: BeatSequenceView? = null

    // BPM Seek Bar Range
    private val bpmRange = 20
    // Each beat is divided into 16 equal pieces, see MIDI protocol for more info
    private val beatUnit = Utils.BeatDivision.SIXTEENTH

    private val timeSigs = arrayOf(
            TimeSignatureEnum.FOUR_FOUR,
            TimeSignatureEnum.EIGHT_EIGHT,
            TimeSignatureEnum.SIXTEEN_SIXTEEN)

    private fun indexOfTimeSig(timeSignature: TimeSignature): Int? {
        val index = timeSigs.indexOfFirst { it.value == timeSignature }
        if (index < 0) { return null }
        return index
    }

    private val app: GlassBlockLEDApplication? get() {
        return activity?.application as? GlassBlockLEDApplication
    }

    // Data Model
    private var lightComposerViewModel: LightComposerViewModel? = null

    // Main thread handler
    private val mainHandler = Handler()
    private val bpmIndicatorRunnable = Runnable {
        refreshBeatIndicatorTextAndScheduleRunnable()
    }
    private val resetBpmTapCountRunnable = Runnable {
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
                bpmButtonTapped(event.eventTime)
            }
            return@setOnTouchListener true
        }

        bpmResetButton = rootView.findViewById(R.id.bpm_reset_button)
        bpmResetButton?.setOnClickListener {
            resetBpmCounter()
            // Reset the BPM sliders to zero
            lightComposerViewModel?.bpmInfo?.postValue(BpmInfo())
        }

        // The re-calibrate button allows the user to fine-tune the BPM and send it
        // after performing the bpm calibrate tap
        bpmRecalibrateButton = rootView.findViewById(R.id.bpm_recalibrate_button)
        bpmRecalibrateButton?.setOnTouchListener { _, event ->
            event?.let {
                if (it.action == ACTION_DOWN) {
                    setBpmToSeekBar(it.eventTime)
                }
            }
            return@setOnTouchListener true
        }

        bpmFineTuneSeekBar = rootView.findViewById(R.id.seek_bar_bpm_fine_tune)
        bpmFineTuneSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                val newBpm = bpmFromProgress(progress)
                bpmSeekBarTextView?.text = "$newBpm BPM"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // No-op needed
            }
        })

        bpmDelaySeekBar = rootView.findViewById(R.id.seek_bar_bpm_delay)
        bpmDelaySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                bpmDelaySeekBarTextView?.text = "$progress ms BPM delay"
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let {
                    sendBpmDelay(seekBar.progress)
                }
            }
        })

        timeSignatureTabs = rootView.findViewById(R.id.tab_layout_beats_per_measure)
        timeSignatureTabs?.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) { /** No-op */ }
            override fun onTabUnselected(tab: TabLayout.Tab?) { /** No-op */ }
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let {
                    lightComposerViewModel?.setTimeSignature(timeSigs[it].value)
                } ?: run {
                    val tag = tab?.tag
                    Log.e(LOG_TAG,"Error processing tab $tag")
                }
            }
        })

        bpmIndicatorTextView = rootView.findViewById(R.id.bpm_indicator_text)

        addBeatSequenceButton = rootView.findViewById(R.id.button_add_beat_sequence)
        addBeatSequenceButton?.setOnTouchListener { _, event ->
            event?.let {
                if (it.action == ACTION_DOWN) {
                    addBeatToSequence(it.eventTime)
                }
            }
            return@setOnTouchListener true
        }

        rootView.findViewById<Button>(R.id.button_clear_beat_sequence)?.setOnClickListener {
            clearBeatSequence()
        }
        rootView.findViewById<Button>(R.id.button_undo_beat_sequence)?.setOnClickListener {
            beatSequenceView?.undoLastBeatSequence()
        }
        rootView.findViewById<Button>(R.id.send_to_animation_sf)?.setOnClickListener {
            sendBeatSequence(0)
        }
        rootView.findViewById<Button>(R.id.send_to_animation_rb)?.setOnClickListener {
            sendBeatSequence(1)
        }

        beatSequenceView = rootView.findViewById(R.id.beat_sequence_view)

        val quantizeLayout = rootView.findViewById<TabLayout>(R.id.tab_sync_per_beat_fraction)
        val startingTabIdx = Utils.BeatDivision.values().indexOf(Utils.BeatDivision.SIXTEENTH)
        quantizeLayout.getTabAt(startingTabIdx)?.select()
        quantizeLayout?.addOnTabSelectedListener(
                object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) { /** No-op */ }
            override fun onTabUnselected(tab: TabLayout.Tab?) { /** No-op */ }
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let {
                    beatSequenceView?.quantizationUnit = Utils.BeatDivision.values()[it]
                } ?: run {
                    val tag = tab?.tag
                    Log.e(LOG_TAG,"Error processing sync tab $tag")
                }
            }
        })

        // Data setup

        lightComposerViewModel = ViewModelProvider(this).get(LightComposerViewModel::class.java)
        lightComposerViewModel?.bpmInfo?.observe(viewLifecycleOwner, Observer<BpmInfo> {
            refreshUi()
        })

        refreshUi()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        currentBpmStartTime()?.let {
            currentBpm()?.let {
                mainHandler.post(bpmIndicatorRunnable)
            }
        }
        sendBeatControllerStart()
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(bpmIndicatorRunnable)
    }

    private fun bpmButtonTapped(touchTime: Long) {
        lastTapTime?.let {
            val diff = touchTime - it
            sum += diff
            tapCount++

            // Send BPM after a full measure of 4/4 music
            val beatNum = tapCount - 1;
            lightComposerViewModel?.bpmInfo?.value?.timeSignature?.beatsPerMeasure?.let { beatsPerMeasure ->
                if (beatNum % beatsPerMeasure == 0) {
                    // Calculate new BPM estimate from avg time between taps in micro seconds
                    val avgBeatInMicros = (sum.toDouble() / tapCount.toDouble()) * 1000
                    val newBpm = Utils.bpm(avgBeatInMicros)
                    lightComposerViewModel?.setBpm(newBpm, touchTime)
                }
            }
        }
        lastTapTime = touchTime

        // Reset tap info after 3 seconds of inactivity
        mainHandler.removeCallbacks(resetBpmTapCountRunnable)
        mainHandler.postDelayed(resetBpmTapCountRunnable, 3000)
    }

    private fun refreshUi() {
        val bpmInfo = lightComposerViewModel?.bpmInfo?.value
        bpmInfo?.signalDelay?.let {
            bpmDelaySeekBar?.progress = it
            bpmDelaySeekBarTextView?.text = "$it ms BPM delay"
        }
        bpmInfo?.timeSignature?.let { timeSig ->
            indexOfTimeSig(timeSig)?.let {
                timeSignatureTabs?.selectTab(timeSignatureTabs?.getTabAt(it))
            }
        }
        val bpm = bpmInfo?.bpm ?: 0
        bpmFineTuneSeekBar?.progress = bpm
        bpmSeekBarTextView?.text = "$bpm BPM"
        bpmInfo?.bpmRange?.let {
            bpmFineTuneSeekBar?.max = it.bpmRangeEnd - it.bpmRangeStart
            bpmFineTuneSeekBar?.progress = bpm - it.bpmRangeStart
        }
        beatSequenceView?.bpmInfo = bpmInfo
        addBeatSequenceButton?.isEnabled = bpm > 0
    }

    val debugCaptureMicros = ArrayList<Long>()
    val debugCaptureBeat = ArrayList<Int>()
    private fun refreshBeatIndicatorTextAndScheduleRunnable() {

        val bpm = currentBpm()
        val startTimeMillis = currentBpmStartTime() ?: return
        val timeSig = currentTimeSig() ?: return

        // See which beat we are on
        val nowMicros = (SystemClock.uptimeMillis() + 6) * 1000
        val startTimeMicros = startTimeMillis * 1000
        val currentDivisionalBeatWithinMeasure = Utils.quantizeBeatWithinMeasureToTruncate(
                Utils.BeatDivision.SIXTEENTH, timeSig,
                nowMicros, bpm, startTimeMicros)

        // This is done by the Arduino now
        //sendBeat(current16thBeatWithinMeasure)

        val wholeBeatsThrough = currentDivisionalBeatWithinMeasure.toDouble() /
                Utils.BeatDivision.SIXTEENTH.divisor.toDouble()

        val wholeBeatsThroughPlus1 = wholeBeatsThrough.toInt() + 1
        var indicatorText = ""
        for (i in 1 until wholeBeatsThroughPlus1) {
            indicatorText += "$i . "
            if (i % 4 == 0) { indicatorText += "\n" } // move to next line
        }
        indicatorText += "$wholeBeatsThroughPlus1 "
        if ((wholeBeatsThrough - wholeBeatsThrough.toInt()) >= 0.5) {
            indicatorText += ". "
        }
        bpmIndicatorTextView?.text = indicatorText
        beatSequenceView?.setCurrentBeat(currentDivisionalBeatWithinMeasure)

        if (currentDivisionalBeatWithinMeasure == 0) {
            var debugMicrosStr = ""
            var debugBeatsStr = ""
            for (i in 0 until debugCaptureBeat.size) {
                debugMicrosStr += debugCaptureMicros[i].toString() + ", "
                debugBeatsStr += debugCaptureBeat[i].toString() + ", "
            }
            Log.d("DEBUG_CAPTURE", "startTime $startTimeMicros")
            Log.d("DEBUG_CAPTURE", "bpm $bpm")
            Log.d("DEBUG_CAPTURE", "micros $debugMicrosStr")
            Log.d("DEBUG_CAPTURE", "beats $debugBeatsStr")
            debugCaptureMicros.clear()
            debugCaptureBeat.clear()
        }
        debugCaptureMicros.add(nowMicros)
        debugCaptureBeat.add(currentDivisionalBeatWithinMeasure)

        // Compute the delay to wait until next 16th beat
        val current16thMillis = (startTimeMicros +
                Utils.quantizeBeatToInMicros(beatUnit, nowMicros, bpm, startTimeMicros)) / 1000.0
        val a16thBeatInMillis = (Utils.microsInBeat(bpm) / Utils.BeatDivision.SIXTEENTH.divisor) / 1000.0
        val nextBeatTimeInMillis = current16thMillis + a16thBeatInMillis
        val delay = round(nextBeatTimeInMillis - SystemClock.uptimeMillis()).toLong()

        mainHandler.postDelayed(bpmIndicatorRunnable, delay - 5) // + 1 to avoid rounding errors
    }

    private var beatSeqMsgs: List<IntArray>? = null;

    private fun resetBpmCounter() {
        lastTapTime = null
        sum = 0L
        tapCount = 0
    }

    public fun sendBeatSequence(animationIndex: Int) {
        beatSequenceView?.getBeatSequence()?.let { sequence ->
            for (beatSeq in Utils.encodeBeatSequence(animationIndex, sequence)) {
                app?.writeBleMessage(ByteArray(20) {
                    return@ByteArray beatSeq[it].toByte()
                })
            }
        }
    }

    public fun sendBeatControllerStart() {
        app?.writeBleMessage(ByteArray(1) { 6 })
    }

    private fun sendNextBeatSeq() {
        val it = beatSeqMsgs?.firstOrNull() ?: run {
            return
        }
        val byteArray = ByteArray(it.size) { i -> it[i].toByte() }
        app?.writeBleMessage(byteArray)
    }

    private fun clearBeatSequence() {
        beatSequenceView?.clearBeatSequence()
    }

    private fun addBeatToSequence(beatTimeInMs: Long) {
        beatSequenceView?.addBeat(beatTimeInMs)
    }

    private fun setBpmToSeekBar(startTime: Long) {
        sendBpm(currentBpm(), startTime, true)
    }

    private fun currentBpmStartTime(): Long? {
        return lightComposerViewModel?.bpmInfo?.value?.bpmStartTime
    }

    private fun currentTimeSig(): TimeSignature? {
        return lightComposerViewModel?.bpmInfo?.value?.timeSignature
    }

    private fun currentBpm(): Int {
        val rangeStart = lightComposerViewModel?.bpmInfo?.value?.bpmRange?.bpmRangeStart
        return (bpmFineTuneSeekBar?.progress ?: 0) + (rangeStart ?: 0)
    }

    private fun currentBpmDelay(): Int {
        return bpmDelaySeekBar?.progress ?: 0
    }

    private fun bpmFromProgress(progress: Int): Int {
        lightComposerViewModel?.bpmInfo?.value?.bpmRange?.bpmRangeStart?.let {
            return progress + it
        }
        return 0
    }

    private fun sendBpmDelay(bpmOffsetMillis: Int) {
        var beatsPerMeasure = currentTimeSig()?.beatsPerMeasure ?: run {
            return
        }

        Log.d(LOG_TAG, "Sent new bpm offset $bpmOffsetMillis")
        val setBpmBytes = ByteArray(4)
            { i -> arrayOf(5, beatsPerMeasure, currentBpm(), currentBpmDelay())[i].toByte() }
        app?.writeBleMessage(setBpmBytes)
        lightComposerViewModel?.setBpmDelay(bpmOffsetMillis)
    }

    private fun sendBpm(newBpm: Int, startTime: Long, resetRange: Boolean = false) {
        var beatsPerMeasure = currentTimeSig()?.beatsPerMeasure ?: run {
            return
        }

        Log.d(LOG_TAG, "Sent new bpm $newBpm")
        val setBpmBytes = ByteArray(4)
            { i -> arrayOf(5, beatsPerMeasure, currentBpm(), currentBpmDelay())[i].toByte() }
        app?.writeBleMessage(setBpmBytes)

        if (resetRange) {
            lightComposerViewModel?.setBpmAndRange(newBpm, startTime,
                    BpmRange(newBpm - bpmRange, newBpm + bpmRange))
        } else {
            lightComposerViewModel?.setBpm(newBpm, startTime)
        }

        // Restart Runnable
        mainHandler.removeCallbacks(bpmIndicatorRunnable)
        mainHandler.post(bpmIndicatorRunnable)
    }

    private fun registerTap(eventTime: Long, toInstrument: Int) {

    }
}