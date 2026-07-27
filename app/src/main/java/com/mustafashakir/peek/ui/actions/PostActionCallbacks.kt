package com.mustafashakir.peek.ui.actions

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.mustafashakir.peek.R
import com.mustafashakir.peek.ui.model.ViewerMediaItemUiModel
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

data class PostActionCallbacks(
    val onCopyLink: suspend (String) -> Unit,
    val onCopyMedia: suspend (ViewerMediaItemUiModel) -> Unit,
    val onDownload: suspend (List<ViewerMediaItemUiModel>) -> Unit,
    val onShare: suspend (List<ViewerMediaItemUiModel>) -> Unit,
)

@Composable
fun rememberPostActionCallbacks(): PostActionCallbacks {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val linkCopied = stringResource(R.string.link_copied)
    val mediaCopied = stringResource(R.string.media_copied)
    val actionFailed = stringResource(R.string.action_failed)
    val mediaSavedOne = stringResource(R.string.media_saved)
    val mediaSavedMany = stringResource(R.string.media_saved_multiple)

    fun savedMessage(count: Int) = if (count == 1) mediaSavedOne else mediaSavedMany.format(count)
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    var pendingPermissionContinuation by remember { mutableStateOf<CancellableContinuation<Boolean>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingPermissionContinuation?.let { continuation -> if (continuation.isActive) continuation.resume(granted) }
        pendingPermissionContinuation = null
    }

    return PostActionCallbacks(
        onCopyLink = { url ->
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Link", url)))
            toast(linkCopied)
        },
        onCopyMedia = { item ->
            val uri = mediaClipUri(context, item)
            if (uri != null) {
                clipboard.setClipEntry(ClipEntry(ClipData.newUri(context.contentResolver, "Media", uri)))
                toast(mediaCopied)
            } else {
                toast(actionFailed)
            }
        },
        onDownload = { items ->
            val needsLegacyPermission = Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            val granted = if (needsLegacyPermission) {
                suspendCancellableCoroutine { continuation ->
                    pendingPermissionContinuation = continuation
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            } else {
                true
            }
            if (granted) {
                val count = downloadMediaItems(context, items)
                toast(if (count > 0) savedMessage(count) else actionFailed)
            } else {
                toast(actionFailed)
            }
        },
        onShare = { items ->
            val intent = shareMediaIntent(context, items)
            if (intent != null) {
                context.startActivity(Intent.createChooser(intent, null))
            } else {
                toast(actionFailed)
            }
        },
    )
}
