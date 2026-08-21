/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.presentation.widget.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.Size
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object WidgetBitmapLoader {

    private const val CACHE_DIR = "widget_cache"
    private const val TAG = "WidgetBitmapLoader"

    /**
     * Loads a bitmap from [uri] using Glide (with all registered decoders) and
     * caches it as a JPEG file for the given [widgetId]/[index].
     * Returns true if the bitmap was successfully cached.
     */
    suspend fun loadAndCacheBitmap(
        context: Context,
        uri: Uri,
        widgetId: Int,
        index: Int,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): Boolean = withContext(Dispatchers.IO) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            Log.w(TAG, "Rejected non-positive widget bitmap dimensions")
            return@withContext false
        }
        val bitmap = loadBitmap(context, uri, maxWidth, maxHeight)
        if (bitmap == null) return@withContext false
        try {
            saveBitmapToFile(context, bitmap, widgetId, index)
            true
        } catch (error: Exception) {
            Log.w(TAG, "Failed to cache widget bitmap (${error::class.simpleName})")
            false
        }
    }

    /**
     * Reads a previously cached bitmap from file. This is a synchronous call
     * safe to use from AppWidgetProvider.onUpdate.
     */
    fun loadCachedBitmap(context: Context, widgetId: Int, index: Int): Bitmap? {
        val file = getBitmapFile(context, widgetId, index)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (error: Exception) {
            Log.w(TAG, "Failed to decode cached widget bitmap (${error::class.simpleName})")
            null
        }
    }

    fun clearCache(context: Context, widgetId: Int) {
        val dir = getCacheDir(context)
        dir.listFiles()?.filter { it.name.startsWith("widget_${widgetId}_") }?.forEach { it.delete() }
    }

    private suspend fun loadBitmap(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext

        // Try Glide first (handles HEIF, JXL, RAW, video thumbnails, GIF, etc.)
        try {
            val bitmap = Glide.with(appContext)
                .asBitmap()
                .load(uri)
                .centerCrop()
                .override(maxWidth, maxHeight)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .submit()
                .get()
            return@withContext bitmap
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Glide widget thumbnail failed (${error::class.simpleName})")
        }

        // Fallback: ContentResolver.loadThumbnail (API 29+)
        try {
            val bitmap = appContext.contentResolver.loadThumbnail(
                uri, Size(maxWidth, maxHeight), null
            )
            return@withContext bitmap
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "ContentResolver widget thumbnail failed (${error::class.simpleName})")
        }

        null
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap, widgetId: Int, index: Int) {
        val file = getBitmapFile(context, widgetId, index)
        val parent = file.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            error("Unable to create widget bitmap cache directory")
        }
        file.outputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                "Bitmap compression failed"
            }
        }
    }

    private fun getBitmapFile(context: Context, widgetId: Int, index: Int): File {
        return File(getCacheDir(context), "widget_${widgetId}_$index.jpg")
    }

    private fun getCacheDir(context: Context): File {
        return File(context.applicationContext.filesDir, CACHE_DIR)
    }
}
