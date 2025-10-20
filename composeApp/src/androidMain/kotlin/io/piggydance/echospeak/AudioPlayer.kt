package io.piggydance.echospeak

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

class AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun playAudio(data: ByteArray) {
        return suspendCancellableCoroutine { continuation ->
            if (isPlaying) {
                continuation.resume(Unit) {}
                return@suspendCancellableCoroutine
            }
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                AudioRecorderProcessor.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                data.size,
                AudioTrack.MODE_STREAM
            )

            audioTrack?.apply {
                play()
                write(data, 0, data.size)
                setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack) {
                        // 播放完成，恢复协程
                        continuation.resume(Unit) {}
                        releaseResources()
                    }

                    override fun onPeriodicNotification(track: AudioTrack) {}
                })
                setNotificationMarkerPosition(data.size / 2)
            }

            isPlaying = true

            // 处理协程取消事件
            continuation.invokeOnCancellation {
                stopPlayback()
            }
        }
    }

    fun stopPlayback() {
        audioTrack?.stop()
        releaseResources()
    }

    private fun releaseResources() {
        audioTrack?.release()
        audioTrack = null
        isPlaying = false
    }
}