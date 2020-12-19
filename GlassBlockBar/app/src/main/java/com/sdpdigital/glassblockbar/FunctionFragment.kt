package com.sdpdigital.glassblockbar

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel


class FunctionFragment : Fragment() {

    val LOG_TAG: String? = (FunctionFragment::class).simpleName

    // Main thread handler
    val mainHandler = Handler()

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_spectrum, container, false)

        // Get root layout
        val relativeRoot = rootView.findViewById<RelativeLayout>(R.id.item_detail_root_layout)

        // We want the audio controls to control our sound volume.
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC

        rootView.findViewById<SeekBar>(R.id.brightness_bar).setOnSeekBarChangeListener(object: OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                // Send new global brightness to block wall
                glassBlockViewModel?.sendGlobalBrightness(progress.toByte())
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // data to populate the RecyclerView with
        val functions = ArrayList<String>()
        functions.add("Rainbow");

        // set up the RecyclerView
        recyclerView = rootView.findViewById(R.id.recycler_view_function)
        recyclerView?.layoutManager = LinearLayoutManager(activity)
        val adapter = FunctionAdapter(activity, functions)
        recyclerView?.adapter = adapter
        adapter.setClickListener(object: FunctionAdapter.ItemClickListener{
            override fun onItemClick(view: View?, position: Int) {
                glassBlockViewModel?.sendFunction((position + 1).toByte())
            }
        })

        setupGlassBlockViewModel()

        return rootView
    }

    override fun onPause() {
        super.onPause()
        // Cancel any functions
        glassBlockViewModel?.sendFunction(0.toByte())
    }

    private fun setupGlassBlockViewModel() {
        val app = activity?.application as? GlassBlockBarApplication
        app?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
        }
    }

    class FunctionAdapter internal constructor(context: Context?, data: List<String>) :
            RecyclerView.Adapter<FunctionAdapter.ViewHolder>() {
        private val mData: List<String>
        private val mInflater: LayoutInflater
        private var mClickListener: ItemClickListener? = null

        // data is passed into the constructor
        init {
            mInflater = LayoutInflater.from(context)
            mData = data
        }

        // inflates the row layout from xml when needed
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = mInflater.inflate(R.layout.recycler_view_function, parent, false)
            return ViewHolder(view)
        }

        // binds the data to the TextView in each row
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val animal = mData[position]
            holder.myTextView.text = animal
        }

        // total number of rows
        override fun getItemCount(): Int {
            return mData.size
        }

        // stores and recycles views as they are scrolled off screen
        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            var myTextView: TextView
            override fun onClick(view: View) {
                if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
            }

            init {
                myTextView = itemView.findViewById(R.id.recycler_text)
                itemView.setOnClickListener(this)
            }
        }

        // convenience method for getting data at click position
        fun getItem(id: Int): String {
            return mData[id]
        }

        // allows clicks events to be caught
        fun setClickListener(itemClickListener: ItemClickListener?) {
            mClickListener = itemClickListener
        }

        // parent activity will implement this method to respond to click events
        interface ItemClickListener {
            fun onItemClick(view: View?, position: Int)
        }

    }
}

