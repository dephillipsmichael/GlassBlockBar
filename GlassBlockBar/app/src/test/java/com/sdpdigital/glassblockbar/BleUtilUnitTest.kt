package com.sdpdigital.glassblockbar

import com.sdpdigital.glassblockbar.ble.Utils
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BleUtilUnitTest {

    val LOG_TAG = BleUtilUnitTest::class.java.simpleName

    val testCases = arrayOf(
            arrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1),
            arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2),
            arrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3),
            arrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4),
            arrayOf(5, 5, 5, 5, 5, 5, 5, 5, 5),
            arrayOf(4, 3, 2, 1, 0, 4, 3, 2, 1),
            arrayOf(0, 1, 2, 3, 4, 0, 1, 2, 3),
            arrayOf(0, 1, 2, 3, 4, 5, 1, 2, 3),
            arrayOf(5, 4, 3, 2, 1, 0, 5, 4, 3),
            arrayOf(1, 0, 1, 1, 1, 1, 2, 2, 5, 0, 0, 0))

    @Test
    fun eq_bitwise_transform_isCorrect() {

        for (test in testCases) {
            val og = test

            val ogBinary = og.map { "$it, " }
            println("Test values $ogBinary")

            val converted = Utils.convertEqBytesToMinimalBytes(og)

            val debugBinary = converted.map { Integer.toBinaryString(it) }
            println("Binary values $debugBinary")

            val debugInt = converted.map { "${it.toUInt()}, " }
            println("Int values $debugInt")

            val convertedByte = Array(4) { converted[it].toUByte().toInt() }

            val debugConvertedByte = converted.map { "$it, " }
            println("Converted byte values $debugConvertedByte")

            for (i in 0 until 4) {
                assertEquals(convertedByte[i], converted[i])
            }
            val ogRecreated = Utils.convertMinimalBytesToEqBytes(converted)

            val debugRecreated = ogRecreated.map { "$it " }
            println("Re-created values $debugRecreated")

            for (i in 0 until 9) {
                assertEquals(og[i], ogRecreated[i])
            }
        }
    }

    // Beats tapped as quarter notes in 4/4
    val fourFourBeatTapsMsg = listOf(
            listOf(2, 0, 0, 24, 24, 24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    val fourFourBeatTaps = listOf(
            0, 24, 48, 72)

    // Last beat tapped 16/16
    val sixteenSixteenLastBeatTapMsg = listOf(
            listOf(2, 0, 255, 105, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    val sixteenSixteenLastBeatTap = listOf(
            360)

    // Last beat tapped 16/16
    val sixteenSixteenSixteenthBeatsMsg = listOf(
            listOf(2, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6),
            listOf(3, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6),
            listOf(3, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6),
            listOf(3, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0))
    val sixteenSixteenSixteenthBeats = listOf(
            0, 6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72, 78, 84, 90, 96, 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, 198, 204, 210, 216, 222, 228, 234, 240, 246, 252, 258, 264, 270, 276, 282, 288, 294, 300, 306, 312, 318, 324, 330, 336, 342, 348, 354, 360, 366, 372, 378)

    // 17 3nd beats and then a long rest and then a beat
    val carryOverBeatsMsg = listOf(
            listOf(2, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 255),
            listOf(3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    val carryOverBeats = listOf(
            0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45, 48, 303)

    // 17 3nd beats and then a long rest and then a beat
    val carryOverBeats2Msg = listOf(
            listOf(2, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 255, 0, 0))
    val carryOverBeats2 = listOf(
            0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 297)

    @Test
    fun encodeMeasureOfQuantizedBeats_4_4_Beats_Test() {
        val actualMsg = Utils
                .encodeBeatSequence(0, fourFourBeatTaps)

        var actualDecoded = Utils.DecodedReturnValue(arrayOf(), 0)
        for (i in fourFourBeatTapsMsg.indices) {

            val encodedMessageArray = fourFourBeatTapsMsg[i].toIntArray()
            // Perform encoding check
            Assert.assertArrayEquals(actualMsg[i], encodedMessageArray)

            // Fill up array for decoding check
            actualDecoded = Utils.decodeAndAppendBeatSequence(
                    encodedMessageArray.toTypedArray(), encodedMessageArray.size,
                    actualDecoded.beatSequence, actualDecoded.beatSequenceSize, actualDecoded.startBeatValue)
        }
        // Perform decoding check
        assertEquals(fourFourBeatTaps.size, actualDecoded.beatSequenceSize)
        Assert.assertArrayEquals(fourFourBeatTaps.toIntArray(), actualDecoded.beatSequence.toIntArray())
    }

    @Test
    fun encodeMeasureOfQuantizedBeats_16_16_15thBeat_Test() {
        val actualMsg = Utils
                .encodeBeatSequence(0, sixteenSixteenLastBeatTap)

        var actualDecoded = Utils.DecodedReturnValue(arrayOf(), 0)
        for (i in sixteenSixteenLastBeatTapMsg.indices) {
            val encodedMessageArray = sixteenSixteenLastBeatTapMsg[i].toIntArray()

            // Perform encoding check
            Assert.assertArrayEquals(actualMsg[i], encodedMessageArray)

            // Fill up array for decoding check
            actualDecoded = Utils.decodeAndAppendBeatSequence(
                    encodedMessageArray.toTypedArray(), encodedMessageArray.size,
                    actualDecoded.beatSequence, actualDecoded.beatSequenceSize, actualDecoded.startBeatValue)
        }

        // Perform decoding check
        assertEquals(sixteenSixteenLastBeatTap.size, actualDecoded.beatSequenceSize)
        Assert.assertArrayEquals(sixteenSixteenLastBeatTap.toIntArray(), actualDecoded.beatSequence.toIntArray())
    }

    @Test
    fun encodeMeasureOfQuantizedBeats_16_16_All16thBeats_Test() {
        val actualMsg = Utils
                .encodeBeatSequence(0, sixteenSixteenSixteenthBeats)

        var actualDecoded = Utils.DecodedReturnValue(arrayOf(), 0)
        for (i in sixteenSixteenSixteenthBeatsMsg.indices) {
            val encodedMessageArray = sixteenSixteenSixteenthBeatsMsg[i].toIntArray()

            // Perform encoding check
            Assert.assertArrayEquals(actualMsg[i], encodedMessageArray)

            // Fill up array for decoding check
            actualDecoded = Utils.decodeAndAppendBeatSequence(
                    encodedMessageArray.toTypedArray(), encodedMessageArray.size,
                    actualDecoded.beatSequence, actualDecoded.beatSequenceSize, actualDecoded.startBeatValue)
        }

        // Perform decoding check
        assertEquals(sixteenSixteenSixteenthBeats.size, actualDecoded.beatSequenceSize)
        Assert.assertArrayEquals(sixteenSixteenSixteenthBeats.toIntArray(), actualDecoded.beatSequence.toIntArray())
    }

    @Test
    fun encodeMeasureOfQuantizedBeats_CarryOver_Beats_Test() {
        val actualMsg = Utils
                .encodeBeatSequence(0, carryOverBeats)

        var actualDecoded = Utils.DecodedReturnValue(arrayOf(), 0)
        for (i in carryOverBeatsMsg.indices) {

            val encodedMessageArray = carryOverBeatsMsg[i].toIntArray()
            // Perform encoding check
            Assert.assertArrayEquals(actualMsg[i], encodedMessageArray)

            // Fill up array for decoding check
            actualDecoded = Utils.decodeAndAppendBeatSequence(
                    encodedMessageArray.toTypedArray(), encodedMessageArray.size,
                    actualDecoded.beatSequence, actualDecoded.beatSequenceSize, actualDecoded.startBeatValue)
        }
        // Perform decoding check
        assertEquals(carryOverBeats.size, actualDecoded.beatSequenceSize)
        Assert.assertArrayEquals(carryOverBeats.toIntArray(), actualDecoded.beatSequence.toIntArray())
    }

    @Test
    fun encodeMeasureOfQuantizedBeats_CarryOver2_Beats_Test() {
        val actualMsg = Utils
                .encodeBeatSequence(0, carryOverBeats2)

        var actualDecoded = Utils.DecodedReturnValue(arrayOf(), 0)
        for (i in carryOverBeats2Msg.indices) {

            val encodedMessageArray = carryOverBeats2Msg[i].toIntArray()
            // Perform encoding check
            Assert.assertArrayEquals(actualMsg[i], encodedMessageArray)

            // Fill up array for decoding check
            actualDecoded = Utils.decodeAndAppendBeatSequence(
                    encodedMessageArray.toTypedArray(), encodedMessageArray.size,
                    actualDecoded.beatSequence, actualDecoded.beatSequenceSize, actualDecoded.startBeatValue)
        }
        // Perform decoding check
        assertEquals(carryOverBeats2.size, actualDecoded.beatSequenceSize)
        Assert.assertArrayEquals(carryOverBeats2.toIntArray(), actualDecoded.beatSequence.toIntArray())
    }
}