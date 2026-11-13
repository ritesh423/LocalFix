package com.localfix.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Fetches a private ticket photo with the same Firebase identity as API calls. */
@Composable
fun AuthenticatedPhoto(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        bitmap = runCatching { fetchAuthenticatedPhoto(url) }.getOrNull()
        failed = bitmap == null
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = requireNotNull(bitmap).asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            failed -> Text("Photo unavailable")
            else -> CircularProgressIndicator()
        }
    }
}

private suspend fun fetchAuthenticatedPhoto(url: String): Bitmap? {
    val token = FirebaseAuth.getInstance().currentUser
        ?.getIdToken(false)
        ?.await()
        ?.token ?: return null
    return withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 90_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (connection.responseCode !in 200..299) return@withContext null
            val bytes = connection.inputStream.use { it.readBytes() }
            if (bytes.size > 5 * 1024 * 1024) return@withContext null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } finally {
            connection.disconnect()
        }
    }
}
