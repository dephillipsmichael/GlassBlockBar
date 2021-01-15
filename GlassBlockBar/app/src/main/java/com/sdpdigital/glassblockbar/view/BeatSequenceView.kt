package com.sdpdigital.glassblockbar.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.sdpdigital.glassblockbar.ble.Utils
import com.sdpdigital.glassblockbar.viewmodel.BpmInfo
import kotlin.math.round

open class BeatSequenceView: View {

    private val LOG_TAG = BeatSequenceView::class.java.simpleName

    constructor(context: Context) :
            super(context, null) {
        commonInit()
    }
    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs, 0) {
        commonInit()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        commonInit()
    }

    public var bpmInfo: BpmInfo? = null
        set(value) {
            field = value
            invalidate()
        }

    // Switched over to 16th beat counting to give the animation more time to run
    private val total16thBeats: Int get() {
        bpmInfo?.let {
            return it.timeSignature.beatsPerMeasure * Utils.BeatDivision.SIXTEENTH.divisor
        }
        return 0
    }

    // The smallest divisor unit is 24th, so that is what we use to count
//    private val total24thBeats: Int get() {
//        bpmInfo?.let {
//            return it.timeSignature.beatsPerMeasure * Utils.BeatDivision.TWENTY_FOURTH.divisor
//        }
//        return 0
//    }

    // You can change where the user's taps get quantized to on the fly
    public var quantizationUnit = Utils.BeatDivision.SIXTEENTH
        set(value) {
            field = value

            quantizedBeatSequenceIn16th.clear()
            // Refresh beat sequence to new quantization
            for (beats in beatsThroughMeasure) {
                processBeatsThroughMeasure(beats)
            }

            // Re-draw
            invalidate()
        }

    // Raw user event tap translated to percent through measure
    private var beatsThroughMeasure = ArrayList<Double>()
    private var quantizedBeatSequenceIn16th = ArrayList<Int>()
    public fun getBeatSequence(): List<Int> {
        return quantizedBeatSequenceIn16th
    }

    /**
     * Clear the beat sequence for a fresh start
     */
    public fun clearBeatSequence() {
        beatsThroughMeasure.clear()
        quantizedBeatSequenceIn16th.clear()
    }

    /**
     * Undo the last beat tapped
     */
    public fun undoLastBeatSequence() {
        if (beatsThroughMeasure.isNotEmpty()) {
            beatsThroughMeasure.removeAt(beatsThroughMeasure.size - 1)
            quantizedBeatSequenceIn16th.removeAt(quantizedBeatSequenceIn16th.size - 1)
        }
    }

    /**
     * Add a beat to the sequence, it will be quantized to whatever is set as quantizationUnit
     */
    public fun addBeat(beatTimeInMillis: Long) {
        Log.d(LOG_TAG, "Adding raw time $beatTimeInMillis")

        val bpm = bpmInfo?.bpm?.let { it } ?: return
        val bpmStartTimeMillis = bpmInfo?.bpmStartTime?.let { it } ?: return
        val timeSig = bpmInfo?.timeSignature?.let { it } ?: return

        val beatMicros = beatTimeInMillis * 1000L
        val startTimeMicros = bpmStartTimeMillis * 1000L
        val beatsElapsed = Utils.beatsElapsed(beatMicros, bpm, startTimeMicros)
        val beats = Utils.beatsThroughMeasure(timeSig, beatsElapsed)

        beatsThroughMeasure.add(beats)
        processBeatsThroughMeasure(beats)
    }

    private fun processBeatsThroughMeasure(beatsThroughMeasure: Double) {
        val bpm = bpmInfo?.bpm?.let { it } ?: return
        val timeSig = bpmInfo?.timeSignature?.let { it } ?: return

        // Start time is always 0 because we already transposed to per measure timing with beatsThroughMeasure
        val startTimeMicros = 0L

        // Get the micros of the beat through the measure
        val beatTimeThroughMeasure = round(beatsThroughMeasure * Utils.microsInBeat(bpm)).toLong()

        // First convert to target quantization
        val quantizedTimeInMicros = Utils.quantizeBeatToInMicros(
                quantizationUnit, beatTimeThroughMeasure, bpm, startTimeMicros)

        // Then translate that to the beat sequencer's 16th scale
        var beatIn16th = Utils.quantizeBeatWithinMeasureTo(
                Utils.BeatDivision.SIXTEENTH, timeSig,
                round(quantizedTimeInMicros).toLong(), bpm, startTimeMicros)

        if (beatIn16th == (Utils.BeatDivision.SIXTEENTH.divisor * timeSig.beatsPerMeasure)) {
            beatIn16th = 0
        }

        Log.d(LOG_TAG, "Added $beatIn16th")
        if (!quantizedBeatSequenceIn16th.contains(beatIn16th)) {
            quantizedBeatSequenceIn16th.add(beatIn16th)
        }
    }

    private var currentBeat = 0
    public fun setCurrentBeat(beat: Int) {
        if (currentBeat != beat) {
            invalidate()
        }
        currentBeat = beat
    }

    private val backgroundPaint = Paint()
    private val backgroundBorderPaint = Paint()
    private val beatPaint = Paint()
    private val beatTickPaint = Paint()
    private val currentBeatPaint = Paint()

    public fun setBeatColor(color: Int) {
        beatPaint.color = color
        invalidate()
    }
    public fun setBackgroundBorderColor(color: Int) {
        backgroundBorderPaint.color = color
        invalidate()
    }
    public fun setCurrentBeatIndicatorColor(color: Int) {
        currentBeatPaint.color = color
        invalidate()
    }
    override fun setBackgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    private fun commonInit() {
        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = Color.WHITE

        backgroundBorderPaint.style = Paint.Style.STROKE
        backgroundBorderPaint.strokeWidth = 10F
        backgroundBorderPaint.color = Color.BLACK

        beatTickPaint.style = Paint.Style.FILL
        beatTickPaint.color = Color.BLACK

        beatPaint.style = Paint.Style.FILL
        beatPaint.color = Color.BLUE

        currentBeatPaint.style = Paint.Style.FILL
        currentBeatPaint.color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            val floatWidth = width.toFloat()
            val floatHeight = height.toFloat()

            var backgroundPaintToUse =  backgroundPaint
            if (quantizedBeatSequenceIn16th.contains(currentBeat)) {
                backgroundPaintToUse = beatPaint
            }

            drawRect(0F, 0F, floatWidth, floatHeight, backgroundPaintToUse)

            var currentBeatX = calculateXOfBeatIndicator(currentBeat, floatWidth)
            drawRect(currentBeatX, 0F, currentBeatX + 2F, floatHeight, currentBeatPaint)

            // Draw all the beat lines
            for (beatNum in 0 until total16thBeats) {
                currentBeatX = calculateXOfBeatIndicator(beatNum, floatWidth)

                drawRect(currentBeatX, floatHeight - calculateHeightOfBeatTick(beatNum),
                        currentBeatX + 2F, floatHeight, beatTickPaint)

                if (quantizedBeatSequenceIn16th.contains(beatNum)) {
                    currentBeatX = calculateXOfBeatIndicator(beatNum, floatWidth)
                    drawRect(currentBeatX, 0F, currentBeatX + 2F, floatHeight, beatPaint)
                }
            }

            drawRect(0F, 0F, floatWidth, floatHeight, backgroundBorderPaint)
        }
    }

    fun calculateXOfBeatIndicator(beatNum: Int, totalWidth: Float): Float {
        return (beatNum.toFloat() / total16thBeats.toFloat()) * totalWidth
    }

    fun calculateHeightOfBeatTick(beatNum: Int): Float {
        if (isWholeBeat16th(beatNum)) {
            return 48F
        } else if (isHalfBeat16th(beatNum)) {
            return 32F
        } else if (isQuarterBeat16th(beatNum)) {
            return 22F
        } else {
            return 18F
        }
    }

    fun isWholeBeat16th(beatNum: Int): Boolean {
        return beatNum % 16 == 0
    }

    fun isHalfBeat16th(beatNum: Int): Boolean {
        return beatNum % 8 == 0
    }

    fun isQuarterBeat16th(beatNum: Int): Boolean {
        return beatNum % 4 == 0
    }
}