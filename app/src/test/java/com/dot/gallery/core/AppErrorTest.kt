/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.core.error.AppError
import com.dot.gallery.core.error.toAppError
import com.dot.gallery.core.error.userMessage
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorTest {
    @Test
    fun ioFailuresAreRetryableNetworkErrors() {
        val error = IOException("private server path").toAppError("sync")

        assertTrue(error is AppError.Network)
        assertTrue(error.retryable)
        assertEquals("Could not connect. Check your network and try again.", error.userMessage())
    }

    @Test
    fun securityFailuresBecomePermissionErrors() {
        val error = SecurityException("sensitive detail").toAppError("read media")

        assertTrue(error is AppError.Permission)
        assertEquals("The required permission was denied.", error.userMessage())
    }

    @Test
    fun cancellationIsNeverConvertedToAUserError() {
        try {
            CancellationException("cancelled").toAppError("load")
            throw AssertionError("CancellationException must be rethrown")
        } catch (_: CancellationException) {
            // Expected: coroutine cancellation must never become a user-visible failure.
        }
    }
}
