package com.sdpdigital.glassblockbar

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

/**
 * A fragment that allows the user to choose a color from the color wheel
 * to set a solid color on the wall
 */
class ColorPickerFragment : Fragment() {

    val LOG_TAG: String? = (ColorPickerFragment::class).simpleName

    private val app: GlassBlockLEDApplication? get() {
        return activity?.application as? GlassBlockLEDApplication
    }

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
                    app?.writeBleMessage(newArgbArray)
                }
            }
        colorPicker?.setColorListener(colorPickerListener)
        val bubbleFlag = BubbleFlag(activity)
        bubbleFlag.flagMode = FlagMode.FADE
        colorPicker?.flagView = bubbleFlag

        return rootView
    }
}