/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sdpdigital.glassblockbar.ble

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.round

object Utils {
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
    const private val beatDivisionFactor24 = 24
    const private val one24thBeat = 1.0 / beatDivisionFactor24.toDouble()

    /**
     * @param timeInMicros of current beat
     * @param bpm beats per minute
     * @param bpmStartTimeMicros start time of bpm counting in microseconds
     * @return the number of 24th beats in long format
     */
    fun syncToNearest24thBeat(timeInMicros: Long, bpm: Int, bpmStartTimeMicros: Long): Long {
        val numberOfBeats = beatsElapsed(timeInMicros, bpm, bpmStartTimeMicros)
        val numberOf24thBeats = numberOfBeats / one24thBeat
        return round(numberOf24thBeats).toLong()
    }

    /**
     * @param timeInMicros of current beat
     * @param bpm beats per minute
     * @param bpmStartTimeMicros start time of bpm counting in microseconds
     * @return the timestamp in microseconds of the time synced to closest 24th beat
     */
    fun syncToNearest24thBeatMicros(timeInMicros: Long, bpm: Int, bpmStartTimeMicros: Long): Double {
        val roundedNumberOf24thBeats =  syncToNearest24thBeat(timeInMicros, bpm, bpmStartTimeMicros)
        val roundedNumberOfBeats = roundedNumberOf24thBeats.toDouble() / beatDivisionFactor24.toDouble()
        return roundedNumberOfBeats * microsInBeat(bpm)
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

//    BPM = 60,000,000 / MicroPerBeat
//    MicrosPerPPQN = MicrosPerBeat / TimeBase
//    MicrosPerMIDIClock = MicrosPerBeat / 24
//
//    PPQNPerMIDIClock = TimeBase / 24
//    MicrosPerSubFrame = 1000000 * Frames * SubFrames
//    SubFramesPerQuarterNote = MicrosPerBeat / (Frames * SubFrames)
//    SubFramesPerPPQN = SubFramesPerQuarterNote/TimeBase
//    MicrosPerPPQN = SubFramesPerPPQN * Frames * SubFrames
}