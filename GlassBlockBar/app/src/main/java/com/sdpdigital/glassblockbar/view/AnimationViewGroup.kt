package com.sdpdigital.glassblockbar.view

import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.res.ResourcesCompat
import com.sdpdigital.glassblockbar.R
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.PatternSelectorInput
import com.sdpdigital.glassblockbar.view.AnimationChildViewGroups.SeekBarInput
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter
import com.thoughtbot.expandablerecyclerview.MultiTypeExpandableRecyclerViewAdapter
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder
import java.util.*


// Rainbow animation classes for expandable recycler view sdk
public class RainbowViewGroup(title: String?, items: List<Parcelable>) :
    ExpandableGroup<Parcelable?>(title, items)

public class BaseAnimationGroupViewHolder(itemView: View) : GroupViewHolder(itemView) {
    private val titleTextView: TextView

    fun setTitle(group: ExpandableGroup<*>, adapter: ExpandableRecyclerViewAdapter<*, *>) {
        titleTextView.text = group.title
        if (adapter.isGroupExpanded(group)) {
            itemView.setBackgroundColor(
                ResourcesCompat.getColor(itemView.context.resources, R.color.grey, null))
        } else {
            itemView.setBackgroundColor(
                ResourcesCompat.getColor(itemView.context.resources, R.color.white, null))
        }
    }

    init {
        titleTextView = itemView.findViewById(R.id.title_text_view)
    }
}

public class SeekBarInputChildViewHolder(itemView: View) : ChildViewHolder(itemView) {
    val seekBarAnimSpeed: AppCompatSeekBar
    val titleTextView: TextView

    fun setRainbowSetting(setting: SeekBarInput) {
        seekBarAnimSpeed.max = setting.progressEnd - setting.progressStart
        seekBarAnimSpeed.progress = setting.seekBarProgress
        val title = setting.title
        val progressVal = setting.seekBarProgress + setting.progressStart
        titleTextView.text =  "$title $progressVal"
    }

    init {
        seekBarAnimSpeed = itemView.findViewById(R.id.speed_slider)
        titleTextView = itemView.findViewById(R.id.seek_bar_title_text_view)
    }
}

public class PatternSelectorInputChildViewHolder(itemView: View) : ChildViewHolder(itemView) {
    val leftButton: Button
    val rightButton: Button
    val titleTextView: TextView

    fun setPattern(setting: PatternSelectorInput) {
        titleTextView.text = setting.patterns[setting.patternIdx];
    }

    init {
        titleTextView = itemView.findViewById(R.id.pattern_title_text_view)
        leftButton = itemView.findViewById(R.id.button_pattern_left)
        rightButton = itemView.findViewById(R.id.button_pattern_right)
    }
}

public class ColorPickerInputChildViewHolder(itemView: View) : ChildViewHolder(itemView) {
    val spinner: Spinner

    fun setColorPickerSetting(setting: AnimationChildViewGroups.ColorGroupPickerInput) {
        spinner.setSelection(setting.colorIdx)
    }

    init {
        spinner = itemView.findViewById(R.id.spinner_color_palette)

        val colorPalettes: MutableList<ColorPalette> = ArrayList()
        colorPalettes.add(ColorPalette.createRainbowColorPalette())
        colorPalettes.add(ColorPalette.createCoolWinterTones())
        colorPalettes.add(ColorPalette.createPinkTones())
        colorPalettes.add(ColorPalette.createIndiansPalette())
        colorPalettes.addAll(ColorPalette.createComplimentaryColorsCombos())
        colorPalettes.add(ColorPalette.createRgbColorPalette())
        colorPalettes.add(ColorPalette.createOgyColorPalette())
        colorPalettes.addAll(ColorPalette.createComplimentaryColorsWithWhite())
        colorPalettes.addAll(ColorPalette.createComplimentaryColors())
        colorPalettes.add(ColorPalette.createArmyTonesPalette())
        colorPalettes.addAll(ColorPalette.createOnlinePalettes())

        spinner.adapter = ColorPaletteSpinnerAdapter(
                spinner.context, R.layout.spinner_color_palette_row, colorPalettes)

        spinner.setSelection(0)

        spinner.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, idx: Int, l: Long) {
                Log.d("TODO_REMOVE", "Index selected $idx")
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                // What should we do here?
                Log.d("TODO_REMOVE", "Nothing selected")
            }
        })
    }
}

public class RainbowExpandableAdapter(groups: List<ExpandableGroup<*>?>?) :
    MultiTypeExpandableRecyclerViewAdapter<BaseAnimationGroupViewHolder, ChildViewHolder>(groups) {

    val SEEK_BAR_VIEW_TYPE = 3
    val PATTERN_VIEW_TYPE = 4
    val COLOR_PICKER_VIEW_TYPE = 5

    var childListener: OnAnimationInputChangedListener? = null

    fun collapseAllGroups() {
        collapseAllGroups(null)
    }

    fun collapseAllGroups(except: ExpandableGroup<*>?) {
        for (group in expandableList.groups) {
            // If the group is not the one passed in and it is expanded, then collapse it
            if (except == null || (group.title != except.title && isGroupExpanded(group))) {
                toggleGroup(group)
            }
        }
    }

    override fun getChildViewType(position: Int, group: ExpandableGroup<*>, childIndex: Int): Int {
        (group.items[childIndex] as? SeekBarInput)?.let {
            return SEEK_BAR_VIEW_TYPE
        }

        (group.items[childIndex] as? PatternSelectorInput)?.let {
            return PATTERN_VIEW_TYPE
        }

        (group.items[childIndex] as? AnimationChildViewGroups.ColorGroupPickerInput)?.let {
            return COLOR_PICKER_VIEW_TYPE
        }

        return 2 // default child index
    }

    override fun isChild(viewType: Int): Boolean {
        return viewType == SEEK_BAR_VIEW_TYPE || viewType == PATTERN_VIEW_TYPE ||
                viewType == COLOR_PICKER_VIEW_TYPE
    }

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): BaseAnimationGroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_group_rainbow, parent, false)
        return BaseAnimationGroupViewHolder(view)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        return when (viewType) {
            SEEK_BAR_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.view_child_seekbar, parent, false)
                SeekBarInputChildViewHolder(view)
            }
            PATTERN_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.view_child_pattern_selection, parent, false)
                PatternSelectorInputChildViewHolder(view)
            }
            else -> { // COLOR_PICKER_VIEW_TYPE
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.view_child_color_picker, parent, false)
                ColorPickerInputChildViewHolder(view)
            }
        }
    }

    override fun onBindChildViewHolder(holder: ChildViewHolder?, position: Int,
                                       group: ExpandableGroup<*>?, childIndex: Int) {

        // Seekbar item
        (holder as? SeekBarInputChildViewHolder)?.let {
            (group?.items?.get(childIndex) as? SeekBarInput)?.let { seekBarSetting ->
                holder?.setRainbowSetting(seekBarSetting)
                holder?.seekBarAnimSpeed?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                        seekBarSetting.seekBarProgress = progress
                        holder?.setRainbowSetting(seekBarSetting)
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                        /** no-op */
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        seekBar?.progress?.let {
                            childListener?.seekbarChanged(group, childIndex, it)
                        }
                    }
                })
            }
        }

        // Pattern selector item
        (holder as? PatternSelectorInputChildViewHolder)?.let {
            (group?.items?.get(childIndex) as? PatternSelectorInput)?.let { setting ->
                holder?.setPattern(setting)
                holder?.leftButton?.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        setting.patternIdx--
                        if (setting.patternIdx < 0) {
                            setting.patternIdx = setting.patterns.size - 1
                        }
                        childListener?.onNewPatternSelected(group, childIndex,
                                setting.patternIdx)
                        notifyDataSetChanged()
                    }
                })
                holder?.rightButton?.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        setting.patternIdx++;
                        if (setting.patternIdx >= setting.patterns.size) {
                            setting.patternIdx = 0
                        }
                        childListener?.onNewPatternSelected(group, childIndex,
                                setting.patternIdx)
                        notifyDataSetChanged()
                    }
                })
            }
        }

        // Color picker item
        (holder as? ColorPickerInputChildViewHolder)?.let {
            (group?.items?.get(childIndex) as? AnimationChildViewGroups.ColorGroupPickerInput)?.let { colorSetting ->
                holder?.setColorPickerSetting(colorSetting)
            }
        }
    }

    override fun onBindGroupViewHolder(holder: BaseAnimationGroupViewHolder?,
                                       position: Int, group: ExpandableGroup<*>?) {
        group?.let {
            holder?.setTitle(group, this)
        }
    }

    public interface OnAnimationInputChangedListener {
        fun seekbarChanged(group: ExpandableGroup<*>?, childIndex: Int, progress: Int)
        fun onNewPatternSelected(group: ExpandableGroup<*>?, childIndex: Int, patternIdx: Int)
    }
}