package io.piggydance.echospeak.audio

import android.content.Context
import com.rikorose.deepfilternet.NativeDeepFilterNet
import io.piggydance.basicdeps.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * DeepFilterNet 降噪处理器
 *
 * 使用 KaleyraVideo/AndroidDeepFilterNet 提供的神经网络降噪模型。
 *
 * 关键约束：
 *   - DeepFilterNet 要求输入采样率 48000 Hz，16-bit PCM，Mono
 *   - 本应用录音采样率 16000 Hz，需要 3x 上采样 → 处理 → 3x 下采样
 *   - NativeDeepFilterNet 异步加载模型，通过 onModelLoaded 回调通知就绪
 *   - processFrame 在 ByteBuffer 上原地修改（in-place）
 */
class DeepFilterNetProcessor(context: Context) {

    companion object {
        private const val TAG = "DeepFilterNetProcessor"

        // DeepFilterNet 原生采样率和我们的采样率
        private const val DFN_SAMPLE_RATE = 48000
        private const val APP_SAMPLE_RATE = 16000
        private const val RESAMPLE_FACTOR = DFN_SAMPLE_RATE / APP_SAMPLE_RATE  // 3

        // 降噪强度（dB），范围 0~100，30dB 是较为激进但保留语音的平衡值
        private const val ATTENUATION_LIMIT = 40f

        // 目标峰值（相对 32767）
        private const val TARGET_PEAK_RATIO = 0.92f
        private const val MAX_GAIN = 12.0f
    }

    // NativeDeepFilterNet 异步加载模型，初始化后通过 onModelLoaded 回调通知就绪
    private val deepFilterNet: NativeDeepFilterNet = NativeDeepFilterNet(
        context = context,
        attenuationLimit = ATTENUATION_LIMIT,
    ).also { dfn ->
        dfn.onModelLoaded {
            Log.i(TAG, "DeepFilterNet model loaded, frameLength=${dfn.frameLength} bytes")
        }
    }

    /**
     * 对完整语音片段（SpeechSegment）进行降噪 + 峰值归一化。
     * 返回降噪后的 PCM ByteArray（格式与输入相同：16-bit 小端序，16000Hz，Mono）。
     *
     * 若模型尚未加载完成（frameLength == -1），则跳过降噪直接返回原始数据。
     */
    fun process(segment: SpeechSegment): ByteArray {
        val data = segment.pcm
        val sampleCount = data.size / 2

        if (sampleCount == 0) return data

        // 检查模型是否已加载（frameLength == -1 表示尚未就绪）
        val frameLength = deepFilterNet.frameLength.toInt()
        if (frameLength <= 0) {
            Log.w(TAG, "DeepFilterNet model not ready yet (frameLength=$frameLength), skipping denoise")
            return data
        }

        return try {
            // ── 1. 解码 PCM bytes → ShortArray (16kHz) ───────────────────────
            val pcm16 = ShortArray(sampleCount)
            for (i in 0 until sampleCount) {
                val lo = data[i * 2].toInt() and 0xFF
                val hi = data[i * 2 + 1].toInt() shl 8
                pcm16[i] = (lo or hi).toShort()
            }

            // ── 2. 上采样 16kHz → 48kHz（线性插值）─────────────────────────
            val upsampled = upsample(pcm16, RESAMPLE_FACTOR)

            // ── 3. 按帧调用 DeepFilterNet（每帧 frameLength 字节 @ 48kHz）──
            val denoised48Bytes = processFrames(upsampled, frameLength)

            // ── 4. 将处理后的字节转回 ShortArray ────────────────────────────
            val denoised48 = ShortArray(denoised48Bytes.size / 2)
            for (i in denoised48.indices) {
                val lo = denoised48Bytes[i * 2].toInt() and 0xFF
                val hi = denoised48Bytes[i * 2 + 1].toInt() shl 8
                denoised48[i] = (lo or hi).toShort()
            }

            // ── 5. 下采样 48kHz → 16kHz（每 3 样本取均值）──────────────────
            val denoised16 = downsample(denoised48, RESAMPLE_FACTOR)

            // ── 6. 峰值归一化（放大到目标响度）──────────────────────────────
            val result16 = peakNormalize(denoised16, segment.vadFrameLabels, segment.vadFrameSizeSamples)

            // ── 7. 编码回 PCM bytes ───────────────────────────────────────────
            val out = ByteArray(result16.size * 2)
            for (i in result16.indices) {
                val v = result16[i].toInt()
                out[i * 2] = (v and 0xFF).toByte()
                out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
            }

            Log.d(TAG, "Denoise done: ${data.size}B → ${out.size}B")
            out
        } catch (e: Exception) {
            Log.e(TAG, "Error during DeepFilterNet processing: ${e.message}", e)
            data  // 降噪失败时返回原始数据
        }
    }

    /**
     * 将 48kHz ShortArray 按 DeepFilterNet 帧大小分块处理。
     * frameLength 是字节数（每帧 = frameLength/2 个 short 样本）。
     */
    private fun processFrames(samples48: ShortArray, frameLength: Int): ByteArray {
        val frameSamples = frameLength / 2  // 每帧样本数
        val totalBytes = samples48.size * 2
        val outputBytes = ByteArray(totalBytes)

        // 分配一个可复用的 direct ByteBuffer（DeepFilterNet 要求 direct buffer）
        val frameBuffer = ByteBuffer.allocateDirect(frameLength).apply {
            order(ByteOrder.LITTLE_ENDIAN)
        }

        var samplePos = 0
        var byteOutPos = 0

        while (samplePos + frameSamples <= samples48.size) {
            frameBuffer.clear()

            // 将当前帧的 short 样本写入 ByteBuffer（小端序）
            for (i in 0 until frameSamples) {
                frameBuffer.putShort(samples48[samplePos + i])
            }
            frameBuffer.flip()

            // DeepFilterNet 原地处理
            deepFilterNet.processFrame(frameBuffer)

            // 读取处理后的数据
            frameBuffer.rewind()
            val frameBytes = ByteArray(frameLength)
            frameBuffer.get(frameBytes)

            // 写入输出
            val copyLen = minOf(frameLength, outputBytes.size - byteOutPos)
            if (copyLen > 0) {
                frameBytes.copyInto(outputBytes, byteOutPos, 0, copyLen)
                byteOutPos += copyLen
            }

            samplePos += frameSamples
        }

        // 尾帧（不足一帧）直接复制原始数据，不处理
        while (samplePos < samples48.size && byteOutPos + 1 < outputBytes.size) {
            val v = samples48[samplePos].toInt()
            outputBytes[byteOutPos] = (v and 0xFF).toByte()
            outputBytes[byteOutPos + 1] = ((v shr 8) and 0xFF).toByte()
            byteOutPos += 2
            samplePos++
        }

        return outputBytes
    }

    // ── 上采样：零填充（Zero-Order Hold）────────────────────────────────────
    // 相比线性插值，ZOH 不会在高频引入额外的相位失真，
    // DeepFilterNet 内部有自己的频域处理，不需要平滑的插值。
    private fun upsample(src: ShortArray, factor: Int): ShortArray {
        val out = ShortArray(src.size * factor)
        for (i in src.indices) {
            val v = src[i]
            for (j in 0 until factor) {
                out[i * factor + j] = v
            }
        }
        return out
    }

    // ── 下采样：直接抽取（Decimation）────────────────────────────────────────
    // DeepFilterNet 输出的 48kHz 信号已经是带限信号（有效带宽 ≤ 8kHz），
    // 不需要额外的低通滤波，直接每 factor 个样本取第一个即可。
    // 均值下采样等价于低通滤波，会严重衰减 2kHz 以上的高频，导致声音"闷"。
    private fun downsample(src: ShortArray, factor: Int): ShortArray {
        val outSize = src.size / factor
        val out = ShortArray(outSize)
        for (i in 0 until outSize) {
            out[i] = src[i * factor]
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
        val out = ShortArray(samples.size)
        for (i in samples.indices) {
            out[i] = (samples[i] * gain).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /**
     * 释放 DeepFilterNet 原生资源，必须在不再使用时调用。
     */
    fun release() {
        try {
            deepFilterNet.release()
            Log.i(TAG, "DeepFilterNet released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing DeepFilterNet: ${e.message}", e)
        }
    }
}
