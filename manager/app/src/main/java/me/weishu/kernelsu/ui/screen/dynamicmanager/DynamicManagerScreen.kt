package me.weishu.kernelsu.ui.screen.dynamicmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.TonalCard
import me.weishu.kernelsu.ui.component.material.TopBarBackButton
import me.weishu.kernelsu.ui.component.material.expressiveTopAppBarColors
import me.weishu.kernelsu.ui.component.miuix.managerCardColors
import me.weishu.kernelsu.ui.component.miuix.managerScreenBarColor
import me.weishu.kernelsu.ui.component.miuix.managerScreenContainerColor
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.DynamicManagerConfig
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

private data class DynamicManagerState(
    val config: DynamicManagerConfig?,
    val candidates: List<ManagerCandidate>,
    val loading: Boolean,
    val busy: Boolean,
    val message: String?,
)

private class DynamicManagerHolder(
    val state: DynamicManagerState,
    val select: (ManagerCandidate) -> Unit,
    val applyManual: (String, String) -> Unit,
    val clear: () -> Unit,
    val dismissMessage: () -> Unit,
)

@Composable
private fun rememberDynamicManagerState(): DynamicManagerHolder {
    var config by remember { mutableStateOf<DynamicManagerConfig?>(null) }
    var candidates by remember { mutableStateOf<List<ManagerCandidate>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = true
        config = getDynamicManagerConfig()
        candidates = listManagerCandidates()
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val holder = DynamicManagerHolder(
        state = DynamicManagerState(config, candidates, loading, busy, message),
        select = { candidate ->
            if (!busy) {
                scope.launch {
                    busy = true
                    val ok = setDynamicManagerFromApk(candidate.apkPath)
                    config = getDynamicManagerConfig()
                    busy = false
                    message = if (ok) {
                        "已切换管理器，请打开 ${candidate.label} 继续操作"
                    } else {
                        "切换失败，请查看日志"
                    }
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
                    message = "证书长度无效"
                } else {
                    scope.launch {
                        busy = true
                        val ok = setDynamicManagerManual(size, hash.trim())
                        config = getDynamicManagerConfig()
                        busy = false
                        message = if (ok) "已应用手动签名" else "应用失败，请检查输入"
                    }
                }
            }
        },
        clear = {
            if (!busy) {
                scope.launch {
                    busy = true
                    val ok = clearDynamicManager()
                    config = getDynamicManagerConfig()
                    busy = false
                    message = if (ok) "已清除动态签名，WoouSU 管理器恢复" else "清除失败"
                }
            }
        },
        dismissMessage = { message = null },
    )

    // mutate captured vars through recomposition-safe lambdas
    if (holder.state.message != null) {
        MessageDialog(holder.state.message) { holder.dismissMessage() }
    }

    return holder
}

@Composable
private fun MessageDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } },
        text = { Text(message) },
    )
}

@Composable
fun DynamicManagerScreen() {
    val navigator = LocalNavigator.current
    when (LocalUiMode.current) {
        UiMode.Miuix -> DynamicManagerScreenMiuix(onBack = { navigator.pop() })
        UiMode.Material -> DynamicManagerScreenMaterial(onBack = { navigator.pop() })
    }
}

@Composable
private fun CandidateRow(
    candidate: ManagerCandidate,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isActive, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = candidate.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "uid ${candidate.uid} · ${candidate.fingerprint}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isActive) {
            Text(text = "当前", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DynamicManagerContent(
    holder: DynamicManagerHolder,
    header: @Composable () -> Unit,
    container: @Composable (content: @Composable () -> Unit) -> Unit,
) {
    val state = holder.state
    val config = state.config
    var showManual by remember { mutableStateOf(false) }
    var manualSize by remember { mutableStateOf("") }
    var manualHash by remember { mutableStateOf("") }

    header()
    container {
        if (config != null && config.isEnabled) {
            Text(
                text = "当前已启用动态签名：0x%x / %s".format(config.size, config.hash),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = "警告：切换后 WoouSU 管理器将失去管理器身份与 root，请打开目标管理器操作；如目标应用无法使用，可在其终端执行 ksud dynamic-manager clear 恢复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Text(
                text = "未启用动态签名，当前使用 WoouSU 管理器。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }

        if (state.loading) {
            Text(
                text = "正在扫描已安装的管理器…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else if (state.candidates.isEmpty()) {
            Text(
                text = "未找到其它 KernelSU 系管理器（需包含 libksud.so）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            state.candidates.forEach { candidate ->
                CandidateRow(
                    candidate = candidate,
                    isActive = config != null && config.isEnabled &&
                            config.size == candidate.certSize && config.hash == candidate.certHash,
                    enabled = !state.busy,
                    onClick = { holder.select(candidate) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = { showManual = !showManual }, enabled = !state.busy) {
                Text(if (showManual) "收起手动设置" else "手动设置签名")
            }
            if (config != null && config.isEnabled) {
                TextButton(onClick = holder.clear, enabled = !state.busy) {
                    Text("清除动态签名")
                }
            }
        }

        if (showManual) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { holder.applyManual(manualSize, manualHash) },
                    enabled = !state.busy,
                ) { Text("应用") }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun DynamicManagerScreenMaterial(onBack: () -> Unit) {
    val holder = rememberDynamicManagerState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
                DynamicManagerContent(
                    holder = holder,
                    header = {},
                    container = { content ->
                        TonalCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Column { content() }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DynamicManagerScreenMiuix(onBack: () -> Unit) {
    val holder = rememberDynamicManagerState()
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
            .only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(bottom = 48.dp),
        ) {
            Card(
                colors = managerCardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val state = holder.state
                    val config = state.config
                    if (config != null && config.isEnabled) {
                        MiuixText(
                            text = "当前已启用动态签名：0x%x / %s".format(config.size, config.hash),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    } else {
                        MiuixText(
                            text = "未启用动态签名，当前使用 WoouSU 管理器。",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.loading) {
                        MiuixText(
                            text = "正在扫描已安装的管理器…",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    } else {
                        state.candidates.forEach { candidate ->
                            BasicCandidateRow(
                                candidate = candidate,
                                isActive = config != null && config.isEnabled &&
                                        config.size == candidate.certSize && config.hash == candidate.certHash,
                                enabled = !state.busy,
                                onClick = { holder.select(candidate) },
                            )
                        }
                        if (state.candidates.isEmpty()) {
                            MiuixText(
                                text = "未找到其它 KernelSU 系管理器（需包含 libksud.so）。",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MiuixText(
                        text = "切换后 WoouSU 管理器将失去管理器身份与 root；恢复可在目标管理器执行 ksud dynamic-manager clear。",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    if (config != null && config.isEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MiuixText(
                            text = "清除动态签名",
                            fontSize = 14.sp,
                            color = colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !state.busy) { holder.clear() }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicCandidateRow(
    candidate: ManagerCandidate,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !isActive, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        MiuixText(
            text = if (isActive) "${candidate.label}（当前）" else candidate.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        MiuixText(
            text = "${candidate.packageName} · uid ${candidate.uid}",
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
        MiuixText(
            text = candidate.fingerprint,
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}
