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
        android.util.Log.i(TAG, "Recording started successfully")
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
