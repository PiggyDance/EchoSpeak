package io.piggydance.echospeak.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission

/**
 * 音频录制器
 *
 * 负责从麦克风录制音频数据
 * 使用 16kHz 采样率，单声道，16-bit PCM 格式
 */
class AudioRecorder {
    private var recorder: AudioRecord? = null
    private var isRecording = false
    private var audioEffects: AudioEffectsProcessor? = null
    
    /**
     * 检查系统降噪是否可用并已启用
     */
    fun hasActiveNoiseReduction(): Boolean {
        return audioEffects?.isNoiseSuppressorActive() == true
    }
    
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val TAG = "AudioRecorder"
    }
    
    /**
     * 初始化并启动录制
     *
     * @param frameSize 每次读取的帧大小（字节）
     * @return 是否成功启动
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(frameSize: Int): Boolean {
        if (isRecording) {
            android.util.Log.w(TAG, "Already recording")
            return false
        }
        
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ).coerceAtLeast(frameSize * 4)
        
        android.util.Log.d(TAG, "Initializing AudioRecord (bufferSize: $bufferSize bytes)")

        // 使用 MIC 音频源（原始麦克风信号，不叠加系统 DSP 处理）。
        // 原因：播放侧已使用 DeepFilterNet 做神经网络降噪，
        // 若录音侧再叠加系统 NoiseSuppressor，会导致双重降噪，
        // 过度抑制语音高频泛音，使声音听起来"闷"。
        // VOICE_COMMUNICATION 会强制开启系统降噪，与 DeepFilterNet 冲突。
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )
        
        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            android.util.Log.e(TAG, "Failed to initialize AudioRecord")
            recorder?.release()
            recorder = null
            return false
        }
        
        recorder?.startRecording()
        isRecording = true
        
        // 初始化音频效果（降噪等）
        val audioSessionId = recorder?.audioSessionId ?: 0
        if (audioSessionId != 0) {
            audioEffects = AudioEffectsProcessor(audioSessionId)
            audioEffects?.initialize()
        }
        
        android.util.Log.i(TAG, "Recording started successfully (audioSessionId: $audioSessionId)")
        return true
    }
    
    /**
     * 读取音频数据
     * 
     * @param buffer 用于存储音频数据的缓冲区
     * @param size 要读取的字节数
     * @return 实际读取的字节数，-1 表示错误
     */
    fun read(buffer: ByteArray, size: Int): Int {
        if (!isRecording || recorder == null) return -1
        return recorder?.read(buffer, 0, size) ?: -1
    }
    
    /**
     * 停止录制
     */
    fun stop() {
        if (!isRecording) return
        
        android.util.Log.i(TAG, "Stopping recording")
        isRecording = false
        
        // 释放音频效果
        audioEffects?.release()
        audioEffects = null
        
        recorder?.stop()
        recorder?.release()
        recorder = null
        android.util.Log.d(TAG, "Recording stopped and resources released")
    }
    
    /**
     * 检查是否正在录制
     */
    fun isRecording(): Boolean = isRecording
}
