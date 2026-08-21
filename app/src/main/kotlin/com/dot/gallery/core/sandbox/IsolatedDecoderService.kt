/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.core.sandbox

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.SharedMemory
import java.nio.ByteBuffer

/**
 * Isolated-process service for image decoding.
 *
 * Runs under a fake UID with no permissions, no app data, and no network.
 * Decodes HEIF/AVIF/JXL images in a sandbox — if a malformed file exploits a codec
 * bug, the attacker cannot reach the main app process.
 *
 * Communication:
 * - Input:  [SharedMemory] containing the encoded image bytes + metadata in Bundle
 * - Output: [SharedMemory] containing decoded ARGB_8888 pixels + dimensions in Bundle
 */
class IsolatedDecoderService : Service() {

    private lateinit var messenger: Messenger

    override fun onCreate() {
        super.onCreate()
        messenger = Messenger(IncomingHandler(Looper.getMainLooper()))
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    private inner class IncomingHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val replyTo = msg.replyTo ?: return
            val data = msg.data
            data.classLoader = SharedMemory::class.java.classLoader

            val reply = Message.obtain()
            reply.what = msg.what

            try {
                reply.data = when (msg.what) {
                    MSG_DECODE -> decodeImage(data)
                    else -> Bundle().apply { putBoolean(KEY_ERROR, true) }
                }
            } catch (e: Exception) {
                reply.data = Bundle().apply {
                    putBoolean(KEY_ERROR, true)
                    putString(KEY_ERROR_MESSAGE, e.message)
                }
            }

            try {
                replyTo.send(reply)
            } catch (_: Exception) {
                // Client is gone
            } finally {
                // The fd backing the output SharedMemory is duplicated into the Binder
                // transaction during send(); the original handle in this process still
                // remains open and must be closed explicitly to avoid leaking ashmem fds.
                reply.data?.let { it.classLoader = SharedMemory::class.java.classLoader }
                @Suppress("DEPRECATION")
                reply.data?.getParcelable<SharedMemory>(KEY_OUTPUT_SHM)?.close()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun decodeImage(input: Bundle): Bundle {
        val inputShm = input.getParcelable<SharedMemory>(KEY_INPUT_SHM)
            ?: return Bundle().apply { putBoolean(KEY_ERROR, true) }
        val mimeType = input.getString(KEY_MIME_TYPE, "")
        val targetW = input.getInt(KEY_TARGET_WIDTH, 0)
        val targetH = input.getInt(KEY_TARGET_HEIGHT, 0)
        val byteCount = input.getInt(KEY_BYTE_COUNT, 0)

        // Read encoded bytes from shared memory
        val inputBuffer = inputShm.mapReadOnly()
        val encodedBytes = ByteArray(byteCount)
        inputBuffer.get(encodedBytes)
        SharedMemory.unmap(inputBuffer)
        inputShm.close()

        // Decode based on mime type
        val bitmap = when {
            isHeifMime(mimeType) -> decodeBitmap(encodedBytes, targetW, targetH)
            mimeType.equals("image/jxl", ignoreCase = true) -> decodeBitmap(encodedBytes, targetW, targetH)
            else -> decodeBitmap(encodedBytes, targetW, targetH)
        } ?: return Bundle().apply {
            putBoolean(KEY_ERROR, true)
            putString(KEY_ERROR_MESSAGE, "Decode returned null")
        }

        // Write decoded pixels to shared memory
        val pixelByteCount = bitmap.byteCount
        val outputShm = SharedMemory.create("decoded_pixels", pixelByteCount)
        val outputBuffer = outputShm.mapReadWrite()
        bitmap.copyPixelsToBuffer(outputBuffer)
        SharedMemory.unmap(outputBuffer)

        val result = Bundle()
        result.putParcelable(KEY_OUTPUT_SHM, outputShm)
        result.putInt(KEY_DECODED_WIDTH, bitmap.width)
        result.putInt(KEY_DECODED_HEIGHT, bitmap.height)
        result.putInt(KEY_DECODED_BYTE_COUNT, pixelByteCount)
        result.putString(KEY_DECODED_CONFIG, (bitmap.config ?: Bitmap.Config.ARGB_8888).name)

        bitmap.recycle()
        return result
    }

    private fun decodeBitmap(bytes: ByteArray, targetW: Int, targetH: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var inSampleSize = 1
        if (targetW > 0 && targetH > 0) {
            val halfW = bounds.outWidth / 2
            val halfH = bounds.outHeight / 2
            while (halfW / inSampleSize >= targetW && halfH / inSampleSize >= targetH) {
                inSampleSize *= 2
            }
        }

        val opts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun isHeifMime(mime: String): Boolean {
        val lower = mime.lowercase()
        return lower in HEIF_MIME_TYPES || lower.substringBefore(';') in HEIF_MIME_TYPES
    }

    companion object {
        const val MSG_DECODE = 1

        // Input keys
        const val KEY_INPUT_SHM = "input_shm"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_TARGET_WIDTH = "target_width"
        const val KEY_TARGET_HEIGHT = "target_height"
        const val KEY_BYTE_COUNT = "byte_count"

        // Output keys
        const val KEY_OUTPUT_SHM = "output_shm"
        const val KEY_DECODED_WIDTH = "decoded_width"
        const val KEY_DECODED_HEIGHT = "decoded_height"
        const val KEY_DECODED_BYTE_COUNT = "decoded_byte_count"
        const val KEY_DECODED_CONFIG = "decoded_config"

        // Error keys
        const val KEY_ERROR = "error"
        const val KEY_ERROR_MESSAGE = "error_message"

        private val HEIF_MIME_TYPES = setOf(
            "image/heif", "image/heic", "image/heif-sequence",
            "image/heic-sequence", "image/avif", "image/avis"
        )
    }
}
