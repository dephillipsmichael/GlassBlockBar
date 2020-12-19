package com.sdpdigital.glassblockbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
                    val newArgbArray = ByteArray(4) { idx -> it[idx].toByte() }
                    glassBlockViewModel?.sendARGB(newArgbArray)
                }
            }
        colorPicker?.setColorListener(colorPickerListener)
        val bubbleFlag = BubbleFlag(activity)
        bubbleFlag.flagMode = FlagMode.FADE
        colorPicker?.setFlagView(bubbleFlag)

        val alphaSlider = rootView.findViewById<AlphaSlideBar>(R.id.alpha_slide_bar)
        colorPicker?.attachAlphaSlider(alphaSlider)

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
}