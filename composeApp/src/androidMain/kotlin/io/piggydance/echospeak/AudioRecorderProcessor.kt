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
        // 44100（音乐）
        // 48000（现代设备标准）
        // 16000/8000（VoIP）
        const val SAMPLE_RATE = 44100
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        if (isRecording) return

        recorder = AudioRecord(
            // 如果要使用AEC,audioSource必须选择VOICE_COMMUNICATION,否则AEC不生效
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            // ENCODING_PCM_16BIT（最稳定）
            // ENCODING_PCM_FLOAT（高精度但不广泛支持）
            AudioFormat.ENCODING_PCM_16BIT,
            // Buffer 太小 → 卡顿、drop
            // Buffer 太大 → 延迟大
            bufferSize
        )

        recorder?.startRecording()
        isRecording = true

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                // AudioRecord.read方法
                // 优点：简单、稳定
                // 缺点：你的线程必须及时 read，否则会丢帧。
                //
                // 还有一种方法是setRecordPositionUpdateListener, AudioRecord 内部线程主动 “回调” 给你。
                // 优点：实时性高、线程稳定、减少阻塞问题
                // 缺点：复杂，许多厂商有 bug（不稳定）
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