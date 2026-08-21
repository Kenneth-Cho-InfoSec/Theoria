/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.cloud.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.dot.gallery.cloud.core.OcrResult
import com.dot.gallery.cloud.core.OcrBlock
import com.dot.gallery.cloud.core.ProviderCapability
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.OcrCapableProvider
import com.dot.gallery.cloud.data.dao.OcrResultDao
import com.dot.gallery.cloud.data.entity.OcrResultEntity
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local on-device OCR provider.
 * Uses the bundled ML Kit text recognizer and persists results in Room.
 */
@Singleton
class LocalOcrProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    database: InternalDatabase,
) : LocalCapabilityProvider(), OcrCapableProvider {

    override val providerType: ProviderType = ProviderType.LOCAL_OCR
    override val displayName: String = ProviderType.LOCAL_OCR.displayName
    override val capabilities: Set<ProviderCapability> = setOf(ProviderCapability.OCR)

    private val ocrDao: OcrResultDao = database.getOcrResultDao()
    private val mediaDao = database.getMediaDao()
    private var recognizer: TextRecognizer? = null

    override suspend fun initialize() {
        if (recognizer == null) {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    override fun release() {
        recognizer?.close()
        recognizer = null
    }

    override val isAvailable: Boolean
        get() = recognizer != null

    override suspend fun extractText(mediaId: Long): OcrResult? {
        val existing = ocrDao.getByMediaId(mediaId)
        if (existing != null) return existing.toResult()

        val media = mediaDao.getMediaById(mediaId)
        val activeRecognizer = recognizer ?: return null
        val result = withContext(Dispatchers.IO) {
            val image = InputImage.fromFilePath(context, media.uri)
            Tasks.await(activeRecognizer.process(image))
        }
        val blocks = result.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val bounds = line.boundingBox
                OcrBlock(
                    text = line.text,
                    left = bounds?.left?.toFloat() ?: 0f,
                    top = bounds?.top?.toFloat() ?: 0f,
                    right = bounds?.right?.toFloat() ?: 0f,
                    bottom = bounds?.bottom?.toFloat() ?: 0f,
                    confidence = line.confidence
                )
            }
        }
        val ocrResult = OcrResult(result.text, blocks, providerType)
        ocrDao.upsert(
            OcrResultEntity(
                mediaId = mediaId,
                fullText = ocrResult.fullText,
                blocksJson = Json.encodeToString(blocks),
                timestamp = System.currentTimeMillis()
            )
        )
        return ocrResult
    }

    override fun searchByText(query: String): Flow<Resource<List<Media>>> {
        val normalizedQuery = query.trim()
        return flow {
            if (normalizedQuery.isEmpty()) {
                emit(Resource.Success(emptyList()))
            } else {
                val ids = ocrDao.findMediaIds(normalizedQuery)
                val media: List<Media> = mediaDao.getMediaByIds(ids)
                emit(Resource.Success(media))
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun OcrResultEntity.toResult(): OcrResult = OcrResult(
        fullText = fullText,
        blocks = runCatching { Json.decodeFromString<List<OcrBlock>>(blocksJson) }
            .getOrDefault(emptyList()),
        providerType = providerType
    )
}
