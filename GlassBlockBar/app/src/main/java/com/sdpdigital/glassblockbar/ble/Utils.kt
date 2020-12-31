package com.sdpdigital.glassblockbar.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sdpdigital.glassblockbar.viewmodel.TimeSignature
import kotlin.math.round

object Utils {
    private val LOG_TAG: String? = (Utils::class.java).simpleName

    private const val PREFS_LOCATION_NOT_REQUIRED = "location_not_required"
    private const val PREFS_PERMISSION_REQUESTED = "permission_requested"

    /**
     * Checks whether Bluetooth is enabled.
     *
     * @return true if Bluetooth is enabled, false otherwise.
     */
    val isBleEnabled: Boolean
        get() {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            return adapter != null && adapter.isEnabled
        }

    /**
     * Checks for required permissions.
     *
     * @return True if permissions are already granted, false otherwise.
     */
    fun isLocationPermissionsGranted(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Returns true if location permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     *
     * @param activity the activity.
     * @return True if permission has been denied and the popup will not come up any more,
     * false otherwise.
     */
    fun isLocationPermissionDeniedForever(activity: Activity): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return (!isLocationPermissionsGranted(activity) // Location permission must be denied
                && preferences.getBoolean(PREFS_PERMISSION_REQUESTED, false) // Permission must have been requested before
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) // This method should return false
    }

    /**
     * On some devices running Android Marshmallow or newer location services must be enabled in
     * order to scan for Bluetooth LE devices. This method returns whether the Location has been
     * enabled or not.
     *
     * @return True on Android 6.0+ if location mode is different than LOCATION_MODE_OFF.
     * It always returns true on Android versions prior to Marshmallow.
     */
	@JvmStatic
	fun isLocationEnabled(context: Context): Boolean {
        if (isMarshmallowOrAbove) {
            var locationMode = Settings.Secure.LOCATION_MODE_OFF
            try {
                locationMode = Settings.Secure.getInt(context.contentResolver,
                        Settings.Secure.LOCATION_MODE)
            } catch (e: SettingNotFoundException) {
                // do nothing
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF
        }
        return true
    }

    /**
     * Location enabled is required on some phones running Android Marshmallow or newer
     * (for example on Nexus and Pixel devices).
     *
     * @param context the context.
     * @return False if it is known that location is not required, true otherwise.
     */
	@JvmStatic
	fun isLocationRequired(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(PREFS_LOCATION_NOT_REQUIRED, isMarshmallowOrAbove)
    }

    /**
     * When a Bluetooth LE packet is received while Location is disabled it means that Location
     * is not required on this device in order to scan for LE devices. This is a case of Samsung
     * phones, for example. Save this information for the future to keep the Location info hidden.
     *
     * @param context the context.
     */
	@JvmStatic
	fun markLocationNotRequired(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_LOCATION_NOT_REQUIRED, false).apply()
    }

    /**
     * The first time an app requests a permission there is no 'Don't ask again' checkbox and
     * [ActivityCompat.shouldShowRequestPermissionRationale] returns false.
     * This situation is similar to a permission being denied forever, so to distinguish both cases
     * a flag needs to be saved.
     *
     * @param context the context.
     */
    fun markLocationPermissionRequested(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, true).apply()
    }

    val isMarshmallowOrAbove: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    fun convertEqBytesToMinimalBytes(eqBytes: Array<Int>): Array<Int> {
        var retValBytes = Array(4) { 0 }
        val oneDigit = 1 // 001 in binary
        val twoDigits = 3 // 001 in binary
        val threeDigits = 7 // 111 in binary
        val fullMask = 255 // 11111111 in binary

        var eqByte = eqBytes.map { it and threeDigits }

		retValBytes[0] = (eqByte[0] shl 5) or (eqByte[1] shl 2) or (eqByte[2] shr 1)
        retValBytes[1] = ((eqByte[2] and oneDigit) shl 7) or (eqByte[3] shl 4) or (eqByte[4] shl 1) or (eqByte[5] shr 2)
        retValBytes[2] = ((eqByte[5] and twoDigits) shl 6) or (eqByte[6] shl 3) or eqByte[7]
        retValBytes[3] = eqByte[8]

        return retValBytes
    }

    fun convertMinimalBytesToEqBytes(eqByte: Array<Int>): Array<Int> {
        var retValBytes = Array(9) { 0 }

        val maskVal = 7 // 111 in binary
        retValBytes[0] = (eqByte[0] shr 5) and maskVal
        retValBytes[1] = (eqByte[0] shr 2) and maskVal
        retValBytes[2] = ((eqByte[0] shl 1) and maskVal) or ((eqByte[1] shr 7) and maskVal)
        retValBytes[3] = (eqByte[1] shr 4) and maskVal
        retValBytes[4] = (eqByte[1] shr 1) and maskVal
        retValBytes[5] = ((eqByte[1] shl 2) and maskVal) or ((eqByte[2] shr 6) and maskVal)
        retValBytes[6] = (eqByte[2] shr 3) and maskVal
        retValBytes[7] = eqByte[2] and maskVal
        retValBytes[8] = eqByte[3]

        return retValBytes
    }

    // One minute in milliseconds
    const private val perMinuteMillis = (1000L * 60L).toDouble()
    // One minute in microseconds
    const private val perMinuteMicros = 60000000.00

    /**
     * Beat division is a way to divide beats by factors common in musical notes
     * 24 is used as the transmit protocol for BLE, for the same reasons that
     * MIDI protocol chose 24 as the beat division.
     * Its the minimal division that works with all time signatures and triplet notes.
     *
     * The rest of them are used to quantize music in more common (4/4) patterns.
     */
    public enum class BeatDivision(val divisor: Int) {
        FOURTH(        4),
        EIGHTH(        8),
        SIXTEENTH(    16),
        TWENTY_FOURTH(24);

        val one: Double get() {
            return 1.0 / divisor.toDouble()
        }
    }

    fun isWholeBeat24th(beatNum: Int): Boolean {
        return beatNum % 24 == 0
    }

    fun isHalfBeat24th(beatNum: Int): Boolean {
        return beatNum % 12 == 0
    }

    fun isQuarterBeat24th(beatNum: Int): Boolean {
        return beatNum % 6 == 0
    }

    /**
     * @param division the divisional unit of the beat
     * @param timeInMicros of current beat
     * @param bpm beats per minute
     * @param bpmStartTimeMicros start time of bpm counting in microseconds
     * @return the number of 24th beats in long format
     */
    fun quantizeBeatTo(division: BeatDivision, timeInMicros: Long, bpm: Int, bpmStartTimeMicros: Long): Long {
        val numberOfBeats = beatsElapsed(timeInMicros, bpm, bpmStartTimeMicros)
        val numberOfDivisionalBeats = numberOfBeats / division.one
        return round(numberOfDivisionalBeats).toLong()
    }

    /**
     * @param division the divisional unit of the beat
     * @param timeInMicros of current beat
     * @param bpm beats per minute
     * @param bpmStartTimeMicros start time of bpm counting in microseconds
     * @return the timestamp in microseconds of the time synced to closest 24th beat
     */
    fun quantizeBeatToInMicros(division: BeatDivision, timeInMicros: Long, bpm: Int, bpmStartTimeMicros: Long): Double {
        val roundedNumberOfDivisionalBeats =
                quantizeBeatTo(division, timeInMicros, bpm, bpmStartTimeMicros)
        val roundedNumberOfBeats =
                roundedNumberOfDivisionalBeats.toDouble() / division.divisor.toDouble()
        return roundedNumberOfBeats * microsInBeat(bpm)
    }

    /**
     * @param division the divisional unit of the beat
     * @param timeInMicros of current beat
     * @param bpm beats per minute
     * @param bpmStartTimeMicros start time of bpm counting in microseconds
     * @return the number of 24th beats through the current measure that the time is rounded to
     */
    fun quantizeBeatWithinMeasureTo(division: BeatDivision, timeSignature: TimeSignature,
                                    timeInMicros: Long, bpm: Int, bpmStartTimeMicros: Long): Int {

        val beatsElapsed = beatsElapsed(timeInMicros, bpm, bpmStartTimeMicros)
        val beatsThroughMeasure = beatsThroughMeasure(timeSignature, beatsElapsed)
        val numberOfDivisionalBeats = beatsThroughMeasure / division.one
        return round(numberOfDivisionalBeats).toInt()
    }

    /**
     * @param timeInMicros of current beat
     * @param bpm beats per minute
     * @param bpmStartTimeMicros start time of bpm counting in microseconds
     * @return the whole and fractional number of beats in double format
     */
    fun beatsElapsed(timeInMicros: Long, bpm: Int, bpmStartTimeMicros: Long): Double {
        return (timeInMicros - bpmStartTimeMicros) / microsInBeat(bpm)
    }

    /**
     * @param timeSignature with valid beats per measure
     * @param beatsElapsed results of beatsElapsed function
     * @return the number of beats through the current measure
     */
    fun beatsThroughMeasure(timeSignature: TimeSignature, beatsElapsed: Double): Double {
        return beatsElapsed % timeSignature.beatsPerMeasure
    }

    /**
     * @param bpm beats per minute
     * @return number of milliseconds in a beat in double precision
     */
    fun microsInBeat(bpm: Int): Double {
        return perMinuteMicros / bpm
    }

    /**
     * @return the bpm value in decimal precision
     */
    fun bpmFractional(microsInBeat: Long): Double {
        return perMinuteMicros / microsInBeat
    }

    /**
     * @return the bpm value rounded to a whole number
     */
    fun bpm(microsInBeat: Double): Int {
        return round(perMinuteMicros / microsInBeat).toInt()
    }

    // Beat measure characteristic data length
    const val beatMeasueDataLength = 20
    // Start command byte
    const val startSequenceByte = 1
    // Append part to previous sequence messages
    const val appendSequenceByte = 2

    /**
     * To efficiently send quantized beat sequences through BLE,
     * we quantize all user taps to within a 24th beat in the loop (like MIDI).
     * A loop is a musical measure in the specified time signature.
     *
     * Each byte value between the start and end commands
     * represents a 24th beat, and the actual
     * time in milliseconds these represent is computed by the receiver
     * using BPM, BPM time offset, and time signature.
     *
     * The beat sequence message format is as follows:
     * [startSequenceByte][animation_index = AI in these examples]
     * [0 if starting on a beat, or 24th beats to first beat]
     * [distance in 24th beats from first beat to second beat]
     * [distance in 24th beats from first beat to second beat...
     * but 255 if distance > 255 (10x 24th beats), and next byte is added to 255]
     * [endSequenceByte]
     *
     * Take a simple example of 4/4 with the user tapping on the beat, the sequence would be:
     * [1][AI]][0][24][24][24]
     *
     * Take a more complicated example of 16/16 with the user tapping
     * on only the last beat (15/16) in the measure:
     * [1][AI][255][105]
     * Here we had to split the tap into 2 bytes as there is 360 24th beats
     * to the 15/16th beat in the measure, and a byte only has 255 values.
     *
     * Take an even more complicated example of when we need to split the message into
     * multiple ByteArrays, due to the limitation in BLE MTU size matching characteristic size.
     * Say we want to send a measure with beats every 16th note in a 16/16 time signature.
     * The byte sequence would look like this:
     * [1][AI][0][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6]
     * [2][AI][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6]
     * [2][AI][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6][6]
     * [2][AI][6][6][6][6][6][6][6][6][6][6]
     *
     * Take an even EVEN more complicated example of when we need to split the message into
     * multiple ByteArrays, with the first array ending on 255.  17 32nd notes and then a long rest.
     * The byte sequence would look like this:
     * [1][AI][0][3][3][3][3][3][3][3][3][3][3][3][3][3][3][3][3][255]
     * [2][AI][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0][0]
     *
     * This takes 4 messages to send that sequence.
     * See these examples as Unit Tests in BleUtilUnitTest.encodeMeasureOfQuantizedBeats_Test()
     *
     * @param tapsQuantizedTo24thBeats the quantized beats, i.e. in 4/4 first beat is 0,
     *                              second beat is 24, and last is (4 * 24) = 96.
     * @return messages ready to send over BLE to describe a beat sequence
     */
    fun encodeMeasureOfQuantizedBeats(animationIndex: Int, tapsQuantizedTo24thBeats: List<Int>): List<IntArray> {
        val msgList = ArrayList<IntArray>()
        val msg = ArrayList<Int>()

        var addToMsg : (Int) -> Unit = { toAdd: Int ->
            if (msg.size >= beatMeasueDataLength) {
                msgList.add(msg.toIntArray())
                msg.clear()
                msg.add(appendSequenceByte)
                msg.add(animationIndex)
            }
            msg.add(toAdd)
        }

        val sortedTaps = tapsQuantizedTo24thBeats.sorted()
        // Beats must be sorted, low to high, to send distance to next beat
        for ((i, beat) in sortedTaps.withIndex()) {
            // Check for start sequence of the first message
            if (i == 0) {
                addToMsg(startSequenceByte)  // Always start with this to give context to following bytes
                addToMsg(animationIndex)     // The animation this sequence should be assigned to
            }
            // Check for the start sequence of appending a message
            if (msg.size == 0) {
                addToMsg(appendSequenceByte)
                msg.add(animationIndex)
            }
            // Write the difference from last tap for all steps but the first
            var diff = if(i == 0) { beat } else { beat - sortedTaps[i-1] }
            while (diff >= 255) {
                addToMsg(255)
                diff -= 255
            }
            if (diff >= 0 || i == 0) {
                addToMsg(diff)
            }
        }

        // Check if we need to end the last sequence by filling all 20 bytes
        if (msg.size > 0) {
            for (i in msg.size until beatMeasueDataLength) {
                msg.add(0)
            }
            msgList.add(msg.toIntArray())
        }
        return msgList
    }

    /**
     * This function decodes messages that are encoded like above in encodeMeasureOfQuantizedBeats.
     * @param lastBeatVal in the current encodedMessage
     * @param beatSequence the current beat sequence for the designated animation
     * @param startBeatValue this is the value at which we should start counting at
     *                       when decoding the next message
     *
     * @return the new beatSequence value to be used
     */
    fun decodeMeasureOfQuantized24thBeatsMessage(
            encodedMessage: Array<Int>,
            encodedMessageSize: Int,
            beatSequence: Array<Int>,
            beatSequenceSize: Int,
            startBeatValue: Int): DecodedReturnValue {

        // First two bytes always designate message type and the animation index
        val messageType = encodedMessage[0]
        val animationIndex = encodedMessage[1]

        // Find the length of the beat sequence section of the message
        // The end is signaled by a zero after the 3rd byte
        var i = 3
        var accurateMessageSize = 0  // set to 1 to get it to run
        var found = false
        var count255 = 0            // Count the number of 255 values
        if (encodedMessage[2] == 255) {
            count255++
        }
        while (!found && (i < encodedMessageSize)) {
            // Check for end of the message, if these conditions are met
            if (encodedMessage[i] == 0 &&
                (encodedMessage[i -1] != 255)) {
                accurateMessageSize = i - 2 - count255
                found = true
            } else if (encodedMessage[i] == 255) {
                // Ignore 255 in the message count,
                // because it is a continuing sum
                count255++
            }
            i++
        }

        // The whole beat sequence in the message is valid
        if (accurateMessageSize == 0) {
            accurateMessageSize = encodedMessageSize - 2 - count255
        }

        // If we are starting the sequence, erase all previous data
        var newBeatSequenceSize = beatSequenceSize + accurateMessageSize
        var newLastBeatValIn24th = 0
        i = 0
        if (messageType == startSequenceByte) {
            newBeatSequenceSize = accurateMessageSize
            newLastBeatValIn24th = 0
        }
        // Create a new array with the new size
        var newBeatSequence = Array(newBeatSequenceSize) { 0 }
        // Fill up the array, depending on the messageType
        if (messageType == appendSequenceByte) {  // appendSequenceByte
            for (copyI in 0 until beatSequenceSize) {
                newBeatSequence[copyI] = beatSequence[copyI]
            }
            i = beatSequenceSize
            newLastBeatValIn24th = startBeatValue
        }

        // Now, fill in the rest of the translated beat sequence to full time signature beat scale
        // Skipping first 2 bytes for messageType and animationIdx
        for (encodedIdx in 2 until (accurateMessageSize + 2 + count255)) {
            // Always add to the running total
            newLastBeatValIn24th += encodedMessage[encodedIdx]
            if (i == 33) {
                val debug = true
            }
            // 255 is a case where it is considered "carried over"
            // to the next sum, and not an actual beat value
            if (encodedMessage[encodedIdx] != 255) {
                newBeatSequence[i] = newLastBeatValIn24th
                i++
            }
        }

        // In the translated arduino code, here we must...
        // Delete old memory here in c
        // De-reference passed in lastBeatVal here in C

        return DecodedReturnValue(newBeatSequence, newBeatSequenceSize, newLastBeatValIn24th)
    }

    class DecodedReturnValue(
            val beatSequence: Array<Int> = Array(0) { 0 },
            val beatSequenceSize: Int = 0,
            val startBeatValue: Int = 0)
}