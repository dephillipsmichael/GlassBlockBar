package com.sdpdigital.glassblockbar

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.material.tabs.TabLayout
import com.sdpdigital.glassblockbar.ble.Utils
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
    var bpmLeftButton: ImageButton? = null
    var bpmRightButton: ImageButton? = null
    var bpmDelaySeekBar: AppCompatSeekBar? = null
    var bpmFineTuneSeekBar: AppCompatSeekBar? = null
    var bpmSeekBarTextView: TextView? = null
    var bpmDelaySeekBarTextView: TextView? = null
    var bpmResetButton: Button? = null
    var bpmRecalibrateButton: Button? = null
    var timeSignatureTabs: TabLayout? = null
    var bpmIndicatorTextView: TextView? = null

    // BPM Seek Bar Range
    private val bpmRange = 20
    // Each beat is divided into 24 equal pieces, see MIDI protocol for more info
    private val beatDivision = 24

    private val timeSigs = arrayOf(
            TimeSignatureEnum.FOUR_FOUR,
            TimeSignatureEnum.EIGHT_EIGHT,
            TimeSignatureEnum.SIXTEEN_SIXTEEN)

    private fun indexOfTimeSig(timeSignature: TimeSignature): Int? {
        val index = timeSigs.indexOfFirst { it.value == timeSignature }
        if (index < 0) { return null }
        return index
    }

    // Data Model
    private var glassBlockViewModel: GlassBlockBarViewModel? = null
    private var lightComposerViewModel: LightComposerViewModel? = null

    // Main thread handler
    private val mainHandler = Handler()
    private val bpmLeftTapStateRunnable = Runnable {
//        bpmLeftButton?.setImageResource(R.drawable.glass_block_big_clicked)
//        mainHandler.postDelayed({
//            bpmLeftButton?.setImageResource(R.drawable.glass_block_big)
//        }, 100)
    }
    private val bpmRightTapStateRunnable = Runnable {
//        bpmRightButton?.setImageResource(R.drawable.glass_block_big_clicked)
//        mainHandler.postDelayed({
//            bpmRightButton?.setImageResource(R.drawable.glass_block_big)
//        }, 100)
    }
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

        bpmLeftButton = rootView.findViewById(R.id.bpm_left_button)
        bpmLeftButton?.setOnTouchListener { view, event ->
            if (event.action == ACTION_DOWN) {
                bpmButtonTapped(event.eventTime, true)
            }
            return@setOnTouchListener true
        }

        bpmRightButton = rootView.findViewById(R.id.bpm_right_button)
        bpmRightButton?.setOnTouchListener { _, event ->
            if (event.action == ACTION_DOWN) {
                bpmButtonTapped(event.eventTime, false)
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


        // Data setup

        lightComposerViewModel = ViewModelProvider(this).get(LightComposerViewModel::class.java)
        lightComposerViewModel?.bpmInfo?.observe(viewLifecycleOwner, Observer<BpmInfo> {
            refreshUi()
        })
        // Glass block model is shared globally, so access it through app life cycle owner
        val app = activity?.application as? GlassBlockBarApplication
        app?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
        }

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
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(bpmIndicatorRunnable)
    }

    private fun bpmButtonTapped(touchTime: Long, isLeftButton: Boolean) {
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

        // Run tap state animation
        if (isLeftButton) {
            mainHandler.post(bpmLeftTapStateRunnable)
        } else { // if is right button
            mainHandler.post(bpmRightTapStateRunnable)
        }
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
    }

    private fun refreshBeatIndicatorTextAndScheduleRunnable() {
        currentBpm()?.let { bpm ->
            currentBpmStartTime()?.let { startTime ->
                // See which beat we are on
                val nowMicros = SystemClock.uptimeMillis() * 1000
                val startTimeMicros = startTime * 1000
                val beatsElapsed = Utils.beatsElapsed(nowMicros, bpm, startTimeMicros)
                val beatRemainder = beatsElapsed - beatsElapsed.toInt()

                // Check beats through the measure
                val beatsPerMeasure = (currentTimeSig() ?: TimeSignatureEnum.FOUR_FOUR.value).beatsPerMeasure
                val beatThroughMeasure = ((beatsElapsed % beatsPerMeasure) + 1).toInt()

                var indicatorText = ""
                for (i in 1 until beatThroughMeasure) {
                    indicatorText += "$i . "
                    if (i % 4 == 0) { indicatorText += "\n" } // move to next line
                }
                indicatorText += "$beatThroughMeasure "
                if (beatRemainder >= 0.5) {
                    indicatorText += ". "
                }
                bpmIndicatorTextView?.text = indicatorText

                val millisInBeat = Utils.microsInBeat(bpm) / 1000.0
                val nextHalfBeat = startTime + if (beatRemainder >= 0.5) {
                    (millisInBeat * (beatsElapsed.toInt() + 1)).toLong()
                } else {
                    (millisInBeat * (beatsElapsed.toInt() + 0.5)).toLong()
                }
                mainHandler.postDelayed(bpmIndicatorRunnable, nextHalfBeat - SystemClock.uptimeMillis())
            }
        }
    }

    private fun resetBpmCounter() {
        lastTapTime = null
        sum = 0L
        tapCount = 0
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

    private fun bpmFromProgress(progress: Int): Int {
        lightComposerViewModel?.bpmInfo?.value?.bpmRange?.bpmRangeStart?.let {
            return progress + it
        }
        return 0
    }

    private fun sendBpmDelay(bpmOffsetMillis: Int) {
        Log.d(LOG_TAG, "Sent new bpm offset $bpmOffsetMillis")
        val setBpmBytes = ByteArray(4)
        { i -> arrayOf(1, 0, 0, bpmOffsetMillis)[i].toByte() }
        glassBlockViewModel?.sendBpmInfo(setBpmBytes)
        lightComposerViewModel?.setBpmDelay(bpmOffsetMillis)
    }

    private fun sendBpm(newBpm: Int, startTime: Long, resetRange: Boolean = false) {
        Log.d(LOG_TAG, "Sent new bpm $newBpm")
        val setBpmBytes = ByteArray(4)
            { i -> arrayOf(0, 0, 0, newBpm)[i].toByte() }
        glassBlockViewModel?.sendBpmInfo(setBpmBytes)

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