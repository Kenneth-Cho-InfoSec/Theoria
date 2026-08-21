/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.core.util.MediaStoreBuckets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaStoreBucketsTest {
    @Test
    fun bucketIdsAreUniqueAndStable() {
        val ids = MediaStoreBuckets.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(-0x0001DEAD, MediaStoreBuckets.MEDIA_STORE_BUCKET_FAVORITES.id)
        assertEquals(-0x0006DEAD, MediaStoreBuckets.MEDIA_STORE_BUCKET_VIDEOS.id)
    }

    @Test
    fun placeholderDoesNotShareARealBucketId() {
        assertNotEquals(
            MediaStoreBuckets.MEDIA_STORE_BUCKET_PLACEHOLDER.id,
            MediaStoreBuckets.MEDIA_STORE_BUCKET_TIMELINE.id
        )
    }
}
