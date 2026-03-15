package io.piggydance.echospeak.audio

import kotlin.math.abs

/**
 * RNNoise 降噪处理器
 *
 * RNNoise 是 Mozilla 开源的 RNN 神经网络降噪库，效果接近商用级别。
 *
 * 关键约束：
 *   - RNNoise 要求输入采样率 48000 Hz，帧大小固定 480 样本（10ms）
 *   - 本应用录音采样率 16000 Hz，需要 3x 上采样 → 处理 → 3x 下采样
 *   - 输入格式：16-bit PCM ShortArray，范围 [-32768, 32767]
 */
class RnnoiseProcessor {

    companion object {
        init {
            System.loadLibrary("echospeak_rnnoise")
        }

        // RNNoise 原生采样率和我们的采样率
        private const val RNNOISE_SAMPLE_RATE = 48000
        private const val APP_SAMPLE_RATE     = 16000
        private const val RESAMPLE_FACTOR     = RNNOISE_SAMPLE_RATE / APP_SAMPLE_RATE  // 3

        // 目标峰值（相对 32767）
        private const val TARGET_PEAK_RATIO = 0.92f
        private const val MAX_GAIN          = 12.0f
    }

    // 原生 DenoiseState 的不透明指针（jlong）
    private var nativeHandle: Long = 0L

    // RNNoise 帧大小（在 48kHz 下，一般为 480）
    private val rnnoiseFrameSize: Int by lazy { nativeGetFrameSize() }

    // 对应到 16kHz 下每次需要喂入的样本数
    private val appFrameSize: Int get() = rnnoiseFrameSize / RESAMPLE_FACTOR

    init {
        nativeHandle = nativeCreate()
    }

    /**
     * 对完整语音片段（SpeechSegment）进行降噪 + 峰值归一化。
     * 返回降噪后的 PCM ByteArray（格式与输入相同：16-bit 小端序）。
     */
    fun process(segment: SpeechSegment): ByteArray {
        val data        = segment.pcm
        val sampleCount = data.size / 2
        if (sampleCount == 0 || nativeHandle == 0L) return data

        // ── 1. 解码 PCM bytes → ShortArray ───────────────────────────────────
        val pcm16 = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            val lo = data[i * 2].toInt() and 0xFF
            val hi = data[i * 2 + 1].toInt() shl 8
            pcm16[i] = (lo or hi).toShort()
        }

        // ── 2. 上采样 16kHz → 48kHz（线性插值）─────────────────────────────
        val upsampled = upsample(pcm16, RESAMPLE_FACTOR)

        // ── 3. 按帧调用 RNNoise（每帧 480 样本 @ 48kHz）──────────────────────
        val denoised48 = ShortArray(upsampled.size)
        val frameSize  = rnnoiseFrameSize
        var pos        = 0
        while (pos + frameSize <= upsampled.size) {
            nativeProcessFrame(
                nativeHandle,
                upsampled, pos,
                denoised48, pos,
                frameSize
            )
            pos += frameSize
        }
        // 尾帧（不足一帧）直接复制，不处理
        while (pos < upsampled.size) {
            denoised48[pos] = upsampled[pos]
            pos++
        }

        // ── 4. 下采样 48kHz → 16kHz（每 3 样本取均值）──────────────────────
        val denoised16 = downsample(denoised48, RESAMPLE_FACTOR)

        // ── 5. 峰值归一化（放大到目标响度）──────────────────────────────────
        val result16 = peakNormalize(denoised16, segment.vadFrameLabels, segment.vadFrameSizeSamples)

        // ── 6. 编码回 PCM bytes ───────────────────────────────────────────────
        val out = ByteArray(result16.size * 2)
        for (i in result16.indices) {
            val v = result16[i].toInt()
            out[i * 2]     = (v and 0xFF).toByte()
            out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }

    /**
     * 释放 RNNoise 原生资源，必须在不再使用时调用。
     */
    fun release() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0L
        }
    }

    // ── 线性插值上采样 ────────────────────────────────────────────────────────
    private fun upsample(src: ShortArray, factor: Int): ShortArray {
        val out = ShortArray(src.size * factor)
        for (i in src.indices) {
            val curr = src[i].toFloat()
            val next = if (i + 1 < src.size) src[i + 1].toFloat() else curr
            for (j in 0 until factor) {
                val t = j.toFloat() / factor
                out[i * factor + j] = (curr + t * (next - curr)).toInt()
                    .coerceIn(-32768, 32767).toShort()
            }
        }
        return out
    }

    // ── 均值下采样 ────────────────────────────────────────────────────────────
    private fun downsample(src: ShortArray, factor: Int): ShortArray {
        val outSize = src.size / factor
        val out = ShortArray(outSize)
        for (i in 0 until outSize) {
            var sum = 0
            for (j in 0 until factor) sum += src[i * factor + j].toInt()
            out[i] = (sum / factor).coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    // ── 基于人声帧的峰值归一化 ────────────────────────────────────────────────
    private fun peakNormalize(
        samples: ShortArray,
        vadLabels: BooleanArray,
        vadFrameSamples: Int
    ): ShortArray {
        val targetPeak = 32767f * TARGET_PEAK_RATIO
        var speechPeak = 0f
        for (i in samples.indices) {
            val f = (i / vadFrameSamples).coerceAtMost(vadLabels.size - 1)
            if (vadLabels.getOrElse(f) { false }) {
                val a = abs(samples[i].toFloat())
                if (a > speechPeak) speechPeak = a
            }
        }
        if (speechPeak < 100f) return samples  // 信号太弱，不做归一化

        val gain = (targetPeak / speechPeak).coerceIn(1.0f, MAX_GAIN)
        val out  = ShortArray(samples.size)
        for (i in samples.indices) {
            out[i] = (samples[i] * gain).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    // ── Native 方法声明 ───────────────────────────────────────────────────────
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeGetFrameSize(): Int
    private external fun nativeProcessFrame(
        handle: Long,
        inBuf: ShortArray, offset: Int,
        outBuf: ShortArray, outOffset: Int,
        length: Int
    ): Float
}
