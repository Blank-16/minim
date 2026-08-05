package com.minim.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real icon rendering: loads via the given suspend loader (backed by
 * IconLoader's LRU cache — see data/repository/IconLoader.kt), converts the
 * platform Drawable to an ImageBitmap off the main thread, and holds only
 * that single bitmap in composition state. Shows a soft placeholder while
 * loading so the list never jank-shifts as icons pop in.
 */
@Composable
fun AppIcon(
    packageName: String,
    loader: suspend (String) -> Any?,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        val drawable = loader(packageName) as? Drawable ?: return@LaunchedEffect
        val converted = withContext(Dispatchers.Default) { drawable.toBitmapSafely() }
        bitmap = converted?.asImageBitmap()
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

private fun Drawable.toBitmapSafely(size: Int = 96): Bitmap? = runCatching {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, size, size)
    draw(canvas)
    bmp
}.getOrNull()
