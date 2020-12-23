package com.sdpdigital.glassblockbar.view

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.R

public class BaseListRecyclerAdapter internal constructor(context: Context?, data: List<String>) :
        RecyclerView.Adapter<BaseListRecyclerAdapter.ViewHolder>() {

    private val items: List<String>
    private val inflater: LayoutInflater
    private var clickListener: ItemClickListener? = null
    private var selectedPosition = -1

    // data is passed into the constructor
    init {
        inflater = LayoutInflater.from(context)
        this.items = data
    }

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.recycler_view_function, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(selectedPosition == position) {
            holder.itemView.setBackgroundColor(
                ResourcesCompat.getColor(inflater.context.resources, R.color.grey, null))
        } else {
            holder.itemView.setBackgroundColor(
                ResourcesCompat.getColor(inflater.context.resources, R.color.white, null))
        }
        val animal = items[position]
        holder.myTextView.text = animal
    }

    // total number of rows
    override fun getItemCount(): Int {
        return items.size
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView), View.OnClickListener {

        var myTextView: TextView
        override fun onClick(view: View) {
            selectedPosition = adapterPosition
            if (clickListener != null) {
                clickListener!!.onItemClick(view, adapterPosition)
            }
            notifyDataSetChanged()
        }

        init {
            myTextView = itemView.findViewById(R.id.recycler_text)
            itemView.setOnClickListener(this)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): String {
        return items[id]
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        clickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }
}