/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.core.decoder.glide.EncryptedMediaStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EncryptedMediaStreamTest {
    @Test
    fun equalityUsesByteContentAndMetadata() {
        val first = EncryptedMediaStream(byteArrayOf(1, 2, 3), "image/jpeg", false)
        val same = EncryptedMediaStream(byteArrayOf(1, 2, 3), "image/jpeg", false)
        val different = EncryptedMediaStream(byteArrayOf(1, 2, 4), "image/jpeg", false)
        assertEquals(first, same)
        assertEquals(first.hashCode(), same.hashCode())
        assertNotEquals(first, different)
    }
}
