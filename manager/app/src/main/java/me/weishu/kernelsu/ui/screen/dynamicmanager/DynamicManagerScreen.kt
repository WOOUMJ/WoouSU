package me.weishu.kernelsu.ui.screen.dynamicmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.TopBarBackButton
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.component.miuix.managerScreenBarColor
import me.weishu.kernelsu.ui.component.miuix.managerScreenContainerColor
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.DynamicManagerConfig
import me.weishu.kernelsu.ui.util.ExecResult
import me.weishu.kernelsu.ui.util.ManagerCandidate
import me.weishu.kernelsu.ui.util.clearDynamicManager
import me.weishu.kernelsu.ui.util.getDynamicManagerConfig
import me.weishu.kernelsu.ui.util.listManagerCandidates
import me.weishu.kernelsu.ui.util.setDynamicManagerFromApk
import me.weishu.kernelsu.ui.util.setDynamicManagerManual
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private const val TITLE = "动态管理器签名"

private const val HINT = "动态管理器签名用于让其它 KernelSU 系 root 管理器配合 WoouSU 内核工作。" +
    "选择下方扫描到的管理器后，它的签名会被写入内核并持久保存，重启后依然生效；" +
    "切换后 WoouSU 管理器会暂时失去管理器身份与 root，请打开目标管理器继续操作。"

private const val RESTORE_COMMANDS = "su -c 'L=\$(ls -dt /data/app/*/com.woousu.wo-*/lib/arm64/libksud.so|head -1); " +
    "cat \"\$L\">/data/adb/ksud; chmod 755 /data/adb/ksud; /data/adb/ksud dynamic-manager clear'"

@Composable
private fun CopyableCommands(background: Color, textColor: Color) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable {
                clipboard?.setPrimaryClip(ClipData.newPlainText("ksud", RESTORE_COMMANDS))
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            }
            .padding(10.dp),
    ) {
        Text(
            text = RESTORE_COMMANDS,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = textColor,
        )
    }
}

@Composable
private fun RestoreHint(bodyColor: Color, codeBackground: Color, codeColor: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("恢复方法：", fontSize = 12.sp, color = bodyColor)
        Text(
            text = "1. 在终端执行以下命令：",
            fontSize = 12.sp,
            color = bodyColor,
            modifier = Modifier.padding(top = 4.dp),
        )
        CopyableCommands(codeBackground, codeColor)
        Text(
            text = "（点击命令可直接复制粘贴）",
            fontSize = 11.sp,
            color = bodyColor,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "2. 删除 /data/adb/ksu/dynamic_manager.json 后重启。",
            fontSize = 12.sp,
            color = bodyColor,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private data class DynamicManagerState(
    val config: DynamicManagerConfig?,
    val candidates: List<ManagerCandidate>,
    val loading: Boolean,
    val busy: Boolean,
    val dialog: String?,
)

private class DynamicManagerHolder(
    val state: DynamicManagerState,
    val select: (ManagerCandidate) -> Unit,
    val applyManual: (String, String) -> Unit,
    val clear: () -> Unit,
    val dismissDialog: () -> Unit,
)

private suspend fun applyAndReport(
    action: suspend () -> ExecResult,
): Pair<DynamicManagerConfig?, String> {
    val result = action()
    val config = getDynamicManagerConfig()
    val text = if (result.ok) {
        "切换成功。请打开目标管理器继续操作；如需恢复 WoouSU 管理器，清除动态签名即可。" +
            if (result.message.isNotBlank()) "\n\n${result.message}" else ""
    } else {
        "操作失败。\n\n${result.message.ifBlank { "ksud 未返回错误信息" }}"
    }
    return config to text
}

@Composable
private fun rememberDynamicManagerState(): DynamicManagerHolder {
    var config by remember { mutableStateOf<DynamicManagerConfig?>(null) }
    var candidates by remember { mutableStateOf<List<ManagerCandidate>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        config = getDynamicManagerConfig()
        candidates = listManagerCandidates()
        loading = false
    }

    val holder = DynamicManagerHolder(
        state = DynamicManagerState(config, candidates, loading, busy, dialog),
        select = { candidate ->
            if (!busy) {
                scope.launch {
                    busy = true
                    val (newConfig, text) = applyAndReport { setDynamicManagerFromApk(candidate.apkPath) }
                    config = newConfig
                    busy = false
                    dialog = text
                }
            }
        },
        applyManual = { sizeText, hash ->
            if (!busy) {
                val size = sizeText.trim().let {
                    if (it.startsWith("0x") || it.startsWith("0X")) {
                        it.substring(2).toIntOrNull(16)
                    } else {
                        it.toIntOrNull()
                    }
                }
                if (size == null || size <= 0) {
                    dialog = "证书长度无效，请输入十进制或 0x 开头的十六进制数值。"
                } else {
                    scope.launch {
                        busy = true
                        val (newConfig, text) = applyAndReport { setDynamicManagerManual(size, hash.trim()) }
                        config = newConfig
                        busy = false
                        dialog = text
                    }
                }
            }
        },
        clear = {
            if (!busy) {
                scope.launch {
                    busy = true
                    val (newConfig, text) = applyAndReport { clearDynamicManager() }
                    config = newConfig
                    busy = false
                    dialog = text
                }
            }
        },
        dismissDialog = { dialog = null },
    )

    if (dialog != null) {
        AlertDialog(
            onDismissRequest = holder.dismissDialog,
            confirmButton = { TextButton(onClick = holder.dismissDialog) { Text("确定") } },
            text = { Text(dialog.orEmpty()) },
        )
    }

    return holder
}

@Composable
fun DynamicManagerScreen() {
    val navigator = LocalNavigator.current
    when (LocalUiMode.current) {
        UiMode.Miuix -> DynamicManagerScreenMiuix(onBack = { navigator.pop() })
        UiMode.Material -> DynamicManagerScreenMaterial(onBack = { navigator.pop() })
    }
}

private fun shortFingerprint(candidate: ManagerCandidate): String =
    "0x%x / %s…%s".format(
        candidate.certSize,
        candidate.certHash.take(12),
        candidate.certHash.takeLast(8),
    )

private fun statusText(config: DynamicManagerConfig?): String =
    if (config != null && config.isEnabled) {
        "当前已启用动态签名（0x%x）".format(config.size)
    } else {
        "当前未启用动态签名，正在使用 WoouSU 管理器"
    }

@Composable
private fun DynamicManagerScreenMaterial(onBack: () -> Unit) {
    val holder = rememberDynamicManagerState()
    val state = holder.state
    val cfg = state.config
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showManual by remember { mutableStateOf(false) }
    var manualSize by remember { mutableStateOf("") }
    var manualHash by remember { mutableStateOf("") }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(TITLE) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item {
                Text(
                    text = HINT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            item {
                Text(
                    text = statusText(state.config),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item {
                Text(
                    text = "已扫描到的 Root 管理器",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                )
            }
            if (state.loading) {
                item {
                    Text(
                        text = "正在扫描…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            } else if (state.candidates.isEmpty()) {
                item {
                    Text(
                        text = "未找到其它 KernelSU 系管理器（需要应用内包含 libksud.so）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            } else {
                items(state.candidates, key = { it.packageName }) { candidate ->
                    CandidateRowMaterial(
                        candidate = candidate,
                        isActive = cfg != null && cfg.isEnabled &&
                            cfg.size == candidate.certSize &&
                            cfg.hash == candidate.certHash,
                        enabled = !state.busy,
                        onClick = { holder.select(candidate) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (cfg != null && cfg.isEnabled) {
                            TextButton(onClick = holder.clear, enabled = !state.busy) {
                                Text("清除动态签名")
                            }
                        }
                        TextButton(
                            onClick = { showManual = !showManual },
                            enabled = !state.busy,
                        ) {
                            Text(if (showManual) "收起手动设置" else "手动输入签名")
                        }
                    }
                    if (showManual) {
                        OutlinedTextField(
                            value = manualSize,
                            onValueChange = { manualSize = it },
                            label = { Text("证书长度（如 0x307 或 775）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualHash,
                            onValueChange = { manualHash = it },
                            label = { Text("证书 SHA-256（64 位小写十六进制）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(
                            onClick = { holder.applyManual(manualSize, manualHash) },
                            enabled = !state.busy,
                        ) {
                            Text("应用")
                        }
                    }
                    RestoreHint(
                        bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        codeBackground = MaterialTheme.colorScheme.surfaceVariant,
                        codeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CandidateRowMaterial(
    candidate: ManagerCandidate,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isActive, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${candidate.packageName} · uid ${candidate.uid}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = shortFingerprint(candidate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (isActive) "当前" else "切换",
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun DynamicManagerScreenMiuix(onBack: () -> Unit) {
    val holder = rememberDynamicManagerState()
    val state = holder.state
    val cfg = state.config
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        containerColor = managerScreenContainerColor(),
        topBar = {
            SmallTopAppBar(
                title = TITLE,
                color = managerScreenBarColor(false),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
        ) {
            MiuixText(
                text = HINT,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            MiuixText(
                text = statusText(state.config),
                fontSize = 12.sp,
                color = colorScheme.primary,
            )
            MiuixText(
                text = "已扫描到的 Root 管理器",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    when {
                        state.loading -> MiuixText(
                            text = "正在扫描…",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(14.dp),
                        )

                        state.candidates.isEmpty() -> MiuixText(
                            text = "未找到其它 KernelSU 系管理器（需要应用内包含 libksud.so）。",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(14.dp),
                        )

                        else -> state.candidates.forEach { candidate ->
                            CandidateRowMiuix(
                                candidate = candidate,
                                isActive = cfg != null && cfg.isEnabled &&
                                    cfg.size == candidate.certSize &&
                                    cfg.hash == candidate.certHash,
                                enabled = !state.busy,
                                onClick = { holder.select(candidate) },
                            )
                        }
                    }
                }
            }
            if (cfg != null && cfg.isEnabled) {
                TextButton(
                    onClick = holder.clear,
                    enabled = !state.busy,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("清除动态签名")
                }
            }
            RestoreHint(
                bodyColor = colorScheme.onSurfaceVariantSummary,
                codeBackground = Color(0x1F888888),
                codeColor = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun CandidateRowMiuix(
    candidate: ManagerCandidate,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isActive, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MiuixText(
                text = candidate.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            MiuixText(
                text = "${candidate.packageName} · uid ${candidate.uid}",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
            MiuixText(
                text = shortFingerprint(candidate),
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
        MiuixText(
            text = if (isActive) "当前" else "切换",
            fontSize = 13.sp,
            color = if (isActive) colorScheme.onSurfaceVariantSummary else colorScheme.primary,
        )
    }
}
