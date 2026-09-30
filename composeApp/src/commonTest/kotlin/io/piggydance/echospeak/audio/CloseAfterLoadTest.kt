package io.piggydance.echospeak.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloseAfterLoadTest {
    @Test
    fun closeBeforeAsyncLoadEventuallyFreesExactlyOnce() {
        val lifecycle = CloseAfterLoad()
        var frees = 0
        lifecycle.close()
        lifecycle.close()
        assertEquals(0, frees)
        lifecycle.onLoaded { frees++ }
        lifecycle.onLoaded { frees++ }
        lifecycle.close()
        assertEquals(1, frees)
    }

    @Test
    fun loadedNativeRemainsAliveUntilOwnerCloses() {
        val lifecycle = CloseAfterLoad()
        var frees = 0
        lifecycle.onLoaded { frees++ }
        assertEquals(0, frees)
        lifecycle.close()
        lifecycle.close()
        lifecycle.onLoaded { frees++ }
        assertEquals(1, frees)
    }

    @Test
    fun failedNativeFreeCannotBeRetriedAgainstTheSamePointer() {
        val lifecycle = CloseAfterLoad()
        var attempts = 0
        lifecycle.onLoaded { attempts++; error("native free failed") }
        assertFailsWith<IllegalStateException> { lifecycle.close() }
        lifecycle.close()
        lifecycle.onLoaded { attempts++ }
        assertEquals(1, attempts)
    }
}
