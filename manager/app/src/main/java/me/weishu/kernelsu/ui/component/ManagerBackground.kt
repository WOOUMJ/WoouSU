package me.weishu.kernelsu.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
fun ManagerBackground(path: String, opacity: Float, modifier: Modifier = Modifier) {
    ManagerImage(
        path = path,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = opacity,
    )
}

/**
 * Draws an image file. [path] is expected to be unique per image (the picker writes a new
 * file name every time), so a plain [remember] keyed on it reloads instantly.
 */
@Composable
fun ManagerImage(
    path: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
) {
    val bitmap = remember(path) {
        if (path.isEmpty()) null else runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    } ?: return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = contentScale,
        alpha = alpha,
        modifier = modifier
    )
}
