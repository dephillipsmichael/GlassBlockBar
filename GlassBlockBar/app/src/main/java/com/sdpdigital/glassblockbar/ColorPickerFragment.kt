package com.sdpdigital.glassblockbar

import android.os.Bundle
import android.util.Log
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
import no.nordicsemi.android.ble.livedata.state.ConnectionState


/**
 * A fragment that allows the user to choose a color from the color wheel
 * to set a solid color on the wall
 */
class ColorPickerFragment : Fragment() {

    val LOG_TAG: String? = (ColorPickerFragment::class).simpleName

    var glassBlockViewModel: GlassBlockBarViewModel? = null

    val currentArgb = arrayOf(255, 255, 255, 255) // full white

    val durationBetweenSends = 60;
    var lastColorSendTime = System.currentTimeMillis()

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

                    val debug = it.joinToString(", ") { value -> "$value" }
                    Log.d(LOG_TAG, "Color tapped $debug")

                    val now = System.currentTimeMillis()
                    if (now - lastColorSendTime < durationBetweenSends) {
                        return@ColorEnvelopeListener
                    }
                    lastColorSendTime = now

                    val newArgbArray = ByteArray(5) { idx ->
                        if (idx == 0) { return@ByteArray 0 }
                        return@ByteArray it[idx - 1].toByte()
                    }
                    glassBlockViewModel?.sendARGB(newArgbArray)
                }
            }
        colorPicker?.setColorListener(colorPickerListener)
        val bubbleFlag = BubbleFlag(activity)
        bubbleFlag.flagMode = FlagMode.FADE
        colorPicker?.flagView = bubbleFlag

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