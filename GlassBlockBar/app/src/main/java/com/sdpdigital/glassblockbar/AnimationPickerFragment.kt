package com.sdpdigital.glassblockbar

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.PatternSelectorInput
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.SeekBarInput
import com.sdpdigital.glassblockbar.view.RainbowExpandableAdapter
import com.sdpdigital.glassblockbar.view.RainbowViewGroup
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.thoughtbot.expandablerecyclerview.listeners.GroupExpandCollapseListener
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import no.nordicsemi.android.ble.livedata.state.ConnectionState


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

    val RAINBOW_TITLE = "Rainbow"
    val RAINBOW_ROW_TITLE = "Rainbow Row"
    val animations = listOf(RAINBOW_TITLE, RAINBOW_ROW_TITLE)

    val LINE_PATTERN = "Slanted"
    val ARROW_PATTERN = "Arrow"
    val EQUAL_PATTERN = "Equal Space"
    val SINE_PATTERN = "Square Wave"
    val SINE_BLOCK_PATTERN = "Square Block Wave"
    val CIRCLE_PATTERN = "Circle"
    val patterns = arrayOf(
        LINE_PATTERN, ARROW_PATTERN, EQUAL_PATTERN,
        SINE_PATTERN, SINE_BLOCK_PATTERN, CIRCLE_PATTERN)

    val functionOff = -1
    var selectedFunction = functionOff

    // data to populate the RecyclerView with
    val animationGroups = listOf(
        RainbowViewGroup(RAINBOW_TITLE, listOf(
            SeekBarInput(8, "Animation Speed")  // 8 = speed of 2.0 on BLE peripheral
        )),
        RainbowViewGroup(RAINBOW_ROW_TITLE, listOf(
            SeekBarInput(8, "Animation Speed"),
            PatternSelectorInput(patterns, 0)
        ))
    )
    val adapter = RainbowExpandableAdapter(animationGroups)

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
                        selectAnimation(it)
                    }
                }
            }
        })

        adapter.childListener = object: RainbowExpandableAdapter.OnAnimationInputChangedListener {
            override fun seekbarChanged(group: ExpandableGroup<*>?, childIndex: Int, progress: Int) {
                when (group?.title) {
                    RAINBOW_TITLE -> {
                        Log.d(LOG_TAG, "Sent Rainbow speed change $progress")
                        val speedChangeARGB = ByteArray(4)
                        { i -> arrayOf(1, 0, 0, progress)[i].toByte() }
                        glassBlockViewModel?.sendARGB(speedChangeARGB)
                    }
                    RAINBOW_ROW_TITLE -> {
                        Log.d(LOG_TAG, "Sent Rainbow row $childIndex speed change $progress")
                        val speedChangeARGB = ByteArray(4)
                        { i -> arrayOf(1, 1, childIndex, progress)[i].toByte() }
                        glassBlockViewModel?.sendARGB(speedChangeARGB)
                    }
                }
            }

            override fun onNewPatternSelected(group: ExpandableGroup<*>?, childIndex: Int, patternIdx: Int) {
                when (group?.title) {
                    RAINBOW_ROW_TITLE -> {
                        Log.d(LOG_TAG, "Sent Rainbow row pattern change $patternIdx")
                        val speedChangeARGB = ByteArray(4)
                            { i -> arrayOf(1, 1, childIndex, patternIdx)[i].toByte() }
                        glassBlockViewModel?.sendARGB(speedChangeARGB)
                    }
                }
            }
        }
        recyclerView?.adapter = adapter
    }

    private fun selectAnimation(groupIdx: Int) {
        selectedFunction = groupIdx

        if (selectedFunction == animations.indexOf(RAINBOW_TITLE)) {
            Log.d(LOG_TAG, "Sent Rainbow animation start")
            glassBlockViewModel?.sendARGB(ByteArray(4) {
                arrayOf(1, 33, 99, 133)[it].toByte() })
        }
        if  (selectedFunction == animations.indexOf(RAINBOW_ROW_TITLE)) {
            Log.d(LOG_TAG, "Sent Rainbow row animation start")
            glassBlockViewModel?.sendARGB(ByteArray(4) { i ->
                arrayOf(1, 33, 99, 134)[i].toByte() })
        }
    }

    private fun groupIndexOf(title: String): Int? {
        when(title) {
            RAINBOW_TITLE -> return animations.indexOf(RAINBOW_TITLE)
            RAINBOW_ROW_TITLE -> return animations.indexOf(RAINBOW_ROW_TITLE)
        }
        return null
    }

    private fun setupGlassBlockViewModel() {
        val app = activity?.application as? GlassBlockBarApplication
        app?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
        }
    }
}