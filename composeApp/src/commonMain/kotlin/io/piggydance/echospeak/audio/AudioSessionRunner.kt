package io.piggydance.echospeak.audio

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serializes complete session lifetimes, including asynchronous native cleanup. */
internal class AudioSessionRunner<Key>(private val create: (Key) -> Session) {
    interface Session {
        suspend fun start()
        suspend fun close()
    }

    private val mutex = Mutex()

    suspend fun run(key: Key): Nothing {
        mutex.withLock {
            val session = create(key)
            try {
                session.start()
                awaitCancellation()
            } finally {
                // Activity/permission disposal and rapid changes must finish closing the
                // old native session before a waiting replacement can acquire the lock.
                withContext(NonCancellable) { session.close() }
            }
        }
    }
}
