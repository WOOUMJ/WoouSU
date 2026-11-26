package me.weishu.kernelsu.ui.util

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.IKsuInterface
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.KsuService
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.resume

private val jsonParser = Json { ignoreUnknownKeys = true }

data class DynamicManagerConfig(
    val size: Int,
    val hash: String,
) {
    val isEnabled: Boolean get() = size > 0 && hash.isNotEmpty()
}

data class ManagerCandidate(
    val packageName: String,
    val label: String,
    val uid: Int,
    val apkPath: String,
    val certSize: Int,
    val certHash: String,
) {
    val fingerprint: String get() = "0x%x / %s".format(certSize, certHash)
}

private suspend fun execKsudCapture(args: String): String = withContext(Dispatchers.IO) {
    val out = getRootShell().newJob()
        .add("${getKsudPath()} $args")
        .to(ArrayList<String>(), null)
        .exec().out
    out.joinToString("\n")
}

suspend fun getDynamicManagerConfig(): DynamicManagerConfig? = withContext(Dispatchers.IO) {
    val raw = execKsudCapture("dynamic-manager get --json").trim()
    runCatching {
        val obj = jsonParser.parseToJsonElement(raw).jsonObject
        DynamicManagerConfig(
            size = obj["size"]?.jsonPrimitive?.longOrNull?.toInt() ?: 0,
            hash = obj["hash"]?.jsonPrimitive?.content.orEmpty(),
        )
    }.getOrNull()
}

suspend fun setDynamicManagerFromApk(apkPath: String): Boolean = withContext(Dispatchers.IO) {
    val escaped = apkPath.replace("'", "'\\''")
    execKsud("dynamic-manager set-apk '$escaped'", true)
}

suspend fun setDynamicManagerManual(size: Int, hash: String): Boolean = withContext(Dispatchers.IO) {
    execKsud("dynamic-manager set $size '$hash'", true)
}

suspend fun clearDynamicManager(): Boolean = withContext(Dispatchers.IO) {
    execKsud("dynamic-manager clear", true)
}

private fun extractCert(pkg: PackageInfo): Pair<Int, String>? {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pkg.signingInfo ?: return null
        if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
    } else {
        @Suppress("DEPRECATION")
        pkg.signatures
    } ?: return null

    val der = signatures.firstOrNull()?.toByteArray() ?: return null
    val digest = MessageDigest.getInstance("SHA-256").digest(der)
    return der.size to digest.joinToString("") { "%02x".format(it) }
}

private suspend fun connectKsuService(): Pair<IBinder, ServiceConnection> = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { cont ->
        val connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) = Unit

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (cont.isActive && binder != null) {
                    cont.resume(binder to this)
                }
            }
        }

        cont.invokeOnCancellation {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                RootService.unbind(connection)
            } else {
                Handler(Looper.getMainLooper()).post { RootService.unbind(connection) }
            }
        }

        val intent = Intent(ksuApp, KsuService::class.java)
        RootService.bindOrTask(intent, Shell.EXECUTOR, connection)?.let { task ->
            KsuCli.SHELL.execTask(task)
        }
    }
}

/**
 * Lists installed apps that bundle libksud.so, i.e. other KernelSU-family managers.
 */
suspend fun listManagerCandidates(): List<ManagerCandidate> = withContext(Dispatchers.IO) {
    runCatching {
        val pm = ksuApp.packageManager
        val (binder, connection) = connectKsuService()
        try {
            val iface = IKsuInterface.Stub.asInterface(binder)
            val packages = iface.getPackages(PackageManager.GET_SIGNING_CERTIFICATES).list
            val seen = HashSet<String>()
            packages.mapNotNull { pkg ->
                val ai = pkg.applicationInfo ?: return@mapNotNull null
                if (pkg.packageName == BuildConfig.APPLICATION_ID) return@mapNotNull null
                if (!seen.add(pkg.packageName)) return@mapNotNull null
                val libDir = ai.nativeLibraryDir ?: return@mapNotNull null
                if (!File(libDir, "libksud.so").exists()) return@mapNotNull null
                val (size, hash) = extractCert(pkg) ?: return@mapNotNull null
                ManagerCandidate(
                    packageName = pkg.packageName,
                    label = runCatching { ai.loadLabel(pm).toString() }.getOrDefault(pkg.packageName),
                    uid = ai.uid,
                    apkPath = ai.sourceDir,
                    certSize = size,
                    certHash = hash,
                )
            }.sortedBy { it.label.lowercase() }
        } finally {
            withContext(Dispatchers.Main) { RootService.unbind(connection) }
        }
    }.getOrDefault(emptyList())
}
