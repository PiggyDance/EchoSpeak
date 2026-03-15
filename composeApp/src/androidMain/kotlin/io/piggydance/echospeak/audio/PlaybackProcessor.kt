package io.piggydance.echospeak.audio

/**
 * 播放处理器 — 委托给 RnnoiseProcessor 做真正的 RNN 神经网络降噪。
 *
 * 职责：
 *   - 持有 RnnoiseProcessor 的生命周期
 *   - 对外暴露与旧版相同的 process(SpeechSegment) 接口，方便 AudioPlayer 调用
 */
class PlaybackProcessor {

    private val rnnoise = RnnoiseProcessor()

    /**
     * 对完整语音片段做 RNNoise 降噪 + 峰值归一化。
     * 返回降噪后的 PCM ByteArray（16-bit 小端序，16000 Hz，Mono）。
     */
    fun process(segment: SpeechSegment): ByteArray {
        return rnnoise.process(segment)
    }

    /**
     * 重置状态（目前 RnnoiseProcessor 内部按帧处理，无需额外 reset）。
     */
    fun reset() {
        // RNNoise 状态在 RnnoiseProcessor 创建时已初始化，无需手动 reset
    }

    /**
     * 释放原生资源，AudioPlayer 销毁时调用。
     */
    fun release() {
        rnnoise.release()
    }
}
