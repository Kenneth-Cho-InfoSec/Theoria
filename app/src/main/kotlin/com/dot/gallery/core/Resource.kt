/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.core.error.AppError

sealed class Resource<T>(
    var data: T? = null,
    val message: String? = null,
    val error: AppError? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(
        message: String,
        data: T? = null,
        error: AppError? = null
    ) : Resource<T>(data, message, error)
}
