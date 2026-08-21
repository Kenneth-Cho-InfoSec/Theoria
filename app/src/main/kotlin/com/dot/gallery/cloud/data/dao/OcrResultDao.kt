/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.cloud.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dot.gallery.cloud.data.entity.OcrResultEntity

@Dao
interface OcrResultDao {
    @Upsert
    suspend fun upsert(result: OcrResultEntity)

    @Query("SELECT * FROM ocr_results WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getByMediaId(mediaId: Long): OcrResultEntity?

    @Query("SELECT mediaId FROM ocr_results WHERE fullText LIKE '%' || :query || '%' COLLATE NOCASE")
    suspend fun findMediaIds(query: String): List<Long>

    @Query("DELETE FROM ocr_results WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: Long)
}
