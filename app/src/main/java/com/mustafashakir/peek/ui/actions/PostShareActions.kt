package com.mustafashakir.peek.ui.actions

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mustafashakir.peek.domain.model.PreparedMedia
import java.io.File

fun mediaClipData(context: Context, media: PreparedMedia): ClipData? =
    contentUri(context, media)?.let { uri ->
        ClipData.newUri(context.contentResolver, "Media", uri)
    }

fun shareMediaIntent(context: Context, media: List<PreparedMedia>): Intent? {
    val attachments = media.mapNotNull { item ->
        contentUri(context, item)?.let { uri -> MediaAttachment(uri, item.mimeType) }
    }
    if (attachments.size != media.size || attachments.isEmpty()) return null
    val commonType = attachments.map(MediaAttachment::mimeType).distinct().singleOrNull() ?: "*/*"
    return if (attachments.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, attachments.single().uri)
            clipData = ClipData.newUri(context.contentResolver, "Media", attachments.single().uri)
            type = commonType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                ArrayList(attachments.map(MediaAttachment::uri)),
            )
            clipData = ClipData.newUri(context.contentResolver, "Media", attachments.first().uri).apply {
                attachments.drop(1).forEach { attachment ->
                    addItem(ClipData.Item(attachment.uri))
                }
            }
            type = commonType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

private fun contentUri(context: Context, media: PreparedMedia): Uri? =
    runCatching {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(media.path),
        )
    }.getOrNull()

private data class MediaAttachment(
    val uri: Uri,
    val mimeType: String,
)
