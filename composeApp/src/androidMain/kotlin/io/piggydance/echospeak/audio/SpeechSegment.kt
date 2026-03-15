package io.piggydance.echospeak.audio

/**
 * 语音片段：录制的 PCM 数据 + 每帧 VAD 标签
 *
 * @param pcm               原始 PCM 数据（16-bit，小端序，Mono，16000Hz）
 * @param vadFrameLabels    每个 VAD 帧是否为人声（录音阶段的检测结果）
 * @param vadFrameSizeSamples VAD 帧大小（样本数），用于将样本索引映射回帧标签
 */
data class SpeechSegment(
    val pcm: ByteArray,
    val vadFrameLabels: BooleanArray,
    val vadFrameSizeSamples: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpeechSegment) return false
        return pcm.contentEquals(other.pcm) &&
               vadFrameLabels.contentEquals(other.vadFrameLabels) &&
               vadFrameSizeSamples == other.vadFrameSizeSamples
    }

    override fun hashCode(): Int {
        var result = pcm.contentHashCode()
        result = 31 * result + vadFrameLabels.contentHashCode()
        result = 31 * result + vadFrameSizeSamples
        return result
    }
}
