package io.piggydance.echospeak.audio

/** Preserves NORMAL mode, top-one classification and the original continuous speech gate. */
internal class YamnetSpeechGate(
    speechDurationMs: Int,
    silenceDurationMs: Int,
) {
    companion object {
        const val CLASS_COUNT = 521
        const val SPEECH_CLASS = 0
        const val SCORE_THRESHOLD = 0.3f
        private const val FRAME_DURATION_MS = 243 / (16000 / 1000)
    }

    private val maxSpeechFrames = speechDurationMs / FRAME_DURATION_MS
    private val maxSilenceFrames = silenceDurationMs / FRAME_DURATION_MS
    private var speechFrames = 0
    private var silenceFrames = 0

    init {
        require(speechDurationMs in 0..300000 && silenceDurationMs in 0..300000)
    }

    fun isSpeech(scores: FloatArray): Boolean {
        require(scores.size == CLASS_COUNT) { "Expected all 521 YAMNet class scores" }
        val bestClass = scores.indices.maxByOrNull { scores[it] }!!
        val detected = bestClass == SPEECH_CLASS && scores[bestClass] > SCORE_THRESHOLD
        if (detected) {
            if (speechFrames <= maxSpeechFrames) speechFrames++
            if (speechFrames > maxSpeechFrames) {
                silenceFrames = 0
                return true
            }
        } else {
            if (silenceFrames <= maxSilenceFrames) silenceFrames++
            if (silenceFrames > maxSilenceFrames) {
                speechFrames = 0
                return false
            } else if (speechFrames > maxSpeechFrames) {
                return true
            }
        }
        return false
    }
}
