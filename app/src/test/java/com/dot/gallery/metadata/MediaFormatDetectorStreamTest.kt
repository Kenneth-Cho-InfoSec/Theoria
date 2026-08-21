/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.metadata

import com.dot.gallery.core.metadata.MediaContainerFormat
import com.dot.gallery.core.metadata.MediaFormatDetector
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFormatDetectorStreamTest {
    @Test
    fun detectsFormatsWhenTheStreamReturnsShortReads() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val oneByteAtATime = object : FilterInputStream(ByteArrayInputStream(png)) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                super.read(buffer, offset, length.coerceAtMost(1))
        }

        assertEquals(MediaContainerFormat.PNG, MediaFormatDetector.detect(oneByteAtATime))
    }
}
