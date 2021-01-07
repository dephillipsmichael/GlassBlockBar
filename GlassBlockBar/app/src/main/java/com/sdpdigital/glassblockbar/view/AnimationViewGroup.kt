package com.sdpdigital.glassblockbar.view

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
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
        seekBarAnimSpeed.progress = setting.seekBarProgress
        val title = setting.title
        val progressStr = setting.seekBarProgress.toString()
        titleTextView.text =  "$title - $progressStr"
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

public class RainbowExpandableAdapter(groups: List<ExpandableGroup<*>?>?) :
    MultiTypeExpandableRecyclerViewAdapter<BaseAnimationGroupViewHolder, ChildViewHolder>(groups) {

    val SEEK_BAR_VIEW_TYPE = 3
    val PATTERN_VIEW_TYPE = 4

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

        return 2 // default child index
    }

    override fun isChild(viewType: Int): Boolean {
        return viewType == SEEK_BAR_VIEW_TYPE || viewType == PATTERN_VIEW_TYPE
    }

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): BaseAnimationGroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_group_rainbow, parent, false)
        return BaseAnimationGroupViewHolder(view)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        return if (viewType == SEEK_BAR_VIEW_TYPE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_child_seekbar, parent, false)
            SeekBarInputChildViewHolder(view)
        } else { // PATTERN_VIEW_TYPE
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_child_pattern_selection, parent, false)
            PatternSelectorInputChildViewHolder(view)
        }
    }

    override fun onBindChildViewHolder(holder: ChildViewHolder?, position: Int,
                                       group: ExpandableGroup<*>?, childIndex: Int) {

        // Seekbar item
        (holder as? SeekBarInputChildViewHolder)?.let {
            (group?.items?.get(childIndex) as? SeekBarInput)?.let { seekBarSetting ->
                holder?.setRainbowSetting(seekBarSetting)
                holder?.seekBarAnimSpeed?.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                        seekBarSetting.seekBarProgress = progress
                        holder?.setRainbowSetting(seekBarSetting)
                    }
                    override fun onStartTrackingTouch(p0: SeekBar?) { /** no-op */ }
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