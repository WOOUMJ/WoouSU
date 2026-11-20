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

suspend fun loadKpmModule(module: KpmModule, args: String = ""): Boolean = withContext(Dispatchers.IO) {
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
    runRoot(command).isSuccess
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
