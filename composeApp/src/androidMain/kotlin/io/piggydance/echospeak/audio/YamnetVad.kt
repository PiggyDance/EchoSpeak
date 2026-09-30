package io.piggydance.echospeak.audio

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Runs the existing YAMNet model without the old, 4 KB aligned Task Audio JNI library. */
internal class YamnetVad(
    context: Context,
    speechDurationMs: Int,
    silenceDurationMs: Int,
) : Closeable {
    private val window = YamnetAudioWindow()
    private val gate = YamnetSpeechGate(speechDurationMs, silenceDurationMs)
    private val input = FloatArray(YamnetAudioWindow.SAMPLE_COUNT)
    private val output = arrayOf(FloatArray(YamnetSpeechGate.CLASS_COUNT))
    private val modelBuffer: ByteBuffer
    private val interpreter: Interpreter

    init {
        val labels = context.assets.open("yamnet_labels.txt").bufferedReader().use { it.readLines() }
        require(labels.size == YamnetSpeechGate.CLASS_COUNT && labels[0] == "Speech") {
            "YAMNet label mapping does not match the bundled model"
        }
        val model = context.assets.open("yamnet.tflite").use { it.readBytes() }
        modelBuffer = ByteBuffer.allocateDirect(model.size).order(ByteOrder.nativeOrder())
            .apply { put(model); rewind() }
        interpreter = Interpreter(modelBuffer)
        try {
            require(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1)
            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)
            require(inputTensor.dataType() == DataType.FLOAT32 &&
                inputTensor.shape().contentEquals(intArrayOf(YamnetAudioWindow.SAMPLE_COUNT))) {
                "Expected a FLOAT32 YAMNet waveform with 15600 samples"
            }
            require(outputTensor.dataType() == DataType.FLOAT32 &&
                outputTensor.shape().contentEquals(intArrayOf(1, YamnetSpeechGate.CLASS_COUNT))) {
                "Expected 521 FLOAT32 YAMNet class scores"
            }
        } catch (error: Throwable) {
            interpreter.close()
            throw error
        }
    }

    fun isSpeech(audioFrame: ByteArray): Boolean {
        require(audioFrame.size == 243 * 2) { "YAMNet expects one 243-sample PCM16 frame" }
        window.appendPcm16(audioFrame)
        window.copyTo(input)
        interpreter.run(input, output)
        return gate.isSpeech(output[0])
    }

    override fun close() = interpreter.close()
}
