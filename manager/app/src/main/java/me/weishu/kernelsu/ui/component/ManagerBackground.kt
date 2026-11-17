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
    val bitmap = remember(path) {
        if (path.isEmpty()) null else runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    } ?: return

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = opacity,
        modifier = modifier.fillMaxSize()
    )
}
