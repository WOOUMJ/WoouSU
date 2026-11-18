package me.weishu.kernelsu.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Global, instantly-updated appearance settings shared by every page.
 */
@Immutable
data class ManagerAppearance(
    val backgroundImagePath: String = "",
    val backgroundOpacity: Float = 0.35f,
    val hideVersionMismatch: Boolean = false,
    val statusText: String = "",
    val statusImagePath: String = "",
) {
    val hasBackground: Boolean get() = backgroundImagePath.isNotEmpty()
}

val LocalManagerAppearance = staticCompositionLocalOf { ManagerAppearance() }
