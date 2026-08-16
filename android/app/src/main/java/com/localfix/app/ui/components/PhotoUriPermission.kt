package com.localfix.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri

fun persistPhotoAccess(context: Context, uri: Uri): Boolean = runCatching {
    context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
    )
}.isSuccess

fun releasePhotoAccess(context: Context, photoUri: String) {
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            Uri.parse(photoUri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
