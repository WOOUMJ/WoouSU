package me.weishu.kernelsu.ui.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val KPM_DIR = "/data/adb/kpm"

data class KpmModule(
    val id: String,
    val path: String,
    val loaded: Boolean,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
)

private fun kpmCommand(): String = "\"${getKsudPath()}\" kpm"

suspend fun isKpmSupported(): Boolean = withContext(Dispatchers.IO) {
    runRoot("${kpmCommand()} version 2>/dev/null").isSuccess
}

suspend fun listKpmModules(): List<KpmModule> = withContext(Dispatchers.IO) {
    val loaded = runRootOut("${kpmCommand()} list 2>/dev/null")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
    val files = runRootOut("ls -1 $KPM_DIR 2>/dev/null")
        .map { it.trim() }
        .filter { it.endsWith(".kpm") }
    val modules = files.map { file ->
        val id = file.removeSuffix(".kpm")
        val props = runRootOut("${kpmCommand()} info \"$id\" 2>/dev/null")
            .mapNotNull { line ->
                val index = line.indexOf(':')
                if (index <= 0) null else line.substring(0, index).trim() to line.substring(index + 1).trim()
            }
            .toMap()
        KpmModule(
            id = id,
            path = "$KPM_DIR/$file",
            loaded = loaded.contains(id),
            name = props["name"] ?: id,
            version = props["version"].orEmpty(),
            author = props["author"].orEmpty(),
            description = props["description"].orEmpty(),
        )
    }
    modules.sortedBy { it.name.lowercase() }
}

data class KpmActionResult(val ok: Boolean, val detail: String)

private fun friendlyKpmError(raw: String): String {
    val tail = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.lastOrNull().orEmpty()
    return when {
        raw.contains("Exec format error", true) ->
            "模块与当前内核不匹配（可能不是有效的 KPM，或内核版本不同）"
        raw.contains("No such file", true) -> "模块文件不存在"
        raw.contains("File exists", true) -> "模块已加载"
        raw.contains("module init failed", true) ->
            "模块初始化失败（可能与当前内核不兼容）"
        raw.contains("apply relocations", true) ->
            "模块符号解析失败（内核符号不匹配）"
        raw.contains("allocate executable", true) ->
            "内核无法分配可执行内存（可能受内核限制）"
        raw.contains("Operation not supported", true) ->
            "内核未启用 KPM，或 KernelPatch 版本不匹配"
        raw.contains("Permission denied", true) -> "权限不足"
        tail.isNotEmpty() -> tail
        else -> "未知错误"
    }
}

suspend fun loadKpmModule(module: KpmModule, args: String = ""): KpmActionResult = withContext(Dispatchers.IO) {
    val command = buildString {
        append(kpmCommand())
        append(" load \"")
        append(module.path)
        append('"')
        if (args.isNotBlank()) {
            append(' ')
            append(args)
        }
    }
    val result = runRoot(command)
    if (result.isSuccess) {
        KpmActionResult(true, "")
    } else {
        val raw = (result.err + result.out).joinToString("\n")
        val friendly = friendlyKpmError(raw)
        val klog = runRootOut("dmesg 2>/dev/null | grep -iE 'kpm|module' | tail -1")
            .firstOrNull { it.isNotBlank() }
            ?.trim()
        val detail = if (klog.isNullOrEmpty()) friendly else "$friendly\n内核日志：${klog.take(120)}"
        KpmActionResult(false, detail)
    }
}

suspend fun unloadKpmModule(module: KpmModule): Boolean = withContext(Dispatchers.IO) {
    runRoot("${kpmCommand()} unload \"${module.id}\"").isSuccess
}

suspend fun deleteKpmModule(module: KpmModule): Boolean = withContext(Dispatchers.IO) {
    if (module.loaded) unloadKpmModule(module)
    runRoot("rm -f \"${module.path}\"").isSuccess
}

suspend fun installKpmModule(stagedPath: String, id: String): Boolean = withContext(Dispatchers.IO) {
    val safeId = id.filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }
    if (safeId.isEmpty()) return@withContext false
    val target = "$KPM_DIR/$safeId.kpm"
    runRoot("mkdir -p $KPM_DIR; cp -f \"$stagedPath\" \"$target\"; chmod 644 \"$target\"").isSuccess
}
