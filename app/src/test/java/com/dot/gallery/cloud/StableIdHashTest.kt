/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.cloud

import com.dot.gallery.cloud.core.stableIdHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableIdHashTest {
    @Test
    fun hashIsDeterministicAndPositive() {
        val first = stableIdHash("provider/account/remote")
        assertEquals(first, stableIdHash("provider/account/remote"))
        assertTrue(first >= 0L)
    }

    @Test
    fun differentInputsDoNotShareTheExpectedValue() {
        assertNotEquals(stableIdHash("one"), stableIdHash("two"))
    }
}
