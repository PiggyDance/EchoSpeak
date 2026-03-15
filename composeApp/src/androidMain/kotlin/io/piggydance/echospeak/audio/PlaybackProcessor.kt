package io.piggydance.echospeak.audio

import android.content.Context

/**
 * 播放处理器 — 委托给 DeepFilterNetProcessor 做神经网络降噪。
 *
 * 职责：
 *   - 持有 DeepFilterNetProcessor 的生命周期
 *   - 对外暴露与旧版相同的 process(SpeechSegment) 接口，方便 AudioPlayer 调用
 *
 * DeepFilterNetProcessor 在构造时即开始异步加载模型（~8MB），
 * 首次调用 process() 时若模型尚未就绪则跳过降噪，直接返回原始数据。
 */
class PlaybackProcessor(context: Context) {

    private val deepFilterNet = DeepFilterNetProcessor(context)

    /**
     * 对完整语音片段做 DeepFilterNet 降噪 + 峰值归一化。
     * 返回降噪后的 PCM ByteArray（16-bit 小端序，16000 Hz，Mono）。
     */
    fun process(segment: SpeechSegment): ByteArray {
        return deepFilterNet.process(segment)
    }

    /**
     * 重置状态（DeepFilterNet 无状态，无需额外 reset）。
     */
    fun reset() {
        // DeepFilterNet 每次 processFrame 独立处理，无需手动 reset
    }

    /**
     * 释放原生资源，AudioPlayer 销毁时调用。
     */
    fun release() {
        deepFilterNet.release()
    }
}
