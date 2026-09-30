package io.piggydance.echospeak.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rikorose.deepfilternet.NativeDeepFilterNet
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Uses synthetic silence only: no microphone, account, or user recordings are accessed. */
@RunWith(AndroidJUnit4::class)
class NativeAudioSmokeTest {
    @Test
    fun allThreeVadEnginesCanLoadAndRunTheirBundledModels() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (type in VadType.entries) {
            val detector = VadDetector(type)
            try {
                detector.initialize(context)
                repeat(3) { assertFalse(detector.isSpeech(ByteArray(detector.frameSizeBytes))) }
            } finally {
                detector.release()
            }
        }
    }

    @Test
    fun deepFilterNetLoadsAndProcessesOneSilentFrame() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ready = CountDownLatch(1)
        val processor = NativeDeepFilterNet(context, attenuationLimit = 40f)
        try {
            processor.onModelLoaded { ready.countDown() }
            assertTrue("DeepFilterNet model did not load", ready.await(30, TimeUnit.SECONDS))
            val frameLength = processor.frameLength.toInt()
            assertTrue("Expected a nonempty PCM16 frame", frameLength > 0 && frameLength % 2 == 0)
            val frame = ByteBuffer.allocateDirect(frameLength).order(ByteOrder.LITTLE_ENDIAN)
            processor.processFrame(frame)
        } finally {
            processor.release()
        }
    }
}
