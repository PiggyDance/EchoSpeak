package io.piggydance.echospeak

import android.Manifest
import androidx.annotation.RequiresPermission
import com.konovalov.vad.webrtc.config.Mode
import io.piggydance.basicdeps.Log
import io.piggydance.echospeak.audio.AudioPlayer
import io.piggydance.echospeak.audio.SpeechDetector

/**
 * 语音回声控制器
 *
 * 负责协调语音检测和音频播放，实现语音回声效果
 *
 * 工作流程：
 * 1. 启动语音检测，持续监听麦克风
 * 2. 检测到完整语音片段后，停止检测
 * 3. 播放录制的音频（回声效果）
 * 4. 播放完成后，继续语音检测
 *
 * @param silenceDurationMs 静默持续时间阈值（毫秒），默认 1000ms
 * @param vadMode VAD 检测模式，默认 AGGRESSIVE
 * @param volumeThreshold 音量阈值，默认 500.0，可根据环境噪音调整（建议范围：300-1000）
 */
class VoiceEchoController(
    private val silenceDurationMs: Long = 1000L,
    private val vadMode: Mode = Mode.AGGRESSIVE,
    private val volumeThreshold: Double = 500.0
) {
    private val speechDetector = SpeechDetector(
        onSpeechDetected = ::onSpeechDetected,
        silenceDurationMs = silenceDurationMs,
        vadMode = vadMode,
        minSpeechFrames = 3,
        preBufferFrames = 10,
        volumeThreshold = volumeThreshold
    )
    
    private val audioPlayer = AudioPlayer()
    private var isActive = false

    /**
     * 开始语音回声功能
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (isActive) {
            Log.w("VoiceEcho", "Already started")
            return
        }
        
        Log.i("VoiceEcho", "Starting voice echo...")
        isActive = true
        speechDetector.start()
    }

    /**
     * 停止语音回声功能
     */
    fun stop() {
        if (!isActive) return
        
        Log.i("VoiceEcho", "Stopping voice echo")
        isActive = false
        speechDetector.stop()
        audioPlayer.stop()
    }

    /**
     * 当检测到完整语音时的回调
     *
     * @param audioData 录制的音频数据（PCM 16-bit, 16kHz, Mono）
     */
    private suspend fun onSpeechDetected(audioData: ByteArray) {
        Log.i("VoiceEcho", "Speech detected, size: ${audioData.size} bytes")
        
        // 播放录制的音频（回声效果）
        // 注意：SpeechDetector 在回调后会自动重置状态，继续检测下一段语音
        audioPlayer.play(audioData)
        
        Log.i("VoiceEcho", "Playback completed")
    }

    /**
     * 释放所有资源
     */
    fun release() {
        Log.i("VoiceEcho", "Releasing resources")
        stop()
        speechDetector.release()
    }
}
