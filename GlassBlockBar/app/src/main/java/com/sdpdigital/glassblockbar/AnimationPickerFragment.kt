package com.sdpdigital.glassblockbar

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.PatternSelectorInput
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.SeekBarInput
import com.sdpdigital.glassblockbar.view.RainbowExpandableAdapter
import com.sdpdigital.glassblockbar.view.RainbowViewGroup
import com.thoughtbot.expandablerecyclerview.listeners.GroupExpandCollapseListener
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup


/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [LEDFunctionListActivity]
 * in two-pane mode (on tablets) or a [LEDFunctionDetailActivity]
 * on handsets.
 */
class AnimationPickerFragment : Fragment() {

    val LOG_TAG: String? = (AnimationPickerFragment::class).simpleName

    private val app: GlassBlockLEDApplication? get() {
        return activity?.application as? GlassBlockLEDApplication
    }

    var recyclerView: RecyclerView? = null

    val RAINBOW_TITLE = "Rainbow"
    val RAINBOW_ROW_TITLE = "Rainbow Row"
    val RAINBOW_SINE_TITLE = "Rainbow Sine"
    val TETRIS_TITLE = "Tetris"
    val FADE_TITLE = "Fade"
    val animations = listOf(RAINBOW_ROW_TITLE, RAINBOW_SINE_TITLE, TETRIS_TITLE, FADE_TITLE, RAINBOW_TITLE)

    /**
     * From Arduino code:
     *    enum RainbowRowPattern {
     *        Equal        = 0,
     *        EqualOpp     = 1,
     *        SlantedLeft  = 2,
     *        SlantedRight = 3,
     *        ArrowLeft    = 4,
     *        ArrowRight   = 5
     *    };
     */
    val OPPOSITE_PATTERN = "Opposite Rows" // Equal
    val CLASSIC_PATTERN = "Classic"        // Classic
    val SLANTED_LEFT_PATTERN = "Slanted Left"
    val SLANTED_RIGHT_PATTERN = "Slanted Right"
    val ARROW_LEFT_PATTERN = "Arrow Left"
    val ARROW_RIGHT_PATTERN = "Arrow Right"
    val rainbowRowPatterns = arrayOf(
        CLASSIC_PATTERN, OPPOSITE_PATTERN, SLANTED_LEFT_PATTERN, SLANTED_RIGHT_PATTERN,
        ARROW_LEFT_PATTERN, ARROW_RIGHT_PATTERN)

    val SINE_PATTERN_BLOCK      = "Blocks"
    val SINE_BLOCK_PATTERN_1GAP = "Blocks 1 gap"
    val SINE_BLOCK_PATTERN_2GAP = "Blocks 2 gap"
    val SINE_BLOCK_PATTERN_3GAP = "Blocks 3 gap"
    val SINE_LED_PATTERN        = "LEDs"
    val rainbowSinePatterns = arrayOf(
            SINE_PATTERN_BLOCK, SINE_BLOCK_PATTERN_1GAP, SINE_BLOCK_PATTERN_2GAP, SINE_BLOCK_PATTERN_3GAP,
            SINE_LED_PATTERN)

    val COLOR_CHANGE_PATTERN = "Color Changing"
    val tetrisPatterns = arrayOf(
            CLASSIC_PATTERN, COLOR_CHANGE_PATTERN)

    val CIRCLE_PATTERN = "Circle"

    val functionOff = -1
    var selectedFunction = functionOff

    // data to populate the RecyclerView with
    val startSpeed = 8
    val tetrisStartSpeed = 24
    val animationGroups = listOf(
        RainbowViewGroup(RAINBOW_ROW_TITLE, listOf(
            SeekBarInput(startSpeed, "Animation Speed", -24, 24),
            PatternSelectorInput(rainbowRowPatterns, 0)
        )),
        RainbowViewGroup(RAINBOW_SINE_TITLE, listOf(
            SeekBarInput(startSpeed, "Animation Speed", -24, 24),
            PatternSelectorInput(rainbowSinePatterns, 0)
        )),
        RainbowViewGroup(TETRIS_TITLE, listOf(
            SeekBarInput(tetrisStartSpeed, "Animation Speed", 0, 48),
            PatternSelectorInput(tetrisPatterns, 0)
        )),
        RainbowViewGroup(FADE_TITLE, listOf(
            SeekBarInput(tetrisStartSpeed, "Animation Speed", 0, 48),
            AnimationChildViewGroups.ColorGroupPickerInput(0)
        )),
        RainbowViewGroup(RAINBOW_TITLE, listOf(
            SeekBarInput(0, "Does nothing", 0, 48)
        ))
    )
    val adapter = RainbowExpandableAdapter(animationGroups)
    var patternIdxs = mutableListOf(0, 0, 0, 0, 0)

    fun seekBarProgress(groupIdx: Int): Int {
        (animationGroups[groupIdx].items?.firstOrNull() as? SeekBarInput)?.let {
            return it.seekBarProgress
        }
        return startSpeed
    }

    val rainbowRowPattern: Int get() {
        (animationGroups?.firstOrNull()?.items?.firstOrNull() as? PatternSelectorInput)?.let {
            return it.patternIdx
        }
        return 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_animation_picker, container, false)


        recyclerView = rootView.findViewById(R.id.recycler_view_animations)
        setupAnimationRecyclerView()

        return rootView
    }

    private fun setupAnimationRecyclerView() {
        recyclerView?.layoutManager = LinearLayoutManager(activity)

        adapter.setOnGroupExpandCollapseListener(object: GroupExpandCollapseListener{
            override fun onGroupCollapsed(group: ExpandableGroup<*>?) {
                // no-op needed
            }

            override fun onGroupExpanded(group: ExpandableGroup<*>?) {
                val groupUnwrapped = group ?: run { return }
                adapter.collapseAllGroups(groupUnwrapped)
                sendAnimationAndSpeedAndPattern(groupUnwrapped.title)
            }
        })

        adapter.childListener = object: RainbowExpandableAdapter.OnAnimationInputChangedListener {
            override fun seekbarChanged(group: ExpandableGroup<*>?, childIndex: Int, progress: Int) {
                sendAnimationAndSpeedAndPattern(group?.title)
            }

            override fun onNewPatternSelected(group: ExpandableGroup<*>?, childIndex: Int, pattern: Int) {
                val groupIdx = groupIndexOf(group?.title ?: "") ?: run { return }
                patternIdxs[groupIdx] = pattern
                sendAnimationAndSpeedAndPattern(group?.title)
            }
        }
        recyclerView?.adapter = adapter
    }

    private fun sendAnimationAndSpeedAndPattern(groupTitleMaybeNull: String?) {
        val groupTitle = groupTitleMaybeNull ?: run { return }
        val groupIdx = groupIndexOf(groupTitle) ?: run { return }
        val patternIdx = patternIdxs[groupIdx]

        selectedFunction = groupIdx

        val speed = seekBarProgress(groupIdx)
        Log.d(LOG_TAG, "Sent $groupTitle change pattern $patternIdx speed $speed")
        val speedChangeARGB = ByteArray(4)
            { i -> arrayOf(1, groupIdx, patternIdx, speed)[i].toByte() }
        app?.writeBleMessage(speedChangeARGB)
    }

    private fun groupIndexOf(title: String): Int? {
        return animations.indexOf(title)
    }
}