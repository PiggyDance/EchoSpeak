package io.piggydance.echospeak.audio

/** Caller serializes these events; close may precede an uncancelable model load. */
internal class CloseAfterLoad {
    private var closed = false
    private var released = false
    private var release: (() -> Unit)? = null

    fun onLoaded(release: () -> Unit) {
        if (released) return
        this.release = release
        releaseIfClosed()
    }

    fun close() {
        closed = true
        releaseIfClosed()
    }

    private fun releaseIfClosed() {
        val action = release ?: return
        if (!closed || released) return
        released = true
        release = null
        action()
    }
}
