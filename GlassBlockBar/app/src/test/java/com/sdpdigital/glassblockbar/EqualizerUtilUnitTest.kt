package com.sdpdigital.glassblockbar

import com.sdpdigital.glassblockbar.ble.Utils
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class EqualizerUtilUnitTest {

    val LOG_TAG = EqualizerUtilUnitTest::class.java.simpleName

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
            arrayOf(1, 0, 1, 1, 1, 1, 2, 2, 5, 0, 0, 0)
    )

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
}