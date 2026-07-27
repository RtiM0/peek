package com.mustafashakir.peek.ui.actions

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.mustafashakir.peek.ui.model.UiImage
import com.mustafashakir.peek.ui.model.ViewerMediaItemUiModel
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DOWNLOAD_SUBFOLDER = "peek"

private fun mimeTypeFor(item: ViewerMediaItemUiModel): String =
    if (item.videoUrl != null) "video/mp4" else "image/png"

private fun extensionFor(item: ViewerMediaItemUiModel): String =
    if (item.videoUrl != null) "mp4" else "png"

private fun imageModel(image: UiImage): Any = when (image) {
    is UiImage.Resource -> image.id
    is UiImage.Url -> image.value
}

private suspend fun fetchBitmap(context: Context, image: UiImage): Bitmap? {
    val loader = SingletonImageLoader.get(context)
    val request = ImageRequest.Builder(context).data(imageModel(image)).build()
    val result = loader.execute(request)
    return (result as? SuccessResult)?.image?.toBitmap()
}

/** Fetches the item's media (image via Coil, video via raw HTTP) into a fresh cache file. */
suspend fun fetchMediaFile(context: Context, item: ViewerMediaItemUiModel): File? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "shared_media").apply { mkdirs() }
            val file = File(dir, "peek_${item.id.hashCode()}_${System.nanoTime()}.${extensionFor(item)}")
            val videoUrl = item.videoUrl
            if (videoUrl != null) {
                URL(videoUrl).openStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            } else {
                val bitmap = fetchBitmap(context, item.image) ?: return@runCatching null
                file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
            }
            file
        }.getOrNull()
    }

/** Fetches the item's media into a cache file and returns a FileProvider content Uri for it. */
suspend fun mediaClipUri(context: Context, item: ViewerMediaItemUiModel): Uri? {
    val file = fetchMediaFile(context, item) ?: return null
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private suspend fun saveFileToDownloads(
    context: Context,
    sourceFile: File,
    mimeType: String,
    displayName: String,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_SUBFOLDER")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: return@runCatching false
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_SUBFOLDER)
            downloadsDir.mkdirs()
            val destFile = File(downloadsDir, displayName)
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf(mimeType), null)
        }
        true
    }.getOrDefault(false)
}

/** Downloads every item into Downloads/peek. Returns how many succeeded. */
suspend fun downloadMediaItems(context: Context, items: List<ViewerMediaItemUiModel>): Int {
    var successCount = 0
    for (item in items) {
        val file = fetchMediaFile(context, item) ?: continue
        val displayName = "peek_${item.id.hashCode()}_${System.nanoTime()}.${extensionFor(item)}"
        if (saveFileToDownloads(context, file, mimeTypeFor(item), displayName)) successCount++
    }
    return successCount
}

/** Builds a share Intent (single or multi) attaching only the real media file(s), no link/caption text. */
suspend fun shareMediaIntent(context: Context, items: List<ViewerMediaItemUiModel>): Intent? {
    val uris = items.mapNotNull { mediaClipUri(context, it) }
    if (uris.isEmpty()) return null
    val commonType = items.map(::mimeTypeFor).distinct().singleOrNull() ?: "*/*"
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, uris.first()) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply { putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris)) }
    }
    return intent.apply {
        type = commonType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
