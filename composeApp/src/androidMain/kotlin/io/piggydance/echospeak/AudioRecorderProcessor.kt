package io.piggydance.echospeak

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

class AudioRecorderProcessor(
    private val onSpeechDetected: suspend (ByteArray) -> Unit,
    private val silenceDuration: Long = 1000L // 1秒静默视为语句结束
) {
    private var isRecording = false
    private var recorder: AudioRecord? = null
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private val audioBuffer = ByteArrayOutputStream()
    private var lastSoundTime = 0L

    companion object {
        const val SAMPLE_RATE = 44100
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (isRecording) return

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        recorder?.startRecording()
        isRecording = true

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val bytesRead = recorder?.read(buffer, 0, bufferSize) ?: 0

                if (bytesRead > 0) {
                    val isSilent = isSilence(buffer, bytesRead)

                    if (!isSilent) {
                        audioBuffer.write(buffer, 0, bytesRead)
                        lastSoundTime = System.currentTimeMillis()
                    } else if (lastSoundTime > 0 &&
                        System.currentTimeMillis() - lastSoundTime > silenceDuration) {
                        // 检测到静默时间超过阈值，处理录制的音频
                        val recordedData = audioBuffer.toByteArray()
                        onSpeechDetected(recordedData)
                        audioBuffer.reset()
                        lastSoundTime = 0
                    }
                }
            }
        }
    }

    fun stopRecording() {
        isRecording = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        audioBuffer.reset()
    }

    private fun isSilence(buffer: ByteArray, length: Int): Boolean {
        // 简单的静音检测逻辑
        var sum = 0.0
        for (i in 0 until length step 2) {
            val sample = (buffer[i + 1].toInt() shl 8) or buffer[i].toInt()
            sum += sample * sample
        }
        val rms = sqrt(sum / (length / 2))
        return rms < 500 // 静音阈值，可根据需要调整
    }
}