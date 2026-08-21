/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.core.error

import android.util.Log

/** Centralized, sanitized reporting for handled failures. */
object ErrorReporter {
    private const val TAG = "TheoriaError"

    fun report(error: AppError) {
        // Do not include exception messages: providers may put URLs, credentials, or paths in them.
        Log.w(TAG, "${error.operation} failed: ${error::class.simpleName}")
    }
}
