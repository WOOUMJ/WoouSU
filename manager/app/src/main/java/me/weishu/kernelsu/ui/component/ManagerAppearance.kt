package me.weishu.kernelsu.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.SwitchColors
import top.yukonga.miuix.kmp.basic.SwitchDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val statusCardScale: Float = 1f,
    val statusCardShowIcon: Boolean = true,
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

/**
 * Alpha multiplier for surfaces that animate their own alpha (e.g. the search
 * overlay). Multiply the animated alpha by this value so they still respect the
 * shared block opacity when a manager background image is active.
 */
@Composable
fun managerBlockAlpha(): Float {
    val appearance = LocalManagerAppearance.current
    return if (appearance.hasBackground) appearance.uiOpacity else 1f
}

/**
 * Miuix switch colors whose track follows the shared block opacity, so switches
 * no longer stay at a fixed opacity while other blocks fade with the slider.
 */
@Composable
fun managerSwitchColors(): SwitchColors {
    val scheme = MiuixTheme.colorScheme
    return SwitchDefaults.switchColors(
        checkedTrackColor = managerBlockColor(scheme.primary),
        uncheckedTrackColor = managerBlockColor(scheme.secondary),
    )
}
