package io.piggydance.echospeak

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.piggydance.basicdeps.Log
import io.piggydance.echospeak.audio.AudioPlayer
import io.piggydance.echospeak.audio.SpeechDetector
import io.piggydance.echospeak.audio.SpeechSegment
import io.piggydance.echospeak.audio.VadType

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
 * @param vadType           VAD 引擎类型，默认 Silero
 */
class VoiceEchoController(
    private val silenceDurationMs: Long = 1000L,
    private val vadType: VadType = VadType.SILERO,
) {
    private val speechDetector = SpeechDetector(
        onSpeechDetected = ::onSpeechDetected,
        silenceDurationMs = silenceDurationMs,
        vadType = vadType,
        minSpeechFrames = 3,
        preBufferFrames = 30,
    )
    
    private val audioPlayer = AudioPlayer()
    private var isActive = false

    /**
     * 开始语音回声功能
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(context: Context?) {
        if (isActive) {
            Log.w("VoiceEcho", "Already started")
            return
        }
        
        Log.i("VoiceEcho", "Starting voice echo...")
        isActive = true
        context?.let {
            speechDetector.start(it)
        }
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
     * @param segment 录制的语音片段（PCM + VAD 帧标签）
     */
    private suspend fun onSpeechDetected(segment: SpeechSegment) {
        Log.i("VoiceEcho", "Speech detected, size: ${segment.pcm.size} bytes, vadFrames: ${segment.vadFrameLabels.size}")

        // 播放录制的音频（回声效果），携带 VAD 标签供播放侧精确降噪
        audioPlayer.play(segment)

        Log.i("VoiceEcho", "Playback completed")
    }

    /**
     * 释放所有资源
     */
    fun release() {
        Log.i("VoiceEcho", "Releasing resources")
        stop()
        speechDetector.release()
        audioPlayer.release()  // 释放 RNNoise 原生内存
    }
}
