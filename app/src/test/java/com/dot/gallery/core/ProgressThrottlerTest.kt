/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.core.util.ProgressThrottler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressThrottlerTest {
    @Test
    fun retriesTheSameValueAfterCallbackFailure() = runBlocking {
        val throttler = ProgressThrottler()
        var attempts = 0

        try {
            throttler.emit(40) {
                attempts++
                error("temporary progress sink failure")
            }
        } catch (_: IllegalStateException) {
            // Expected: a failed callback must not mark the value as delivered.
        }

        throttler.emit(40) { attempts++ }

        assertEquals(2, attempts)
    }

    @Test
    fun clampsValuesAndSuppressesSuccessfulDuplicates() = runBlocking {
        val throttler = ProgressThrottler()
        val values = mutableListOf<Int>()

        throttler.emit(-10) { values += it }
        throttler.emit(0) { values += it }
        throttler.emit(140) { values += it }
        throttler.emit(100) { values += it }

        assertEquals(listOf(0, 100), values)
    }
}
