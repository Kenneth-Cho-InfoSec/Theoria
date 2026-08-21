/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.data

import androidx.room.TypeConverter
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import java.util.Locale

object CloudConverters {
    @TypeConverter
    fun fromProviderType(value: ProviderType): String = value.name

    @TypeConverter
    fun toProviderType(value: String): ProviderType = runCatching { ProviderType.parse(value) }
        .getOrElse { error("Invalid cloud provider type in database: ${it.message}") }

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.entries
        .firstOrNull { it.name == value.trim().uppercase(Locale.ROOT) }
        ?: error("Unknown cloud sync state in database: '${value.take(MAX_DIAGNOSTIC_VALUE_LENGTH)}'")

    private const val MAX_DIAGNOSTIC_VALUE_LENGTH = 64
}
