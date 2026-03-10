package io.piggydance.echospeak.audio

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import io.piggydance.basicdeps.Log

/**
 * 音频效果处理器
 * 
 * 使用Android系统内置的音频效果：
 * - NoiseSuppressor: 噪声抑制 (基于硬件优化)
 * - AcousticEchoCanceler: 回声消除
 * - AutomaticGainControl: 自动增益控制
 * 
 * 这些都是系统级优化，比手搓的降噪算法效果好得多
 */
class AudioEffectsProcessor(private val audioSessionId: Int) {
    
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null
    
    /**
     * 初始化音频效果
     */
    fun initialize() {
        try {
            Log.i("AudioEffects", "Starting initialization for audio session: $audioSessionId")
            
            // 1. 噪声抑制 - 核心功能
            if (NoiseSuppressor.isAvailable()) {
                try {
                    noiseSuppressor = NoiseSuppressor.create(audioSessionId)
                    if (noiseSuppressor != null) {
                        noiseSuppressor?.enabled = true
                        Log.i("AudioEffects", "✓ NoiseSuppressor initialized successfully")
                        Log.i("AudioEffects", "  - Effect ID: ${noiseSuppressor?.id}")
                        Log.i("AudioEffects", "  - Enabled: ${noiseSuppressor?.enabled}")
                    } else {
                        Log.w("AudioEffects", "⚠ NoiseSuppressor.create() returned null")
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffects", "Failed to create NoiseSuppressor: ${e.message}", e)
                }
            } else {
                Log.w("AudioEffects", "⚠ NoiseSuppressor not available on this device")
            }
            
            // 2. 回声消除 - 防止播放声音被录进去
            if (AcousticEchoCanceler.isAvailable()) {
                try {
                    echoCanceler = AcousticEchoCanceler.create(audioSessionId)
                    if (echoCanceler != null) {
                        echoCanceler?.enabled = true
                        Log.i("AudioEffects", "✓ AcousticEchoCanceler initialized successfully")
                        Log.i("AudioEffects", "  - Effect ID: ${echoCanceler?.id}")
                        Log.i("AudioEffects", "  - Enabled: ${echoCanceler?.enabled}")
                    } else {
                        Log.w("AudioEffects", "⚠ AcousticEchoCanceler.create() returned null")
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffects", "Failed to create AcousticEchoCanceler: ${e.message}", e)
                }
            } else {
                Log.w("AudioEffects", "⚠ AcousticEchoCanceler not available on this device")
            }
            
            // 3. 自动增益控制 - 保持音量稳定
            if (AutomaticGainControl.isAvailable()) {
                try {
                    gainControl = AutomaticGainControl.create(audioSessionId)
                    if (gainControl != null) {
                        gainControl?.enabled = true
                        Log.i("AudioEffects", "✓ AutomaticGainControl initialized successfully")
                        Log.i("AudioEffects", "  - Effect ID: ${gainControl?.id}")
                        Log.i("AudioEffects", "  - Enabled: ${gainControl?.enabled}")
                    } else {
                        Log.w("AudioEffects", "⚠ AutomaticGainControl.create() returned null")
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffects", "Failed to create AutomaticGainControl: ${e.message}", e)
                }
            } else {
                Log.w("AudioEffects", "⚠ AutomaticGainControl not available on this device")
            }
            
            logStatus()
            
        } catch (e: Exception) {
            Log.e("AudioEffects", "Failed to initialize audio effects: ${e.message}", e)
        }
    }
    
    /**
     * 启用/禁用噪声抑制
     */
    fun setNoiseSuppressorEnabled(enabled: Boolean) {
        noiseSuppressor?.enabled = enabled
        Log.i("AudioEffects", "NoiseSuppressor ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 启用/禁用回声消除
     */
    fun setEchoCancelerEnabled(enabled: Boolean) {
        echoCanceler?.enabled = enabled
        Log.i("AudioEffects", "EchoCanceler ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 启用/禁用自动增益控制
     */
    fun setGainControlEnabled(enabled: Boolean) {
        gainControl?.enabled = enabled
        Log.i("AudioEffects", "GainControl ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * 检查噪声抑制是否可用且已启用
     */
    fun isNoiseSuppressorActive(): Boolean {
        return noiseSuppressor?.enabled == true
    }
    
    /**
     * 获取当前状态
     */
    fun getStatus(): String {
        return buildString {
            appendLine("Audio Effects Status:")
            appendLine("  NoiseSuppressor: ${if (noiseSuppressor?.enabled == true) "✓ Active" else "✗ Inactive"}")
            appendLine("  EchoCanceler: ${if (echoCanceler?.enabled == true) "✓ Active" else "✗ Inactive"}")
            appendLine("  GainControl: ${if (gainControl?.enabled == true) "✓ Active" else "✗ Inactive"}")
        }
    }
    
    /**
     * 记录当前状态
     */
    private fun logStatus() {
        Log.i("AudioEffects", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i("AudioEffects", "Audio Effects Initialized")
        Log.i("AudioEffects", "  Session ID: $audioSessionId")
        Log.i("AudioEffects", "  NoiseSuppressor: ${if (noiseSuppressor?.enabled == true) "✓ Active" else "✗ Inactive"}")
        Log.i("AudioEffects", "  EchoCanceler: ${if (echoCanceler?.enabled == true) "✓ Active" else "✗ Inactive"}")
        Log.i("AudioEffects", "  GainControl: ${if (gainControl?.enabled == true) "✓ Active" else "✗ Inactive"}")
        Log.i("AudioEffects", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
    
    /**
     * 释放所有音频效果
     */
    fun release() {
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            
            echoCanceler?.release()
            echoCanceler = null
            
            gainControl?.release()
            gainControl = null
            
            Log.i("AudioEffects", "All audio effects released")
        } catch (e: Exception) {
            Log.e("AudioEffects", "Error releasing audio effects: ${e.message}", e)
        }
    }
}
