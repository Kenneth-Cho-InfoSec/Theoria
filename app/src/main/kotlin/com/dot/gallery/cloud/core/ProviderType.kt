/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.BuildConfig
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class ProviderType(val displayName: String, val isRemote: Boolean) {
    IMMICH("Immich", isRemote = true),
    OWNCLOUD("ownCloud", isRemote = true),
    NEXTCLOUD("Nextcloud", isRemote = true),
    WEBDAV("WebDAV", isRemote = true),
    SMB("SMB", isRemote = true),
    NFS("NFS", isRemote = true),
    LOCAL_PEOPLE("On-Device Faces", isRemote = false),
    LOCAL_OCR("On-Device OCR", isRemote = false),
    LOCAL_CLIP("On-Device Search", isRemote = false);

    val isIncludedInBuild: Boolean
        get() = when (this) {
            IMMICH -> BuildConfig.IMMICH_ENABLED
            OWNCLOUD -> BuildConfig.OWNCLOUD_ENABLED
            NEXTCLOUD -> BuildConfig.NEXTCLOUD_ENABLED
            WEBDAV -> BuildConfig.WEBDAV_ENABLED
            SMB -> BuildConfig.SMB_ENABLED
            NFS -> BuildConfig.NFS_ENABLED
            else -> true
        }

    companion object {
        fun remoteTypes(): List<ProviderType> = entries.filter { it.isRemote }
        fun availableRemoteTypes(): List<ProviderType> = entries.filter { it.isRemote && it.isIncludedInBuild }
        fun hasAnyRemoteProvider(): Boolean = entries.any { it.isRemote && it.isIncludedInBuild }

        /** Decode persisted/URI names consistently across locales and legacy whitespace. */
        fun parse(value: String): ProviderType = entries
            .firstOrNull { it.name == value.trim().uppercase(Locale.ROOT) }
            ?: error("Unknown cloud provider type: '${value.take(MAX_DIAGNOSTIC_VALUE_LENGTH)}'")

        private const val MAX_DIAGNOSTIC_VALUE_LENGTH = 64
    }
}
