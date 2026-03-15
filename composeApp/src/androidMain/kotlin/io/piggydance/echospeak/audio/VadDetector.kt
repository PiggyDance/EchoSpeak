package io.piggydance.echospeak.audio

import android.content.Context
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize as SileroFrameSize
import com.konovalov.vad.silero.config.Mode as SileroMode
import com.konovalov.vad.silero.config.SampleRate as SileroSampleRate
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize as WebRtcFrameSize
import com.konovalov.vad.webrtc.config.Mode as WebRtcMode
import com.konovalov.vad.webrtc.config.SampleRate as WebRtcSampleRate
import com.konovalov.vad.yamnet.VadYamnet
import com.konovalov.vad.yamnet.config.FrameSize as YamnetFrameSize
import com.konovalov.vad.yamnet.config.Mode as YamnetMode
import com.konovalov.vad.yamnet.config.SampleRate as YamnetSampleRate
import io.piggydance.basicdeps.Log
import kotlin.math.sqrt

/**
 * VAD (Voice Activity Detection) 人声检测器
 *
 * 支持三种引擎（通过 [VadType] 选择）：
 * - [VadType.SILERO]  : DNN/ONNX，帧 512 样本，高精度
 * - [VadType.WEBRTC]  : GMM，帧 320 样本，轻量快速
 * - [VadType.YAMNET]  : DNN/TFLite，帧 243 样本，分类 521 种声音
 *
 * @param type              VAD 引擎类型，默认 Silero
 * @param silenceDurationMs VAD 内部静默持续时间阈值（ms）
 * @param speechDurationMs  VAD 内部人声持续时间阈值（ms）
 */
class VadDetector(
    val type: VadType = VadType.SILERO,
    private val silenceDurationMs: Int = 300,
    private val speechDurationMs: Int = 200,
) {
    private var sileroVad: VadSilero? = null
    private var webrtcVad: VadWebRTC? = null
    private var yamnetVad: VadYamnet? = null

    var isInitialized: Boolean = false
        private set

    companion object {
        const val SAMPLE_RATE = 16000

        // 各引擎对应的帧大小（样本数）@ 16kHz
        const val SILERO_FRAME_SAMPLES = 512  // 32ms
        const val WEBRTC_FRAME_SAMPLES = 320  // 20ms
        const val YAMNET_FRAME_SAMPLES = 243  // ~15ms
    }

    /** 当前引擎每帧的样本数 */
    val frameSizeSamples: Int
        get() = when (type) {
            VadType.SILERO -> SILERO_FRAME_SAMPLES
            VadType.WEBRTC -> WEBRTC_FRAME_SAMPLES
            VadType.YAMNET -> YAMNET_FRAME_SAMPLES
        }

    /** 当前引擎每帧的字节数（16-bit PCM = 2 bytes/sample） */
    val frameSizeBytes: Int
        get() = frameSizeSamples * 2

    /**
     * 初始化 VAD 引擎（需要在检测前调用）
     * Silero / YAMNet 需要 Context（加载模型文件）；WebRTC 不需要。
     */
    fun initialize(context: Context) {
        when (type) {
            VadType.SILERO -> {
                sileroVad = Vad.builder()
                    .setContext(context)
                    .setSampleRate(SileroSampleRate.SAMPLE_RATE_16K)
                    .setFrameSize(SileroFrameSize.FRAME_SIZE_512)
                    .setMode(SileroMode.NORMAL)
                    .setSilenceDurationMs(silenceDurationMs)
                    .setSpeechDurationMs(speechDurationMs)
                    .build()
                Log.i("VadDetector", "Silero VAD initialized (frame=${frameSizeBytes}B, ${frameSizeSamples} samples)")
            }
            VadType.WEBRTC -> {
                webrtcVad = VadWebRTC(
                    WebRtcSampleRate.SAMPLE_RATE_16K,
                    WebRtcFrameSize.FRAME_SIZE_320,
                    WebRtcMode.VERY_AGGRESSIVE,
                    silenceDurationMs,
                    speechDurationMs,
                )
                Log.i("VadDetector", "WebRTC VAD initialized (frame=${frameSizeBytes}B, ${frameSizeSamples} samples)")
            }
            VadType.YAMNET -> {
                yamnetVad = VadYamnet(
                    context,
                    YamnetSampleRate.SAMPLE_RATE_16K,
                    YamnetFrameSize.FRAME_SIZE_243,
                    YamnetMode.NORMAL,
                    silenceDurationMs,
                    speechDurationMs,
                )
                Log.i("VadDetector", "YAMNet VAD initialized (frame=${frameSizeBytes}B, ${frameSizeSamples} samples)")
            }
        }
        isInitialized = true
    }

    /**
     * 检测音频帧是否包含人声
     *
     * @param audioFrame PCM 16-bit 小端，大小必须等于 [frameSizeBytes]
     * @return true = 检测到人声
     */
    fun isSpeech(audioFrame: ByteArray): Boolean {
        require(audioFrame.size == frameSizeBytes) {
            "Audio frame must be $frameSizeBytes bytes for ${type.displayName}, got ${audioFrame.size}"
        }
        return when (type) {
            VadType.SILERO -> sileroVad?.isSpeech(audioFrame) ?: false
            VadType.WEBRTC -> webrtcVad?.isSpeech(audioFrame) ?: false
            VadType.YAMNET -> {
                // YAMNet 通过分类声音类别判断，返回置信度最高的 SoundCategory
                val category = yamnetVad?.classifyAudio("Speech", audioFrame)
                category?.label == "Speech"
            }
        }
    }

    /**
     * 计算音频帧的 RMS 音量（用于日志/调试）
     */
    fun getVolume(audioFrame: ByteArray): Double = calculateRMS(audioFrame)

    private fun calculateRMS(audioFrame: ByteArray): Double {
        var sum = 0.0
        for (i in 0 until audioFrame.size step 2) {
            val raw = (audioFrame[i].toInt() and 0xFF) or ((audioFrame[i + 1].toInt() and 0xFF) shl 8)
            val signed = if (raw > 32767) raw - 65536 else raw
            sum += signed.toDouble() * signed.toDouble()
        }
        return sqrt(sum / frameSizeSamples)
    }

    /**
     * 释放 VAD 资源
     */
    fun release() {
        sileroVad?.close()
        sileroVad = null
        webrtcVad?.close()
        webrtcVad = null
        yamnetVad?.close()
        yamnetVad = null
        isInitialized = false
        Log.i("VadDetector", "VAD released")
    }
}
