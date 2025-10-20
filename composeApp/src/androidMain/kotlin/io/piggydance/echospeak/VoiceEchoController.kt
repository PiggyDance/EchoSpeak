package io.piggydance.echospeak

import android.Manifest
import androidx.annotation.RequiresPermission

class VoiceEchoController {
    private val audioRecorder = AudioRecorderProcessor(::onSpeechRecorded)
    private val audioPlayer = AudioPlayer()
    private var isProcessing = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening() {
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
}