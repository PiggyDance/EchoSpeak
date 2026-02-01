package io.piggydance.echospeak.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 音频可视化数据管理器
 * 
 * 负责收集音频数据并计算频谱用于可视化
 */
object AudioVisualizerManager {
    
    enum class AudioMode {
        IDLE,       // 空闲
        RECORDING,  // 录音中
        PLAYING     // 播放中
    }
    
    data class VisualizerData(
        val mode: AudioMode = AudioMode.IDLE,
        val spectrum: List<Float> = List(60) { 0f }  // 60个频段
    )
    
    private val _visualizerData = MutableStateFlow(VisualizerData())
    val visualizerData: StateFlow<VisualizerData> = _visualizerData.asStateFlow()
    
    /**
     * 更新录音数据
     */
    fun updateRecordingData(audioData: ByteArray) {
        val spectrum = calculateSpectrum(audioData)
        _visualizerData.value = VisualizerData(
            mode = AudioMode.RECORDING,
            spectrum = spectrum
        )
    }
    
    /**
     * 更新播放数据
     */
    fun updatePlaybackData(audioData: ByteArray) {
        val spectrum = calculateSpectrum(audioData)
        _visualizerData.value = VisualizerData(
            mode = AudioMode.PLAYING,
            spectrum = spectrum
        )
    }
    
    /**
     * 重置为空闲状态
     */
    fun reset() {
        _visualizerData.value = VisualizerData(
            mode = AudioMode.IDLE,
            spectrum = List(60) { 0f }
        )
    }
    
    /**
     * 计算音频频谱
     * 
     * 简化版本：将音频数据分成多个频段，计算每个频段的能量
     */
    private fun calculateSpectrum(audioData: ByteArray): List<Float> {
        if (audioData.isEmpty()) {
            return List(60) { 0f }
        }
        
        val barCount = 60
        val samplesPerBar = (audioData.size / 2) / barCount  // 16-bit = 2 bytes per sample
        
        if (samplesPerBar == 0) {
            return List(60) { 0f }
        }
        
        return List(barCount) { barIndex ->
            val startSample = barIndex * samplesPerBar
            val endSample = minOf((barIndex + 1) * samplesPerBar, audioData.size / 2)
            
            var sum = 0.0
            var count = 0
            
            for (i in startSample until endSample) {
                val byteIndex = i * 2
                if (byteIndex + 1 < audioData.size) {
                    // 小端序：低字节在前，高字节在后
                    val sample = (audioData[byteIndex].toInt() and 0xFF) or
                                ((audioData[byteIndex + 1].toInt() and 0xFF) shl 8)
                    
                    // 转换为有符号 16-bit 值
                    val signedSample = if (sample > 32767) sample - 65536 else sample
                    
                    sum += signedSample * signedSample
                    count++
                }
            }
            
            if (count == 0) {
                0f
            } else {
                // 计算 RMS 并归一化到 0-1 范围
                val rms = sqrt(sum / count)
                val normalized = (rms / 32768.0).toFloat()
                
                // 应用对数缩放使低音量更可见
                val scaled = sqrt(normalized.toDouble()).toFloat()
                
                // 限制在 0-1 范围
                scaled.coerceIn(0f, 1f)
            }
        }
    }
}
