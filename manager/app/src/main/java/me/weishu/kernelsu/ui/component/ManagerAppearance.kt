package me.weishu.kernelsu.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Global, instantly-updated appearance settings shared by every page.
 */
@Immutable
data class ManagerAppearance(
    val backgroundImagePath: String = "",
    val backgroundOpacity: Float = 0.7f,
    val uiOpacity: Float = 0.5f,
    val hideVersionMismatch: Boolean = false,
    val statusText: String = "",
    val statusImagePath: String = "",
) {
    val hasBackground: Boolean get() = backgroundImagePath.isNotEmpty()
}

val LocalManagerAppearance = staticCompositionLocalOf { ManagerAppearance() }

/**
 * Adjust the container color of a card/block/button when a manager background
 * image is active, so the background shows through these blocks.
 */
@Composable
fun managerBlockColor(color: Color): Color {
    val appearance = LocalManagerAppearance.current
    if (!appearance.hasBackground || appearance.uiOpacity >= 1f) return color
    return color.copy(alpha = appearance.uiOpacity)
}
