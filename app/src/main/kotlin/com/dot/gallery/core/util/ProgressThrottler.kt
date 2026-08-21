package com.dot.gallery.core.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe (coroutine-safe) progress throttler to suppress duplicate integer percent updates.
 * Provides suspend emit to allow calling code to perform suspend work inside the block safely.
 */
class ProgressThrottler {
    private var lastSuccessful: Int? = null
    private val mutex = Mutex()

    /**
     * Emit a new progress percent only if it changed from previous value.
     * @param p progress percent (0..100 typically) – coerced to Int.
     * @param block suspend block executed only when progress changes.
     */
    internal suspend fun emit(p: Int, block: suspend (Int) -> Unit) {
        val normalized = p.coerceIn(0, 100)
        mutex.withLock {
            if (normalized == lastSuccessful) {
                return
            }
            block(normalized)
            lastSuccessful = normalized
        }
    }

    internal suspend fun reset() = mutex.withLock { lastSuccessful = null }
}
