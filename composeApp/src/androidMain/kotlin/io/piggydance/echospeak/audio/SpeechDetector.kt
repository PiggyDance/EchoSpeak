package io.piggydance.echospeak.audio

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.piggydance.basicdeps.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * 语音检测器
 *
 * 整合音频录制和 VAD 人声检测，实现智能语音片段提取
 *
 * 工作流程：
 * 1. 持续录制音频并进行 VAD 检测
 * 2. 使用预缓冲区保存最近的音频帧
 * 3. 检测到连续人声时，开始正式录制（包含预缓冲）
 * 4. 连续静默超过阈值时，结束录制并回调
 *
 * @param onSpeechDetected  检测到完整语音片段的回调（携带 PCM 和 VAD 帧标签）
 * @param silenceDurationMs 静默持续时间阈值（毫秒），超过此时间视为语句结束
 * @param vadType           VAD 引擎类型，默认 Silero
 * @param minSpeechFrames   开始录制前需要连续检测到的人声帧数
 * @param preBufferFrames   预缓冲帧数，用于保存开始检测前的音频
 */
class SpeechDetector(
    private val onSpeechDetected: suspend (SpeechSegment) -> Unit,
    private val silenceDurationMs: Long = 1000L,
    private val vadType: VadType = VadType.SILERO,
    private val minSpeechFrames: Int = 3,
    private val preBufferFrames: Int = 10,
) {
    private val audioRecorder = AudioRecorder()
    private val vadDetector = VadDetector(type = vadType)

    private val audioBuffer = ByteArrayOutputStream()
    private val preBuffer = ArrayDeque<ByteArray>(preBufferFrames)
    // 与 preBuffer 一一对应的 VAD 标签（预缓冲阶段）
    private val preBufferLabels = ArrayDeque<Boolean>(preBufferFrames)
    // 录音阶段每帧的 VAD 标签
    private val vadLabelBuffer = mutableListOf<Boolean>()

    private var detectionJob: Job? = null
    private var hasSpeech = false
    private var consecutiveSpeechFrames = 0
    private var lastSpeechTime = 0L

    // 统计信息
    private var totalFramesProcessed = 0
    private var speechFramesCount = 0
    private var silenceFramesCount = 0
    private var recordingStartTime = 0L
    
    /**
     * 开始语音检测
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(context: Context) {
        if (detectionJob?.isActive == true) {
            Log.w("SpeechDetector", "Already running, skipping start")
            return
        }
        
        Log.i("SpeechDetector", "Starting speech detection...")
        
        // 初始化 VAD（如果还未初始化）
        if (!vadDetector.isInitialized) {
            Log.d("SpeechDetector", "Initializing VAD detector (type=${vadType.displayName})")
            vadDetector.initialize(context)
        }
        
        if (!audioRecorder.start(vadDetector.frameSizeBytes)) {
            Log.e("SpeechDetector", "Failed to start audio recorder")
            return
        }
        
        // 重置状态
        resetState()
        
        // 设置为监听模式
        AudioVisualizerManager.setListening()
        
        // 启动检测协程
        detectionJob = CoroutineScope(Dispatchers.IO).launch {
            processAudioStream()
        }
        
        Log.i("SpeechDetector", "Speech detection started (vad=${vadType.displayName}, frame=${vadDetector.frameSizeBytes}B, minSpeechFrames=$minSpeechFrames)")
    }
    
    /**
     * 停止语音检测（不释放VAD资源，可以重新启动）
     */
    fun stop() {
        detectionJob?.cancel()
        detectionJob = null
        
        audioRecorder.stop()
        
        // 重置可视化数据
        AudioVisualizerManager.reset()
        
        resetState()
        
        Log.i("SpeechDetector", "Stopped speech detection")
    }
    
    /**
     * 处理音频流
     */
    private suspend fun processAudioStream() {
        val frameBytes = vadDetector.frameSizeBytes
        val buffer = ByteArray(frameBytes)
        Log.d("SpeechDetector", "Audio stream processing started (frameBytes=$frameBytes)")
        
        var frameCount = 0
        while (audioRecorder.isRecording()) {
            val bytesRead = audioRecorder.read(buffer, frameBytes)
            
            if (bytesRead == frameBytes) {
                frameCount++
                processAudioFrame(buffer)
                
                // 每1000帧输出一次统计信息
                if (frameCount % 1000 == 0) {
                    val frameDurationMs = vadDetector.frameSizeSamples * 1000 / VadDetector.SAMPLE_RATE
                    val elapsedSeconds = frameCount * frameDurationMs / 1000
                    Log.d("SpeechDetector", "Stream stats: ${frameCount} frames (${elapsedSeconds}s), speech: $speechFramesCount, silence: $silenceFramesCount")
                }
            } else if (bytesRead > 0) {
                Log.w("SpeechDetector", "Incomplete frame read: $bytesRead bytes (expected: $frameBytes)")
            }
        }
        
        Log.d("SpeechDetector", "Audio stream processing ended (total frames: $frameCount)")
    }
    
    /**
     * 处理单个音频帧
     */
    private suspend fun processAudioFrame(frame: ByteArray) {
        totalFramesProcessed++

        // 复制帧数据（因为 buffer 会被重用）
        val frameCopy = frame.copyOf()

        // 获取音量用于日志
        val volume = vadDetector.getVolume(frameCopy)

        // VAD 检测：完全交由 Silero VAD 模型判断，不加音量阈值拦截
        // 降噪处理已移至播放侧（PlaybackProcessor），收音侧保持原始信号
        val isSpeech = vadDetector.isSpeech(frameCopy)
        
        // 根据状态更新可视化数据
        if (hasSpeech) {
            // 录音中：已经开始正式录制
            AudioVisualizerManager.updateRecordingData(frameCopy)
        } else {
            // 监听中：传入实时音频帧，让环境音/杂音也驱动波形
            AudioVisualizerManager.updateListeningData(frameCopy)
        }
        
        // 更新统计
        if (isSpeech) {
            speechFramesCount++
        } else {
            silenceFramesCount++
        }
        
        if (!hasSpeech) {
            // 未开始录制，维护预缓冲区
            handlePreBuffering(frameCopy, isSpeech, volume)
        } else {
            // 已开始录制，持续写入
            handleRecording(frameCopy, isSpeech, volume)
        }
    }
    
    /**
     * 处理预缓冲阶段
     */
    private suspend fun handlePreBuffering(frame: ByteArray, isSpeech: Boolean, volume: Double) {
        // 维护预缓冲区（循环队列），同步维护对应的 VAD 标签
        if (preBuffer.size >= preBufferFrames) {
            preBuffer.removeFirst()
            preBufferLabels.removeFirst()
        }
        preBuffer.addLast(frame)
        preBufferLabels.addLast(isSpeech)
        
        // 检测连续人声
        if (isSpeech) {
            consecutiveSpeechFrames++
            Log.d("SpeechDetector", "[PRE-BUFFER] Speech detected → consecutive: $consecutiveSpeechFrames/$minSpeechFrames, volume: ${volume.toInt()}")
            
            if (consecutiveSpeechFrames >= minSpeechFrames) {
                // 达到阈值，开始录制
                startRecording()
            }
        } else {
            // 静音，重置计数
            if (consecutiveSpeechFrames > 0) {
                Log.d("SpeechDetector", "[PRE-BUFFER] Silence detected → resetting counter (was: $consecutiveSpeechFrames, volume: ${volume.toInt()})")
                consecutiveSpeechFrames = 0
            }
        }
    }
    
    /**
     * 开始录制（将预缓冲写入）
     */
    private fun startRecording() {
        recordingStartTime = System.currentTimeMillis()
        val preBufferDurationMs = preBuffer.size * 20  // 每帧20ms
        
        Log.i("SpeechDetector", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i("SpeechDetector", "✓ SPEECH CONFIRMED! Starting recording")
        Log.i("SpeechDetector", "  • Pre-buffer: ${preBuffer.size} frames (≈${preBufferDurationMs}ms)")
        Log.i("SpeechDetector", "  • Consecutive speech frames: $consecutiveSpeechFrames")
        Log.i("SpeechDetector", "  • Total frames processed: $totalFramesProcessed")
        Log.i("SpeechDetector", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        hasSpeech = true
        lastSpeechTime = System.currentTimeMillis()

        // 将预缓冲区的所有帧（及其 VAD 标签）写入
        preBuffer.forEachIndexed { index, frame ->
            audioBuffer.write(frame, 0, frame.size)
            vadLabelBuffer.add(preBufferLabels.getOrElse(index) { false })
        }
        preBuffer.clear()
        preBufferLabels.clear()
        
        Log.d("SpeechDetector", "[RECORDING] Initial buffer size: ${audioBuffer.size()} bytes")
    }
    
    /**
     * 处理录制阶段
     */
    private suspend fun handleRecording(frame: ByteArray, isSpeech: Boolean, volume: Double) {
        // 持续写入所有音频数据，同时记录本帧 VAD 标签
        audioBuffer.write(frame, 0, frame.size)
        vadLabelBuffer.add(isSpeech)
        
        val currentRecordingDuration = System.currentTimeMillis() - recordingStartTime
        
        if (isSpeech) {
            // 更新最后一次检测到人声的时间
            lastSpeechTime = System.currentTimeMillis()
            
            // 每2秒输出一次录制进度
            if (currentRecordingDuration % 2000 < 20) {
                val bufferSizeKB = audioBuffer.size() / 1024.0
                Log.d("SpeechDetector", "[RECORDING] Progress: ${currentRecordingDuration}ms, buffer: ${"%.2f".format(bufferSizeKB)}KB, volume: ${volume.toInt()}")
            }
        } else {
            // 检查是否连续静默超过阈值
            val silenceDuration = System.currentTimeMillis() - lastSpeechTime
            
            if (silenceDuration > silenceDurationMs) {
                // 语句结束
                finishRecording(silenceDuration)
            } else {
                // 每500ms记录一次静默进度
                if (silenceDuration % 500 < 20) {
                    val progress = (silenceDuration * 100 / silenceDurationMs).toInt()
                    Log.d("SpeechDetector", "[RECORDING] Silence: ${silenceDuration}ms / ${silenceDurationMs}ms (${progress}%), volume: ${volume.toInt()}")
                }
            }
        }
    }
    
    /**
     * 结束录制并回调
     */
    private suspend fun finishRecording(silenceDuration: Long) {
        val actualRecordingDuration = System.currentTimeMillis() - recordingStartTime
        val durationMs = audioBuffer.size() / (SAMPLE_RATE * 2 / 1000)  // 计算录音时长
        val bufferSizeKB = audioBuffer.size() / 1024.0
        
        Log.i("SpeechDetector", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i("SpeechDetector", "✓ SPEECH ENDED!")
        Log.i("SpeechDetector", "  • Silence duration: ${silenceDuration}ms")
        Log.i("SpeechDetector", "  • Recording duration: ${actualRecordingDuration}ms")
        Log.i("SpeechDetector", "  • Audio duration: ${durationMs}ms")
        Log.i("SpeechDetector", "  • Buffer size: ${"%.2f".format(bufferSizeKB)}KB (${audioBuffer.size()} bytes)")
        Log.i("SpeechDetector", "  • Total frames: $totalFramesProcessed (speech: $speechFramesCount, silence: $silenceFramesCount)")
        Log.i("SpeechDetector", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val recordedData = audioBuffer.toByteArray()

        if (recordedData.isNotEmpty()) {
            Log.i("SpeechDetector", "Invoking speech detected callback (frames=${vadLabelBuffer.size}, speechFraction=${vadLabelBuffer.count { it }}/${vadLabelBuffer.size})...")
            val segment = SpeechSegment(
                pcm = recordedData,
                vadFrameLabels = vadLabelBuffer.toBooleanArray(),
                vadFrameSizeSamples = vadDetector.frameSizeSamples,
            )
            val callbackStartTime = System.currentTimeMillis()
            onSpeechDetected(segment)
            val callbackDuration = System.currentTimeMillis() - callbackStartTime
            Log.d("SpeechDetector", "Callback completed in ${callbackDuration}ms")
        } else {
            Log.w("SpeechDetector", "⚠ Recorded data is empty, skipping callback")
        }
        
        // 重置状态，准备下一次检测
        resetState()
        Log.i("SpeechDetector", "✓ State reset, ready for next detection")
    }
    
    companion object {
        private const val SAMPLE_RATE = 16000
    }
    
    /**
     * 重置检测状态
     */
    private fun resetState() {
        Log.d("SpeechDetector", "[RESET] Clearing state (buffer: ${audioBuffer.size()} bytes, preBuffer: ${preBuffer.size} frames, vadLabels: ${vadLabelBuffer.size})")

        audioBuffer.reset()
        preBuffer.clear()
        preBufferLabels.clear()
        vadLabelBuffer.clear()
        hasSpeech = false
        consecutiveSpeechFrames = 0
        lastSpeechTime = 0L
        totalFramesProcessed = 0
        speechFramesCount = 0
        silenceFramesCount = 0
        recordingStartTime = 0L

        Log.d("SpeechDetector", "[RESET] State cleared successfully")
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        stop()
    }
}
