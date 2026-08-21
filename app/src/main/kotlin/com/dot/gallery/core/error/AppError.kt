/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.core.error

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

/** A stable error vocabulary shared by data, domain, and presentation layers. */
sealed interface AppError {
    val operation: String
    val retryable: Boolean
    val cause: Throwable?

    data class Network(
        override val operation: String,
        override val retryable: Boolean = true,
        override val cause: Throwable? = null
    ) : AppError

    data class Authentication(
        override val operation: String,
        override val cause: Throwable? = null
    ) : AppError {
        override val retryable: Boolean = false
    }

    data class Permission(
        override val operation: String,
        override val cause: Throwable? = null
    ) : AppError {
        override val retryable: Boolean = false
    }

    data class Storage(
        override val operation: String,
        override val retryable: Boolean = false,
        override val cause: Throwable? = null
    ) : AppError

    data class InvalidData(
        override val operation: String,
        override val cause: Throwable? = null
    ) : AppError {
        override val retryable: Boolean = false
    }

    data class Unexpected(
        override val operation: String,
        override val retryable: Boolean = false,
        override val cause: Throwable? = null
    ) : AppError
}

fun Throwable.toAppError(operation: String): AppError {
    if (this is CancellationException) throw this
    return when (this) {
        is SecurityException -> AppError.Permission(operation, this)
        is SocketTimeoutException, is ConnectException, is IOException ->
            AppError.Network(operation, cause = this)
        is IllegalArgumentException, is IllegalStateException ->
            AppError.InvalidData(operation, this)
        else -> AppError.Unexpected(operation, cause = this)
    }
}

/** User-safe text. Technical exception messages must not be shown directly. */
fun AppError.userMessage(): String = when (this) {
    is AppError.Network -> "Could not connect. Check your network and try again."
    is AppError.Authentication -> "Authentication failed. Check your account details."
    is AppError.Permission -> "The required permission was denied."
    is AppError.Storage -> "Storage could not be accessed. Check available space and permissions."
    is AppError.InvalidData -> "The received data was invalid. Try again."
    is AppError.Unexpected -> "Something went wrong. Try again."
}
