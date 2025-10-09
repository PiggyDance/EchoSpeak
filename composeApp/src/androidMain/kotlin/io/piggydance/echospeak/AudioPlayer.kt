package io.piggydance.echospeak

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    fun playAudio(data: ByteArray, onComplete: () -> Unit = {}) {
        if (isPlaying) return

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
                    onComplete()
                    releaseResources()
                }

                override fun onPeriodicNotification(track: AudioTrack) {}
            })
            setNotificationMarkerPosition(data.size / 2)
        }

        isPlaying = true
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