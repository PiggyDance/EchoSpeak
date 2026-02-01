package io.piggydance.echospeak.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import io.piggydance.basicdeps.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 音频播放器
 * 
 * 负责播放 PCM 音频数据
 * 使用 16kHz 采样率，单声道，16-bit PCM 格式
 */
class AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var visualizerJob: Job? = null
    private var currentPlaybackData: ByteArray? = null
    
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    /**
     * 播放音频数据
     *
     * @param data PCM 音频数据（16-bit, 16kHz, Mono）
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun play(data: ByteArray) {
        return suspendCancellableCoroutine { continuation ->
            if (isPlaying) {
                Log.w("AudioPlayer", "Already playing, skipping")
                continuation.resume(Unit) {}
                return@suspendCancellableCoroutine
            }
            
            if (data.isEmpty()) {
                Log.w("AudioPlayer", "Empty audio data")
                continuation.resume(Unit) {}
                return@suspendCancellableCoroutine
            }
            
            val durationMs = data.size / (SAMPLE_RATE * 2 / 1000)
            Log.i("AudioPlayer", "Starting playback (size: ${data.size} bytes, duration: ${durationMs}ms)")
            
            try {
                // 计算合适的缓冲区大小
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )
                
                if (minBufferSize == AudioTrack.ERROR_BAD_VALUE || minBufferSize == AudioTrack.ERROR) {
                    Log.e("AudioPlayer", "Invalid buffer size")
                    continuation.resume(Unit) {}
                    return@suspendCancellableCoroutine
                }
                
                val bufferSize = maxOf(minBufferSize, data.size)
                
                // 创建 AudioTrack
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )
                
                audioTrack?.apply {
                    // MODE_STATIC 模式下先写入数据
                    val bytesWritten = write(data, 0, data.size)
                    
                    if (bytesWritten < 0) {
                        Log.e("AudioPlayer", "Failed to write audio data: $bytesWritten")
                        continuation.resume(Unit) {}
                        release()
                        return@suspendCancellableCoroutine
                    }
                    
                    // 计算总帧数（每帧2字节，16-bit PCM）
                    val totalFrames = data.size / 2
                    
                    // 设置播放完成监听
                    setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onMarkerReached(track: AudioTrack) {
                            Log.i("AudioPlayer", "✓ Playback completed successfully")
                            continuation.resume(Unit) {}
                            releaseResources()
                        }
                        
                        override fun onPeriodicNotification(track: AudioTrack) {
                            // 不需要周期性通知
                        }
                    })
                    
                    // 设置播放完成标记
                    setNotificationMarkerPosition(totalFrames)
                    
                    // 保存播放数据用于可视化
                    currentPlaybackData = data
                    
                    // 开始播放
                    play()
                    isPlaying = true
                    
                    // 启动可视化更新
                    startVisualizerUpdates(data, durationMs.toLong())
                    
                    Log.i("AudioPlayer", "Playback started ($totalFrames frames)")
                }
                
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error during playback: ${e.message}")
                continuation.resume(Unit) {}
                releaseResources()
            }
            
            // 处理协程取消
            continuation.invokeOnCancellation {
                stop()
            }
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        if (!isPlaying) return
        
        try {
            audioTrack?.stop()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error stopping playback: ${e.message}")
        }
        
        releaseResources()
    }
    
    /**
     * 启动可视化数据更新
     */
    private fun startVisualizerUpdates(data: ByteArray, durationMs: Long) {
        visualizerJob?.cancel()
        visualizerJob = CoroutineScope(Dispatchers.Default).launch {
            val frameSize = 1024  // 每次取1024字节用于可视化
            val updateInterval = 50L  // 50ms更新一次
            val totalUpdates = (durationMs / updateInterval).toInt()
            
            for (i in 0 until totalUpdates) {
                if (!isPlaying) break
                
                // 计算当前播放位置
                val progress = i.toFloat() / totalUpdates
                val startIndex = (data.size * progress).toInt().coerceIn(0, data.size - frameSize)
                val endIndex = (startIndex + frameSize).coerceAtMost(data.size)
                
                // 提取当前帧数据
                val frameData = data.copyOfRange(startIndex, endIndex)
                
                // 更新可视化
                AudioVisualizerManager.updatePlaybackData(frameData)
                
                delay(updateInterval)
            }
            
            // 播放结束，重置可视化
            if (!isPlaying) {
                AudioVisualizerManager.reset()
            }
        }
    }
    
    /**
     * 释放资源
     */
    private fun releaseResources() {
        visualizerJob?.cancel()
        visualizerJob = null
        currentPlaybackData = null
        
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error releasing AudioTrack: ${e.message}")
        }
        
        audioTrack = null
        isPlaying = false
        
        // 重置可视化
        AudioVisualizerManager.reset()
    }
}
