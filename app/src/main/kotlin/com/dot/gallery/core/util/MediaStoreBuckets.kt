/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */
package com.dot.gallery.core.util

enum class MediaStoreBuckets(val id: Long) {
    /**
     * Favorites album.
     */
    MEDIA_STORE_BUCKET_FAVORITES(-0x0001DEAD),

    /**
     * Trash album.
     */
    MEDIA_STORE_BUCKET_TRASH(-0x0002DEAD),

    /**
     * Timeline, contains all medias.
     */
    MEDIA_STORE_BUCKET_TIMELINE(-0x0003DEAD),

    /**
     * Reserved bucket ID for placeholders, throw an exception if this value is used.
     */
    MEDIA_STORE_BUCKET_PLACEHOLDER(-0x0004DEAD),

    /**
     * Timeline, contains only photos.
     */
    MEDIA_STORE_BUCKET_PHOTOS(-0x0005DEAD),

    /**
     * Timeline, contains only videos.
     */
    MEDIA_STORE_BUCKET_VIDEOS(-0x0006DEAD);
}
