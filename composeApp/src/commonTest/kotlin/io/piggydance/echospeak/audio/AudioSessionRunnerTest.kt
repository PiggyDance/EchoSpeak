package io.piggydance.echospeak.audio

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AudioSessionRunnerTest {
    @Test
    fun replacementWaitsForNativeCleanup() = runBlocking {
        withTimeout(5_000) {
            val events = mutableListOf<String>()
            val closeOld = CompletableDeferred<Unit>()
            val secondStarted = CompletableDeferred<Unit>()
            val runner = AudioSessionRunner<String> { key ->
                object : AudioSessionRunner.Session {
                    override suspend fun start() {
                        events += "$key:start"
                        if (key == "second") secondStarted.complete(Unit)
                    }
                    override suspend fun close() {
                        events += "$key:closing"
                        if (key == "first") closeOld.await()
                        events += "$key:closed"
                    }
                }
            }
            val old = launch(start = CoroutineStart.UNDISPATCHED) { runner.run("first") }
            old.cancel()
            val replacement = launch(start = CoroutineStart.UNDISPATCHED) { runner.run("second") }
            yield()
            assertFalse(secondStarted.isCompleted)
            closeOld.complete(Unit)
            old.join()
            secondStarted.await()
            replacement.cancelAndJoin()
            assertEquals(
                listOf("first:start", "first:closing", "first:closed", "second:start", "second:closing", "second:closed"),
                events,
            )
        }
    }

    @Test
    fun rapidCanceledSelectionsNeverOpenNativeInstances() = runBlocking {
        withTimeout(5_000) {
            val opens = mutableListOf<String>()
            val closes = mutableListOf<String>()
            val allowClose = CompletableDeferred<Unit>()
            val runner = AudioSessionRunner<String> { key ->
                opens += key
                object : AudioSessionRunner.Session {
                    override suspend fun start() = Unit
                    override suspend fun close() {
                        allowClose.await()
                        closes += key
                    }
                }
            }
            val active = launch(start = CoroutineStart.UNDISPATCHED) { runner.run("active") }
            active.cancel()
            val skipped = launch(start = CoroutineStart.UNDISPATCHED) { runner.run("skipped") }
            skipped.cancelAndJoin()
            allowClose.complete(Unit)
            active.join()
            assertEquals(listOf("active"), opens)
            assertEquals(opens, closes)
        }
    }

    @Test
    fun failingOrCanceledStartupClosesExactlyOnceAndUnlocks() = runBlocking {
        withTimeout(5_000) {
            var closes = 0
            val startup = CompletableDeferred<Unit>()
            val runner = AudioSessionRunner<Boolean> { fail ->
                object : AudioSessionRunner.Session {
                    override suspend fun start() {
                        if (fail) error("native start failed")
                        startup.await()
                    }
                    override suspend fun close() { closes++ }
                }
            }
            assertFailsWith<IllegalStateException> { runner.run(true) }
            assertEquals(1, closes)
            val next = launch(start = CoroutineStart.UNDISPATCHED) { runner.run(false) }
            next.cancelAndJoin()
            assertEquals(2, closes)
        }
    }
}
