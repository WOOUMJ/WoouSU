package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.component.miuix.managerCardColors
import me.weishu.kernelsu.ui.util.HookInfo
import me.weishu.kernelsu.ui.util.detectHook
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private fun hookSummary(info: HookInfo): String =
    "${info.type}  ${info.version}" + if (info.active) " · 运行中" else " · 未激活"

@Composable
fun HookEntryCardMiuix(onClick: () -> Unit) {
    val info by produceState<HookInfo?>(null) { value = detectHook() }
    val current = info ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = managerCardColors(colorScheme.surfaceContainer),
    ) {
        ArrowPreference(
            title = "钩子类型",
            summary = hookSummary(current),
            onClick = onClick,
        )
    }
}

@Composable
fun KpmEntryCardMiuix(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = managerCardColors(colorScheme.surfaceContainer),
    ) {
        ArrowPreference(
            title = "KPM 内核模块",
            summary = "KernelPatch Module 管理与加载",
            onClick = onClick,
        )
    }
}

@Composable
private fun EntryCardMaterial(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun HookEntryCardMaterial(onClick: () -> Unit) {
    val info by produceState<HookInfo?>(null) { value = detectHook() }
    val current = info ?: return
    EntryCardMaterial(
        title = "钩子类型",
        summary = hookSummary(current),
        onClick = onClick,
    )
}

@Composable
fun KpmEntryCardMaterial(onClick: () -> Unit) {
    EntryCardMaterial(
        title = "KPM 内核模块",
        summary = "KernelPatch Module 管理与加载",
        onClick = onClick,
    )
}
