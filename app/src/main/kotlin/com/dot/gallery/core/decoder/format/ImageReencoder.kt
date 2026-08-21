/*
 * SPDX-FileCopyrightText: 2026 kennethcho
 * SPDX-License-Identifier: MPL-2.0
 */

package com.dot.gallery.core.decoder.format

import android.graphics.Bitmap
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.OutputStream

object ImageReencoder {

    enum class ImageWriteFormat(val mimeType: String, val fileExtension: String) {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp")
    }

    enum class QualityMode {
        AUTO,
        MANUAL
    }

    data class ReencodeConfig(
        val mode: QualityMode = QualityMode.AUTO,
        val lossyQuality: Int = 90,
        val jxlEffort: Int = 7,
        val jxlLossless: Boolean = true,
        val detectedQuality: Int? = null
    ) {
        val effectiveLossyQuality: Int
            get() = if (mode == QualityMode.MANUAL) lossyQuality else (detectedQuality ?: lossyQuality)
    }

    fun isReencodable(mime: String?, label: String?): Boolean = formatForMime(mime, label) != null

    fun formatForMime(mime: String?, label: String?): ImageWriteFormat? {
        val m = mime?.lowercase().orEmpty()
        when {
            m.contains("jpeg") || m.contains("jpg") -> return ImageWriteFormat.JPEG
            m.contains("png") -> return ImageWriteFormat.PNG
            m.contains("webp") -> return ImageWriteFormat.WEBP
        }
        val labelLower = label?.lowercase().orEmpty()
        return when {
            labelLower.endsWith(".jpg") || labelLower.endsWith(".jpeg") -> ImageWriteFormat.JPEG
            labelLower.endsWith(".png") -> ImageWriteFormat.PNG
            labelLower.endsWith(".webp") -> ImageWriteFormat.WEBP
            else -> null
        }
    }

    fun encodeToBytes(bitmap: Bitmap, format: ImageWriteFormat, config: ReencodeConfig): ByteArray {
        val quality = if (config.mode == QualityMode.MANUAL) config.lossyQuality.coerceIn(0, 100) else 90
        val compressFormat = when (format) {
            ImageWriteFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageWriteFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageWriteFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
        val outQuality = if (format == ImageWriteFormat.PNG) 100 else quality
        val buffer = ByteArrayOutputStream()
        bitmap.compress(compressFormat, outQuality, buffer)
        return buffer.toByteArray()
    }

    fun writeToStream(bitmap: Bitmap, format: ImageWriteFormat, config: ReencodeConfig, out: OutputStream) {
        out.write(encodeToBytes(bitmap, format, config))
        out.flush()
    }
}
