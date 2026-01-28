package io.piggydance.echospeak

import android.Manifest
import androidx.annotation.RequiresPermission
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import io.piggydance.basicdeps.Log

class VoiceEchoController {
    private val audioRecorder = AudioRecorderProcessor(::onSpeechRecorded)
    private val audioPlayer = AudioPlayer()
    private var isProcessing = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        testVad()
        if (!isProcessing) {
            audioRecorder.startRecording()
            isProcessing = true
        }
    }

    fun stopListening() {
        if (isProcessing) {
            audioRecorder.stopRecording()
            isProcessing = false
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun onSpeechRecorded(audioData: ByteArray) {
        // 停止录制，准备播放
        stopListening()

        // 播放录制的音频
        audioPlayer.playAudio(audioData)

        startListening()
    }

    fun release() {
        stopListening()
        audioPlayer.stopPlayback()
    }

    fun testVad() {
        VadWebRTC(
            sampleRate = SampleRate.SAMPLE_RATE_16K,
            frameSize = FrameSize.FRAME_SIZE_320,
            mode = Mode.VERY_AGGRESSIVE,
            silenceDurationMs = 300,
            speechDurationMs = 50
        ).use { vad ->
            val isSpeech = vad.isSpeech(byteArrayOf(100))
            Log.i("testvad", "isSpeech: $isSpeech")
        }
    }
}