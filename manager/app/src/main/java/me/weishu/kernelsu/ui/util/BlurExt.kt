package me.weishu.kernelsu.ui.util

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import me.weishu.kernelsu.ui.component.LocalManagerAppearance
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Loads the manager background image so it can be drawn inside a blur backdrop layer.
 * Without this the backdrop only contains an opaque surface color, which makes glass
 * effects look like a solid gray strip when a wallpaper is set.
 */
@Composable
fun rememberManagerBackgroundBitmap(): ImageBitmap? {
    val path = LocalManagerAppearance.current.backgroundImagePath
    return remember(path) {
        if (path.isEmpty()) {
            null
        } else {
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.asImageBitmap()
        }
    }
}

/** Draws [bitmap] with the same center-crop behavior as `ContentScale.Crop`. */
fun DrawScope.drawCroppedBackground(bitmap: ImageBitmap, alpha: Float) {
    if (bitmap.width == 0 || bitmap.height == 0 || size.width <= 0f || size.height <= 0f) return
    val scale = max(size.width / bitmap.width, size.height / bitmap.height)
    val width = bitmap.width * scale
    val height = bitmap.height * scale
    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(
            ((size.width - width) / 2f).roundToInt(),
            ((size.height - height) / 2f).roundToInt(),
        ),
        dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        alpha = alpha,
    )
}

@Composable
fun rememberBlurBackdrop(
    enableBlur: Boolean,
    background: ImageBitmap? = null,
): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backgroundAlpha = LocalManagerAppearance.current.backgroundOpacity
    val backgroundState = rememberUpdatedState(background)
    val alphaState = rememberUpdatedState(backgroundAlpha)
    return rememberLayerBackdrop {
        val bitmap = backgroundState.value
        if (bitmap != null) {
            drawCroppedBackground(bitmap, alphaState.value)
        } else {
            drawRect(surfaceColor)
        }
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    content: @Composable () -> Unit,
) {
    // When a full-screen manager background image is active, the backdrop layer
    // does not contain the image (it is drawn behind the scaffold), so blurring
    // the bar would render an opaque surface strip that visually cuts the
    // background in two. Keep the bar transparent over the image instead.
    val hasBackground = LocalManagerAppearance.current.hasBackground
    val effective = blurActive && !hasBackground
    Box(
        modifier = if (effective && backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}
