package me.weishu.kernelsu.ui.screen.kpm

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import me.weishu.kernelsu.ui.util.KpmModule
import me.weishu.kernelsu.ui.util.deleteKpmModule
import me.weishu.kernelsu.ui.util.installKpmModule
import me.weishu.kernelsu.ui.util.isKpmSupported
import me.weishu.kernelsu.ui.util.listKpmModules
import me.weishu.kernelsu.ui.util.loadKpmModule
import me.weishu.kernelsu.ui.util.unloadKpmModule
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.io.File

@Composable
fun KpmScreen() {
    val navigator = LocalNavigator.current
    when (LocalUiMode.current) {
        UiMode.Miuix -> KpmScreenMiuix(onBack = { navigator.pop() })
        UiMode.Material -> KpmScreenMaterial(onBack = { navigator.pop() })
    }
}

private data class KpmScreenState(
    val supported: Boolean,
    val modules: List<KpmModule>,
    val loading: Boolean,
)

@Composable
private fun rememberKpmState(): Pair<KpmScreenState, () -> Unit> {
    var supported by remember { mutableStateOf(false) }
    var modules by remember { mutableStateOf<List<KpmModule>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableStateOf(0) }
    LaunchedEffect(refreshTick) {
        loading = true
        supported = isKpmSupported()
        modules = if (supported) listKpmModules() else emptyList()
        loading = false
    }
    return KpmScreenState(supported, modules, loading) to { refreshTick++ }
}

@Composable
private fun rememberInstallLauncher(onInstalled: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    } ?: "module.kpm"
                    val staged = File(context.cacheDir, "kpm_install.kpm")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        staged.outputStream().use { output -> input.copyTo(output) }
                    }
                    installKpmModule(staged.absolutePath, name.substringBeforeLast('.'))
                }.getOrDefault(false)
            }
            Toast.makeText(
                context,
                if (ok) "已安装，返回列表后可加载" else "安装失败",
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) onInstalled()
        }
    }
    return { launcher.launch("*/*") }
}

@Composable
private fun KpmScreenMiuix(onBack: () -> Unit) {
    val (state, refresh) = rememberKpmState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val install = rememberInstallLauncher(onInstalled = refresh)
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        containerColor = managerScreenContainerColor(),
        topBar = {
            SmallTopAppBar(
                title = "KPM 内核模块",
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
                    MiuixText(text = "KernelPatch Module (KPM)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    MiuixText(
                        text = if (state.supported) {
                            "KPM 运行时已就绪，模块目录：/data/adb/kpm"
                        } else {
                            "当前内核未启用 KPM 支持（需要 CONFIG_KPM 与配套 ksud），仅可管理模块文件。"
                        },
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            if (state.modules.isEmpty()) {
                Card(
                    colors = managerCardColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    MiuixText(
                        text = if (state.loading) "加载中…" else "暂无已安装的 KPM 模块",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                state.modules.forEach { module ->
                    Card(
                        colors = managerCardColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    MiuixText(
                                        text = module.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    )
                                    MiuixText(
                                        text = listOfNotNull(
                                            module.version.takeIf { it.isNotEmpty() }?.let { "v$it" },
                                            module.author.takeIf { it.isNotEmpty() },
                                            if (module.loaded) "已加载" else "未加载",
                                        ).joinToString(" · "),
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                                TextButton(
                                    text = if (module.loaded) "卸载" else "加载",
                                    onClick = {
                                        scope.launch {
                                            val (ok, detail) = if (module.loaded) {
                                                unloadKpmModule(module) to ""
                                            } else {
                                                val r = loadKpmModule(module)
                                                r.ok to r.detail
                                            }
                                            Toast.makeText(
                                                context,
                                                if (ok) {
                                                    "操作成功"
                                                } else if (module.loaded) {
                                                    "卸载失败"
                                                } else {
                                                    "加载失败：" + detail.ifBlank { "内核可能不支持 KPM" }
                                                },
                                                Toast.LENGTH_LONG,
                                            ).show()
                                            refresh()
                                        }
                                    },
                                )
                                TextButton(
                                    text = "删除",
                                    onClick = {
                                        scope.launch {
                                            deleteKpmModule(module)
                                            refresh()
                                        }
                                    },
                                )
                            }
                            if (module.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                MiuixText(
                                    text = module.description,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            }
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
                Column(modifier = Modifier.padding(8.dp)) {
                    TextButton(
                        text = "安装 KPM 模块（.kpm）",
                        onClick = install,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun KpmScreenMaterial(onBack: () -> Unit) {
    val (state, refresh) = rememberKpmState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val install = rememberInstallLauncher(onInstalled = refresh)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("KPM 内核模块") },
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
                            text = "KernelPatch Module (KPM)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.supported) {
                                "KPM 运行时已就绪，模块目录：/data/adb/kpm"
                            } else {
                                "当前内核未启用 KPM 支持（需要 CONFIG_KPM 与配套 ksud），仅可管理模块文件。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.modules.isEmpty()) {
                item {
                    TonalCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text(
                            text = if (state.loading) "加载中…" else "暂无已安装的 KPM 模块",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(state.modules, key = { it.id }) { module ->
                    TonalCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = module.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = listOfNotNull(
                                    module.version.takeIf { it.isNotEmpty() }?.let { "v$it" },
                                    module.author.takeIf { it.isNotEmpty() },
                                    if (module.loaded) "已加载" else "未加载",
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (module.description.isNotEmpty()) {
                                Text(
                                    text = module.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        scope.launch {
                                            val (ok, detail) = if (module.loaded) {
                                                unloadKpmModule(module) to ""
                                            } else {
                                                val r = loadKpmModule(module)
                                                r.ok to r.detail
                                            }
                                            Toast.makeText(
                                                context,
                                                if (ok) {
                                                    "操作成功"
                                                } else if (module.loaded) {
                                                    "卸载失败"
                                                } else {
                                                    "加载失败：" + detail.ifBlank { "内核可能不支持 KPM" }
                                                },
                                                Toast.LENGTH_LONG,
                                            ).show()
                                            refresh()
                                        }
                                    },
                                ) { Text(if (module.loaded) "卸载" else "加载") }
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        scope.launch {
                                            deleteKpmModule(module)
                                            refresh()
                                        }
                                    },
                                ) { Text("删除") }
                            }
                        }
                    }
                }
            }
            item {
                TonalCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    androidx.compose.material3.TextButton(
                        onClick = install,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    ) { Text("安装 KPM 模块（.kpm）") }
                }
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}
