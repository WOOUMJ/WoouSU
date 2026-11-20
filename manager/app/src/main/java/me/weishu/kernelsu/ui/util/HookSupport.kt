package me.weishu.kernelsu.ui.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SUSFS_MODULE_DIR = "/data/adb/modules/susfs4ksu"
private const val SUSFS_PERSIST_DIR = "/data/adb/susfs4ksu"
private const val SUSFS_CONFIG = "$SUSFS_PERSIST_DIR/config.sh"
private const val SUSFS_ACTIVE = "/data/adb/ksu/susfs4ksu/logs/susfs_active"

private const val SUSFS_BIN_RESOLVE =
    "SUSFS_BIN=\$(command -v ksu_susfs 2>/dev/null || echo /data/adb/ksu/bin/ksu_susfs); "

data class HookInfo(
    val type: String,
    val version: String,
    val active: Boolean,
    val features: List<String>,
)

/** A single SUSFS switch persisted in the module config file. */
data class HookToggle(
    val key: String,
    val title: String,
    val summary: String,
)

val HOOK_TOGGLES: List<HookToggle> = listOf(
    HookToggle("susfs_log", "启用 SUSFS 日志", "输出内核日志，排查问题时使用"),
    HookToggle("spoof_uname", "伪造内核版本", "对非 root 进程隐藏真实内核版本"),
    HookToggle("hide_cusrom", "隐藏自定义 ROM 痕迹", "隐藏第三方 ROM 的特征文件"),
    HookToggle("hide_vendor_sepolicy", "隐藏 vendor sepolicy", "避免暴露自定义 sepolicy"),
    HookToggle("hide_compat_matrix", "隐藏兼容性矩阵", "隐藏 Treble 兼容矩阵信息"),
    HookToggle("hide_gapps", "隐藏 GApps", "对目标进程隐藏 Google 应用"),
    HookToggle("hide_revanced", "隐藏 ReVanced", "对目标进程隐藏修改版应用"),
    HookToggle("spoof_cmdline", "隐藏启动参数", "过滤 /proc/cmdline 中的敏感信息"),
    HookToggle("hide_loops", "隐藏 loop 设备", "对目标进程隐藏 loop 挂载"),
    HookToggle("force_hide_lsposed", "强制隐藏 LSPosed", "即使检测到也强制隐藏"),
    HookToggle("hide_sus_mnts_for_all_or_non_su_procs", "隐藏全部挂载", "对所有或非 root 进程隐藏挂载"),
    HookToggle("umount_for_zygote_iso_service", "卸载 Zygote 隔离服务", "对隔离进程卸载可疑挂载"),
    HookToggle("auto_try_umount", "自动尝试卸载", "启动时自动卸载可疑挂载点"),
    HookToggle("skip_legit_mounts", "跳过合法挂载", "避免误卸载系统正常挂载"),
    HookToggle("avc_log_spoofing", "AVC 日志伪装", "伪装 SELinux 拒绝日志"),
    HookToggle("emulate_vold_app_data", "模拟 vold app data", "模拟 vold 的应用数据挂载"),
)

fun runRoot(vararg commands: String): Shell.Result =
    getRootShell().newJob().add(*commands).to(ArrayList(), ArrayList()).exec()

fun runRootOut(vararg commands: String): List<String> = runRoot(*commands).out

suspend fun detectHook(): HookInfo? = withContext(Dispatchers.IO) {
    val version = runRootOut(
        "${SUSFS_BIN_RESOLVE}\$SUSFS_BIN show version 2>/dev/null"
    ).firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val moduleInstalled = runRootOut("[ -d $SUSFS_MODULE_DIR ] && echo yes").contains("yes")
    if (version.isEmpty() && !moduleInstalled) {
        null
    } else {
        val features = runRootOut("${SUSFS_BIN_RESOLVE}\$SUSFS_BIN show enabled_features 2>/dev/null")
            .flatMap { it.split(Regex("[,\\s]+")) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val active = runRootOut("[ -f $SUSFS_ACTIVE ] && echo yes").contains("yes")
        HookInfo(
            type = "SUSFS",
            version = version.ifEmpty { "未知版本" },
            active = active,
            features = features,
        )
    }
}

suspend fun readHookConfig(): Map<String, String> = withContext(Dispatchers.IO) {
    runRootOut("[ -f $SUSFS_CONFIG ] && cat $SUSFS_CONFIG")
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains('=')) {
                null
            } else {
                trimmed.substringBefore('=').trim() to
                    trimmed.substringAfter('=').trim().trim('\'', '"')
            }
        }
        .toMap()
}

suspend fun writeHookConfig(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
    val safeKey = key.filter { it.isLetterOrDigit() || it == '_' }
    val safeValue = value.filter { it.isLetterOrDigit() || it == '_' || it == '.' || it == '-' }
    if (safeKey.isEmpty()) return@withContext false
    val file = SUSFS_CONFIG
    val command = "mkdir -p $SUSFS_PERSIST_DIR; " +
        "touch $file; " +
        "if grep -q '^$safeKey=' $file; then " +
        "sed -i 's|^$safeKey=.*|$safeKey=$safeValue|' $file; " +
        "else echo '$safeKey=$safeValue' >> $file; fi"
    runRoot(command).isSuccess
}
