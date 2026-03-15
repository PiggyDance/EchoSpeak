package io.piggydance.echospeak.audio

import android.content.Context
import com.konovalov.vad.silero.Vad
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate
import kotlin.math.sqrt

/**
 * VAD (Voice Activity Detection) 人声检测器
 *
 * 封装 WebRTC VAD 的配置和检测逻辑，并增加音量阈值过滤
 *
 * @param mode VAD 检测模式，越激进越容易判定为人声
 * @param silenceDurationMs VAD 内部静默持续时间阈值
 * @param speechDurationMs VAD 内部人声持续时间阈值
 * @param volumeThreshold 音量阈值(RMS)，低于此值的声音被视为环境噪音
 */
class VadDetector(
    private val mode: Mode = Mode.AGGRESSIVE,
    private val silenceDurationMs: Int = 300,
    private val speechDurationMs: Int = 200,
) {
    var vad: VadSilero? = null
        private set
    
    companion object {
        const val SAMPLE_RATE = 16000
        const val FRAME_SIZE_SAMPLES = 512  // 32ms @ 16kHz (匹配 Silero VAD 模型要求)
        const val FRAME_SIZE_BYTES = FRAME_SIZE_SAMPLES * 2  // 16-bit = 2 bytes per sample
    }
    
    /**
     * 初始化 VAD 检测器
     */
    fun initialize(context: Context) {
        vad = Vad.builder()
            .setContext(context)
            .setSampleRate(SampleRate.SAMPLE_RATE_16K)
            .setFrameSize(FrameSize.FRAME_SIZE_512)
            .setMode(mode)
            .setSilenceDurationMs(silenceDurationMs)  // 使用构造函数传入的参数
            .setSpeechDurationMs(speechDurationMs)  // 使用构造函数传入的参数
            .build()
    }
    
    /**
     * 检测音频帧是否包含人声
     *
     * 结合 VAD 检测和音量阈值，只有音量足够大且 VAD 判定为人声时才返回 true
     *
     * @param audioFrame 音频帧数据，必须是 FRAME_SIZE_BYTES 大小
     * @return true 表示检测到人声，false 表示静音或环境噪音
     */
    fun isSpeech(audioFrame: ByteArray): Boolean {
        require(audioFrame.size == FRAME_SIZE_BYTES) {
            "Audio frame size must be $FRAME_SIZE_BYTES bytes"
        }

        // 直接由 Silero VAD 模型判断是否有人声，不加音量阈值拦截。
        // 音量阈值会导致小声说话被误判为静音，VAD 模型本身对人声/噪音的区分已足够。
        return vad?.isSpeech(audioFrame) ?: false
    }
    
    /**
     * 获取当前音频帧的音量（用于日志）
     */
    fun getVolume(audioFrame: ByteArray): Double {
        return calculateRMS(audioFrame)
    }
    
    /**
     * 计算音频帧的 RMS (Root Mean Square) 音量
     *
     * @param audioFrame 音频帧数据（16-bit PCM）
     * @return RMS 音量值
     */
    private fun calculateRMS(audioFrame: ByteArray): Double {
        var sum = 0.0
        
        // 将字节数组转换为 16-bit 采样值并计算平方和
        for (i in 0 until audioFrame.size step 2) {
            // 小端序：低字节在前，高字节在后
            val sample = (audioFrame[i].toInt() and 0xFF) or
                        ((audioFrame[i + 1].toInt() and 0xFF) shl 8)
            
            // 转换为有符号 16-bit 值
            val signedSample = if (sample > 32767) sample - 65536 else sample
            
            sum += signedSample * signedSample
        }
        
        // 计算均方根
        return sqrt(sum / FRAME_SIZE_SAMPLES)
    }
    
    /**
     * 释放 VAD 资源
     */
    fun release() {
        vad?.close()
        vad = null
    }
}
