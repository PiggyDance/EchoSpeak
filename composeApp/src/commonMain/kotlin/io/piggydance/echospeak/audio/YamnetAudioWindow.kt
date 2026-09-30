package io.piggydance.echospeak.audio

/** The Task Audio window: newest PCM samples at the end, initially padded with silence. */
internal class YamnetAudioWindow(private val capacity: Int = SAMPLE_COUNT) {
    companion object {
        const val SAMPLE_COUNT = 15600
    }

    private val samples = FloatArray(capacity)
    private var nextIndex = 0

    init {
        require(capacity > 0)
    }

    fun appendPcm16(audio: ByteArray) {
        require(audio.size % 2 == 0) { "PCM16 requires complete samples" }
        for (i in audio.indices step 2) {
            val raw = (audio[i].toInt() and 0xff) or (audio[i + 1].toInt() shl 8)
            // Match TensorAudio's Short.MAX_VALUE normalization, including -32768.
            samples[nextIndex] = raw.toShort().toFloat() / Short.MAX_VALUE
            nextIndex = (nextIndex + 1) % capacity
        }
    }

    fun copyTo(output: FloatArray) {
        require(output.size == capacity)
        val tailCount = capacity - nextIndex
        samples.copyInto(output, 0, nextIndex, capacity)
        samples.copyInto(output, tailCount, 0, nextIndex)
    }
}
