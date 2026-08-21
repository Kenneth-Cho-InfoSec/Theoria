/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0 AND MPL-2.0
 */

package com.dot.gallery.core.decryption

import android.content.Context
import androidx.collection.LruCache
import com.dot.gallery.core.metrics.MetricsCollector
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton

data class DecryptResult(val bytes: ByteArray, val mimeType: String)

/** Metadata cached to avoid re-decrypting large files just for header info. */
data class MediaMetadataCacheEntry(
    val path: String,
    val mimeType: String,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?
)

@Singleton
class DecryptManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val metrics: MetricsCollector
) {
    // Small LRU for decrypted byte arrays (only small items inserted)
    private val lru = object : LruCache<String, DecryptResult>(32) {
        override fun sizeOf(key: String, value: DecryptResult): Int = value.bytes.size
    }
    private val inFlight = ConcurrentHashMap<String, CompletableFuture<DecryptResult>>()
    private val keychainHolder by lazy { KeychainHolder(context) }

    fun decrypt(file: File): DecryptResult {
        val key = hash(file)
        lru[key]?.let {
            metrics.incLruHit()
            return it
        }
        metrics.incLruMiss()
        // Single-flight: all callers share the owner's result, including failures.
        val future = CompletableFuture<DecryptResult>()
        val owner = inFlight.putIfAbsent(key, future)
        if (owner != null) {
            metrics.incDecryptWaiters(1)
            return try {
                owner.get()
            } catch (error: ExecutionException) {
                throw (error.cause ?: error)
            }
        }
        try {
            metrics.incDecryptInvocation()
            val decrypted = keychainHolder.decryptVaultMedia(file)
            val result = DecryptResult(decrypted.readBytes(), decrypted.mimeType)
            decrypted.cleanup()
            // Only cache small results (< 2MB) to keep memory bounded
            if (result.bytes.size <= 2 * 1024 * 1024) {
                lru.put(key, result)
            }
            future.complete(result)
            return result
        } catch (error: Throwable) {
            future.completeExceptionally(error)
            throw error
        } finally {
            inFlight.remove(key, future)
        }
    }

    private fun hash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(file.path.encodeToByteArray())
        val digest = md.digest()
        return digest.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}

/** Simple file-based sidecar metadata cache (lazy). */
@Singleton
class MediaMetadataSidecarCache @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dir by lazy { File(context.cacheDir, "meta_sidecar").apply { mkdirs() } }

    fun read(key: String): MediaMetadataCacheEntry? {
        val f = File(dir, key)
        if (!f.exists()) return null
        return try {
            val text = f.readText()
            val parts = text.split('|')
            MediaMetadataCacheEntry(
                path = parts.getOrNull(0) ?: return null,
                mimeType = parts.getOrNull(1) ?: return null,
                width = parts.getOrNull(2)?.toIntOrNull(),
                height = parts.getOrNull(3)?.toIntOrNull(),
                durationMs = parts.getOrNull(4)?.toLongOrNull()
            )
        } catch (_: Throwable) { null }
    }

    fun write(entry: MediaMetadataCacheEntry) {
        val key = hash(entry.path)
        val f = File(dir, key)
        runCatching {
            f.writeText(
                listOf(
                    entry.path, entry.mimeType,
                    entry.width?.toString() ?: "",
                    entry.height?.toString() ?: "",
                    entry.durationMs?.toString() ?: ""
                ).joinToString("|")
            )
        }
    }

    fun keyForFile(file: File): String = hash(file.path)

    private fun hash(path: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(path.encodeToByteArray())
        return md.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }
}

// EntryPoint so non-Hilt classes (like static createEncryptedMediaSource) can access manager.
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DecryptManagerEntryPoint {
    fun decryptManager(): DecryptManager
    fun sidecar(): MediaMetadataSidecarCache
}
