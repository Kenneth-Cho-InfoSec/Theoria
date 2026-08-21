/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.ProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProviderTypeParseTest {
    @Test
    fun parseNormalizesWhitespaceAndCase() {
        assertEquals(ProviderType.WEBDAV, ProviderType.parse("  webdav "))
    }

    @Test
    fun parseRejectsUnknownValuesWithAnExplicitError() {
        assertThrows(IllegalStateException::class.java) {
            ProviderType.parse("not-a-provider")
        }
    }
}
