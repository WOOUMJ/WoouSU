package me.weishu.kernelsu.ui.component.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.weishu.kernelsu.ui.component.LocalManagerAppearance
import me.weishu.kernelsu.ui.component.managerBlockColor
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Scaffold surface for full-screen routes: transparent so the wallpaper shows through. */
@Composable
fun managerScreenContainerColor(default: Color = MiuixTheme.colorScheme.surface): Color =
    if (LocalManagerAppearance.current.hasBackground) Color.Transparent else default

/** Top bar surface: transparent while blurred or when a wallpaper is active. */
@Composable
fun managerScreenBarColor(blurActive: Boolean): Color =
    if (blurActive || LocalManagerAppearance.current.hasBackground) {
        Color.Transparent
    } else {
        MiuixTheme.colorScheme.surface
    }

/** Card surface that follows the block opacity setting. */
@Composable
fun managerCardColors(color: Color = MiuixTheme.colorScheme.surface) =
    CardDefaults.defaultColors(color = managerBlockColor(color))
