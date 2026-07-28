package com.mustafashakir.peek.data.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import com.mustafashakir.peek.domain.model.PreparedMedia
import com.mustafashakir.peek.domain.model.RemoteMedia
import com.mustafashakir.peek.domain.model.RemoteMediaKind
import com.mustafashakir.peek.domain.repository.MediaRepository
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

private const val DOWNLOAD_SUBFOLDER = "peek"
private const val SHARED_MEDIA_SUBFOLDER = "shared_media"
private const val CACHE_MAX_AGE_MILLIS = 24L * 60 * 60 * 1_000

class AndroidMediaRepository(
    context: Context,
    private val client: OkHttpClient = defaultHttpClient(),
) : MediaRepository {
    private val appContext = context.applicationContext
    private val sharedMediaDirectory = File(appContext.cacheDir, SHARED_MEDIA_SUBFOLDER)

    override suspend fun prepareForSharing(media: List<RemoteMedia>): Result<List<PreparedMedia>> {
        if (media.isEmpty()) return Result.failure(IllegalArgumentException("No media requested"))
        removeExpiredCacheFiles()
        val prepared = mutableListOf<PreparedMedia>()
        return try {
            media.forEach { prepared += fetch(it) }
            Result.success(prepared)
        } catch (error: Exception) {
            prepared.forEach(::deletePreparedFile)
            if (error is CancellationException) throw error
            Result.failure(error)
        }
    }

    override suspend fun download(media: List<RemoteMedia>): Int {
        removeExpiredCacheFiles()
        var savedCount = 0
        for (item in media) {
            val prepared = try {
                fetch(item)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                continue
            }
            try {
                if (saveToDownloads(prepared)) savedCount++
            } finally {
                deletePreparedFile(prepared)
            }
        }
        return savedCount
    }

    private suspend fun fetch(media: RemoteMedia): PreparedMedia =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(Request.Builder().url(media.url).build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!continuation.isActive) return
                            try {
                                if (!response.isSuccessful) {
                                    throw IOException("Media request failed with HTTP ${response.code}")
                                }
                                val body = response.body ?: throw IOException("Media response had no body")
                                val mimeType = normalizedMimeType(body.contentType()?.toString(), media.kind)
                                val file = newCacheFile(media, mimeType)
                                try {
                                    body.byteStream().use { input ->
                                        file.outputStream().buffered().use { output -> input.copyTo(output) }
                                    }
                                } catch (error: Exception) {
                                    file.delete()
                                    throw error
                                }
                                val prepared = PreparedMedia(file.absolutePath, mimeType)
                                continuation.resume(prepared) { _, _, _ -> file.delete() }
                            } catch (error: Exception) {
                                if (continuation.isActive) continuation.resumeWithException(error)
                            }
                        }
                    }
                },
            )
        }

    private fun newCacheFile(media: RemoteMedia, mimeType: String): File {
        check(sharedMediaDirectory.exists() || sharedMediaDirectory.mkdirs()) {
            "Could not create media cache directory"
        }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: if (media.kind == RemoteMediaKind.Video) "mp4" else "jpg"
        return File.createTempFile("peek_${media.id.hashCode()}_", ".$extension", sharedMediaDirectory)
    }

    private suspend fun saveToDownloads(media: PreparedMedia): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(media)
        } else {
            saveToLegacyDownloads(media)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(media: PreparedMedia): Boolean {
        val resolver = appContext.contentResolver
        val displayName = File(media.path).name
        var uri: Uri? = null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, media.mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBFOLDER")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            uri = resolver.insert(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                values,
            ) ?: return false
            resolver.openOutputStream(uri)?.use { output ->
                File(media.path).inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Could not open Downloads output stream")
            val published = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
            check(published == 1) { "Could not publish downloaded media" }
            true
        } catch (_: Exception) {
            uri?.let { pendingUri ->
                runCatching { resolver.delete(pendingUri, null, null) }
            }
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyDownloads(media: PreparedMedia): Boolean {
        val downloadsDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOAD_SUBFOLDER,
        )
        if (!downloadsDirectory.exists() && !downloadsDirectory.mkdirs()) return false
        val destination = File(downloadsDirectory, File(media.path).name)
        val partial = File(downloadsDirectory, "${destination.name}.part")
        return try {
            File(media.path).inputStream().use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
            check(partial.renameTo(destination)) { "Could not finalize downloaded media" }
            MediaScannerConnection.scanFile(
                appContext,
                arrayOf(destination.absolutePath),
                arrayOf(media.mimeType),
                null,
            )
            true
        } catch (_: Exception) {
            partial.delete()
            false
        }
    }

    private suspend fun removeExpiredCacheFiles() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MILLIS
        sharedMediaDirectory.listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoff }
            ?.forEach(File::delete)
    }

    private fun deletePreparedFile(media: PreparedMedia) {
        File(media.path).delete()
    }
}

private fun normalizedMimeType(contentType: String?, kind: RemoteMediaKind): String {
    val normalized = contentType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()
        ?.takeIf { type ->
            when (kind) {
                RemoteMediaKind.Image -> type.startsWith("image/")
                RemoteMediaKind.Video -> type.startsWith("video/")
            }
        }
    return normalized ?: when (kind) {
        RemoteMediaKind.Image -> "image/jpeg"
        RemoteMediaKind.Video -> "video/mp4"
    }
}

private fun defaultHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
