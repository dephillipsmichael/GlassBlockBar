package com.sdpdigital.glassblockbar

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.sliders.AlphaSlideBar
import no.nordicsemi.android.ble.livedata.state.ConnectionState

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [LEDFunctionListActivity]
 * in two-pane mode (on tablets) or a [LEDFunctionDetailActivity]
 * on handsets.
 */
class ColorPickerFragment : Fragment() {

    val LOG_TAG: String? = (ColorPickerFragment::class).simpleName

    /**
     * The dummy content this fragment is presenting.
     */
    private var item: String? = null

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    var recyclerView: RecyclerView? = null

    val functionOff = 0
    val functionRainbow = 1
    val functionRainbowRow = 2

    var selectedFunction = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_color_picker, container, false)

        val colorPicker: ColorPickerView? = rootView.findViewById(R.id.colorPickerView)
        val colorPickerListener =
            ColorEnvelopeListener { envelope, _ ->
                envelope?.argb?.let {

                    if (selectedFunction > 0 &&
                        Log.getStackTraceString(Exception()).contains("AlphaSlideBar")) {
                        // If this was triggered by the alpha slide bar,
                        // And we are doing a function, just control the brightness
                        val brightnessOnlyARGB = ByteArray(4)
                            { i -> arrayOf(it[i], 0, 0, 0)[i].toByte() }
                        glassBlockViewModel?.sendARGB(brightnessOnlyARGB)
                        return@ColorEnvelopeListener
                    }

                    selectedFunction = functionOff
                    val newArgbArray = ByteArray(4) { idx -> it[idx].toByte() }
                    glassBlockViewModel?.sendARGB(newArgbArray)
                }
            }
        colorPicker?.setColorListener(colorPickerListener)
        val bubbleFlag = BubbleFlag(activity)
        bubbleFlag.flagMode = FlagMode.FADE
        colorPicker?.setFlagView(bubbleFlag)

        val speedSlider = rootView.findViewById<AppCompatSeekBar>(R.id.speed_slider)
        speedSlider.max = 32
        speedSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                // 0 until 255
                val speedChangeARGB = ByteArray(4)
                    { i -> arrayOf(1, 0, 0, progress)[i].toByte() }
                glassBlockViewModel?.sendARGB(speedChangeARGB)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //no op needed
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //no op needed
            }

        })

        //if (1 == argb[0] && 0 == argb[1] && 0 == argb[2]) {

            val alphaSlider = rootView.findViewById<AlphaSlideBar>(R.id.alpha_slide_bar)
        colorPicker?.attachAlphaSlider(alphaSlider)

        // data to populate the RecyclerView with
        val functions = ArrayList<String>()
        functions.add("Rainbow")
        functions.add("Rainbow Row")

        // set up the RecyclerView
        recyclerView = rootView.findViewById(R.id.recycler_view_function)
        recyclerView?.layoutManager = LinearLayoutManager(activity)
        val adapter = FunctionAdapter(activity, functions)
        recyclerView?.adapter = adapter
        adapter.setClickListener(object: FunctionAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                selectedFunction = position + 1
                if (selectedFunction == functionRainbow) {
                    val rainbowFunctionARGB = ByteArray(4) { arrayOf(1, 33, 99, 133)[it].toByte() }
                    glassBlockViewModel?.sendARGB(rainbowFunctionARGB)
                } else if (selectedFunction == functionRainbowRow) {
                    val rainbowFunctionARGB = ByteArray(4) { arrayOf(1, 33, 99, 134)[it].toByte() }
                    glassBlockViewModel?.sendARGB(rainbowFunctionARGB)
                }
                //glassBlockViewModel?.sendFunction((position + 1).toByte())
            }
        })

        setupGlassBlockViewModel()

        return rootView
    }

    private fun setupGlassBlockViewModel() {
        val app = activity?.application as? GlassBlockBarApplication
        app?.let {
            val factory = AppViewModelFactory(it)
            glassBlockViewModel = ViewModelProvider(it, factory).get(GlassBlockBarViewModel::class.java)
            // Connection state observer
            val connectionObserver = Observer<ConnectionState> { connectionState  ->
                // Update the UI, in this case, a TextView.
                when(connectionState) {
                    ConnectionState.Disconnecting -> {
                        // TODO: go back to connection screen
                    }
                    else -> {
                        // no-op
                    }
                }
            }
            glassBlockViewModel?.connectionState?.observe(viewLifecycleOwner, connectionObserver)
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