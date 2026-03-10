package io.piggydance.echospeak.audio

import kotlin.math.abs

/**
 * 简单噪声门
 * 
 * 作为降噪的补充方案，通过阈值过滤低音量噪声
 * 只在音频效果不可用时作为备选方案
 */
class SimpleNoiseGate(
    private val threshold: Double = 300.0,  // 噪声门阈值
    private val attackTime: Int = 2,         // 开门时间（帧数）
    private val releaseTime: Int = 5         // 关门时间（帧数）
) {
    private var gateOpen = false
    private var attackCounter = 0
    private var releaseCounter = 0
    
    /**
     * 处理音频帧
     * 
     * @param audioData 16-bit PCM 音频数据
     * @return 处理后的音频数据
     */
    fun process(audioData: ByteArray): ByteArray {
        // 计算RMS音量
        val rms = calculateRMS(audioData)
        
        // 噪声门逻辑
        when {
            rms > threshold -> {
                // 信号超过阈值，增加攻击计数
                attackCounter++
                releaseCounter = 0
                
                if (attackCounter >= attackTime) {
                    gateOpen = true
                }
            }
            else -> {
                // 信号低于阈值，增加释放计数
                releaseCounter++
                attackCounter = 0
                
                if (releaseCounter >= releaseTime) {
                    gateOpen = false
                }
            }
        }
        
        // 应用门控
        return if (gateOpen) {
            audioData  // 门开，保留信号
        } else {
            // 门关，衰减信号（不是完全静音，避免咔嚓声）
            applyAttenuation(audioData, 0.1f)
        }
    }
    
    /**
     * 计算RMS音量
     */
    private fun calculateRMS(audioData: ByteArray): Double {
        var sum = 0.0
        var count = 0
        
        for (i in 0 until audioData.size - 1 step 2) {
            val sample = (audioData[i].toInt() and 0xFF) or
                        ((audioData[i + 1].toInt() and 0xFF) shl 8)
            val signedSample = if (sample > 32767) sample - 65536 else sample
            
            sum += signedSample * signedSample
            count++
        }
        
        return if (count > 0) {
            kotlin.math.sqrt(sum / count)
        } else {
            0.0
        }
    }
    
    /**
     * 应用衰减
     */
    private fun applyAttenuation(audioData: ByteArray, factor: Float): ByteArray {
        val result = ByteArray(audioData.size)
        
        for (i in 0 until audioData.size - 1 step 2) {
            val sample = (audioData[i].toInt() and 0xFF) or
                        ((audioData[i + 1].toInt() and 0xFF) shl 8)
            val signedSample = if (sample > 32767) sample - 65536 else sample
            
            val attenuated = (signedSample * factor).toInt()
            
            result[i] = (attenuated and 0xFF).toByte()
            result[i + 1] = ((attenuated shr 8) and 0xFF).toByte()
        }
        
        return result
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        gateOpen = false
        attackCounter = 0
        releaseCounter = 0
    }
}
