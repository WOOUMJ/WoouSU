package me.weishu.kernelsu.ui.screen.hook

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
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import me.weishu.kernelsu.ui.component.miuix.ManagerSwitchPreference
import me.weishu.kernelsu.ui.component.miuix.managerCardColors
import me.weishu.kernelsu.ui.component.miuix.managerScreenBarColor
import me.weishu.kernelsu.ui.component.miuix.managerScreenContainerColor
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.util.HOOK_TOGGLES
import me.weishu.kernelsu.ui.util.HookInfo
import me.weishu.kernelsu.ui.util.detectHook
import me.weishu.kernelsu.ui.util.readHookConfig
import me.weishu.kernelsu.ui.util.writeHookConfig
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

@Composable
fun HookScreen() {
    val navigator = LocalNavigator.current
    when (LocalUiMode.current) {
        UiMode.Miuix -> HookScreenMiuix(onBack = { navigator.pop() })
        UiMode.Material -> HookScreenMaterial(onBack = { navigator.pop() })
    }
}

private data class HookScreenState(
    val info: HookInfo?,
    val config: Map<String, String>,
    val loading: Boolean,
)

private class HookStateHolder(
    val state: HookScreenState,
    val setToggle: (String, Boolean) -> Unit,
)

@Composable
private fun rememberHookState(): HookStateHolder {
    var state by remember { mutableStateOf(HookScreenState(null, emptyMap(), true)) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val info = detectHook()
        val config = readHookConfig()
        state = HookScreenState(info, config, false)
    }
    return HookStateHolder(
        state = state,
        setToggle = { key, enabled ->
            // 先更新本地状态，让开关立即响应并播放动画，再异步落盘
            state = state.copy(config = state.config + (key to if (enabled) "1" else "0"))
            scope.launch {
                writeHookConfig(key, if (enabled) "1" else "0")
            }
        },
    )
}

@Composable
private fun HookScreenMiuix(onBack: () -> Unit) {
    val holder = rememberHookState()
    val state = holder.state
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        containerColor = managerScreenContainerColor(),
        topBar = {
            SmallTopAppBar(
                title = "钩子设置",
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
                Column(modifier = Modifier.padding(16.dp)) {
                    MiuixText(text = "钩子类型", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    MiuixText(
                        text = state.info?.let { "${it.type}  ${it.version}" } ?: "未检测到钩子",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    if (state.info != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        MiuixText(
                            text = if (state.info.active) "状态：运行中" else "状态：未激活",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            Card(
                colors = managerCardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MiuixText(text = "已启用特性", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val features = state.info?.features.orEmpty()
                    if (features.isEmpty()) {
                        MiuixText(
                            text = "未获取到特性列表",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    } else {
                        features.forEach { feature ->
                            MiuixText(
                                text = "· $feature",
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            Card(
                colors = managerCardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Column {
                    HOOK_TOGGLES.forEach { toggle ->
                        val checked = state.config[toggle.key]?.let { it == "1" || it == "true" } ?: false
                        ManagerSwitchPreference(
                            title = toggle.title,
                            summary = toggle.summary,
                            checked = checked,
                            onCheckedChange = { enabled ->
                                holder.setToggle(toggle.key, enabled)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HookScreenMaterial(onBack: () -> Unit) {
    val holder = rememberHookState()
    val state = holder.state
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("钩子设置") },
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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
        ) {
            item {
                TonalCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "钩子类型",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.info?.let { "${it.type}  ${it.version}" } ?: "未检测到钩子",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (state.info != null) {
                            Text(
                                text = if (state.info.active) "状态：运行中" else "状态：未激活",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                TonalCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "已启用特性",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val features = state.info?.features.orEmpty()
                        if (features.isEmpty()) {
                            Text(
                                text = "未获取到特性列表",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            features.forEach { feature ->
                                Text(
                                    text = "· $feature",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
            items(HOOK_TOGGLES.size) { index ->
                val toggle = HOOK_TOGGLES[index]
                val checked = state.config[toggle.key]?.let { it == "1" || it == "true" } ?: false
                TonalCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = toggle.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = toggle.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { enabled ->
                                holder.setToggle(toggle.key, enabled)
                            },
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}
