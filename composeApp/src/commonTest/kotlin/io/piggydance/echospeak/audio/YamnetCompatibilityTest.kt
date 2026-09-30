package io.piggydance.echospeak.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class YamnetCompatibilityTest {
    private fun pcm(vararg samples: Short): ByteArray = ByteArray(samples.size * 2).also { bytes ->
        samples.forEachIndexed { i, sample ->
            bytes[2 * i] = sample.toByte()
            bytes[2 * i + 1] = (sample.toInt() shr 8).toByte()
        }
    }

    @Test
    fun windowPadsOnLeftAndRetainsEverySampleAcrossWraps() {
        val window = YamnetAudioWindow(5)
        val output = FloatArray(5)
        window.appendPcm16(pcm(1, 2))
        window.copyTo(output)
        assertContentEquals(floatArrayOf(0f, 0f, 0f, 1f / 32767, 2f / 32767), output)
        window.appendPcm16(pcm(3, 4, 5, 6))
        window.copyTo(output)
        assertContentEquals(floatArrayOf(2f, 3f, 4f, 5f, 6f).map { it / 32767 }.toFloatArray(), output)
        window.appendPcm16(pcm(7, 8, 9, 10, 11, 12, 13))
        window.copyTo(output)
        assertContentEquals(floatArrayOf(9f, 10f, 11f, 12f, 13f).map { it / 32767 }.toFloatArray(), output)
    }

    @Test
    fun windowUsesOriginalPcmNormalizationAndRejectsHalfSamples() {
        val window = YamnetAudioWindow(3)
        window.appendPcm16(pcm(Short.MIN_VALUE, 0, Short.MAX_VALUE))
        val output = FloatArray(3)
        window.copyTo(output)
        assertEquals(-32768f / 32767f, output[0])
        assertEquals(0f, output[1])
        assertEquals(1f, output[2])
        assertFailsWith<IllegalArgumentException> { window.appendPcm16(byteArrayOf(1)) }
    }

    @Test
    fun speechMustBeTopClassAndStrictlyAboveNormalThreshold() {
        val gate = YamnetSpeechGate(0, 0)
        val scores = FloatArray(521)
        scores[0] = 0.3f
        assertFalse(gate.isSpeech(scores))
        scores[0] = 0.31f
        assertTrue(gate.isSpeech(scores))
        scores[1] = 0.8f // Child speech remains a distinct class, matching the original top-one filter.
        assertFalse(gate.isSpeech(scores))
        assertFailsWith<IllegalArgumentException> { gate.isSpeech(FloatArray(520)) }
    }

    @Test
    fun continuousGatePreservesSpeechAndSilenceDurations() {
        val gate = YamnetSpeechGate(speechDurationMs = 300, silenceDurationMs = 200)
        val speech = FloatArray(521).apply { this[0] = 0.9f }
        val silence = FloatArray(521)
        repeat(20) { assertFalse(gate.isSpeech(speech)) }
        assertTrue(gate.isSpeech(speech))
        repeat(13) { assertTrue(gate.isSpeech(silence)) }
        assertFalse(gate.isSpeech(silence))
        assertFalse(gate.isSpeech(speech))
    }
}
