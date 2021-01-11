package com.sdpdigital.glassblockbar

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.PatternSelectorInput
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.SeekBarInput
import com.sdpdigital.glassblockbar.view.RainbowExpandableAdapter
import com.sdpdigital.glassblockbar.view.RainbowViewGroup
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
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

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    var recyclerView: RecyclerView? = null

    val RAINBOW_ROW_TITLE = "Rainbow Row"
    val RAINBOW_SINE_TITLE = "Rainbow Sine"
    val animations = listOf(RAINBOW_ROW_TITLE, RAINBOW_SINE_TITLE)

    /**
     * From Arduino code:
     */
//    enum RainbowRowPattern {
//        Equal        = 0,
//        EqualOpp     = 1,
//        SlantedLeft  = 2,
//        SlantedRight = 3,
//        ArrowLeft    = 4,
//        ArrowRight   = 5
//    };
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

    val CIRCLE_PATTERN = "Circle"

    val functionOff = -1
    var selectedFunction = functionOff

    // data to populate the RecyclerView with
    val startSpeed = 8
    val animationGroups = listOf(
        RainbowViewGroup(RAINBOW_ROW_TITLE, listOf(
            SeekBarInput(startSpeed, "Animation Speed"),
            PatternSelectorInput(rainbowRowPatterns, 0)
        )),
        RainbowViewGroup(RAINBOW_SINE_TITLE, listOf(
                SeekBarInput(startSpeed, "Animation Speed"),
                PatternSelectorInput(rainbowSinePatterns, 0)
        ))
    )
    val adapter = RainbowExpandableAdapter(animationGroups)
    var rainbowRowPatternIdx = 0
    var rainbowSinePatternIdx = 0

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

        setupGlassBlockViewModel()

        return rootView
    }

    private fun setupAnimationRecyclerView() {
        recyclerView?.layoutManager = LinearLayoutManager(activity)

        adapter.setOnGroupExpandCollapseListener(object: GroupExpandCollapseListener{
            override fun onGroupCollapsed(group: ExpandableGroup<*>?) {
                // no-op needed
            }

            override fun onGroupExpanded(group: ExpandableGroup<*>?) {
                group?.let { groupUnwrapped ->
                    adapter.collapseAllGroups(groupUnwrapped)

                    groupIndexOf(groupUnwrapped.title)?.let {
                        val patternIdx = when(it) {
                            0 -> rainbowRowPatternIdx
                            1 -> rainbowSinePatternIdx
                            else -> 0
                        }
                        sendAnimationAndSpeed(it, patternIdx)
                    }
                }
            }
        })

        adapter.childListener = object: RainbowExpandableAdapter.OnAnimationInputChangedListener {
            override fun seekbarChanged(group: ExpandableGroup<*>?, childIndex: Int, progress: Int) {

                when (group?.title) {
                    RAINBOW_ROW_TITLE -> {
                        groupIndexOf(group.title)?.let {
                            sendAnimationAndSpeed(it, rainbowRowPatternIdx)
                        }
                    }
                    RAINBOW_SINE_TITLE -> {
                        groupIndexOf(group.title)?.let {
                            sendAnimationAndSpeed(it, rainbowSinePatternIdx)
                        }
                    }
                }
            }

            override fun onNewPatternSelected(group: ExpandableGroup<*>?, childIndex: Int, pattern: Int) {
                when (group?.title) {
                    RAINBOW_ROW_TITLE -> {
                        groupIndexOf(group.title)?.let {
                            rainbowRowPatternIdx = pattern
                            sendAnimationAndSpeed(it, rainbowRowPatternIdx)
                        }
                    }
                    RAINBOW_SINE_TITLE -> {
                        rainbowSinePatternIdx = pattern
                        groupIndexOf(group.title)?.let {
                            sendAnimationAndSpeed(it, rainbowSinePatternIdx)
                        }
                    }
                }
            }
        }
        recyclerView?.adapter = adapter
    }

    private fun sendAnimationAndSpeed(groupIdx: Int, patternIdx: Int) {
        selectedFunction = groupIdx

        val speed = seekBarProgress(groupIdx)
        Log.d(LOG_TAG, "Sent Rainbow row $patternIdx change $speed")
        val speedChangeARGB = ByteArray(4)
            { i -> arrayOf(1, groupIdx, patternIdx, speed)[i].toByte() }
        glassBlockViewModel?.sendARGB(speedChangeARGB)
    }

    private fun groupIndexOf(title: String): Int? {
        return animations.indexOf(title)
    }

    private fun setupGlassBlockViewModel() {
        val app = activity?.application as? GlassBlockBarApplication
        app?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
        }
    }
}