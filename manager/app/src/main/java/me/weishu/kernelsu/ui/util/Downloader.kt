package me.weishu.kernelsu.ui.util

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.data.registry.ExpectedFile
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo
import okhttp3.Request

/**
 * @author weishu
 * @date 2023/6/22.
 */
suspend fun download(
    url: String,
    fileName: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {},
    onProgress: (Int) -> Unit = {},
    expected: ExpectedFile? = null,
    onFailed: (DownloadManager.DownloadState) -> Unit = {},
) {
    onDownloading()

    val downloadId = DownloadManager.enqueue(
        context = ksuApp,
        url = url,
        fileName = fileName,
        onCompleted = onDownloaded,
        expected = expected,
    )

    val finalState = DownloadManager.downloads
        .onEach { map -> map[downloadId]?.let { onProgress(it.progress) } }
        .first { map ->
            val status = map[downloadId]?.status
            status == DownloadManager.Status.COMPLETED ||
                status == DownloadManager.Status.FAILED
        }[downloadId]
    if (finalState?.status == DownloadManager.Status.FAILED) {
        withContext(Dispatchers.Main) { onFailed(finalState) }
    }
}

internal suspend fun isDownloadAvailable(uri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        ksuApp.contentResolver.openFileDescriptor(uri, "r").use { it != null }
    }.getOrDefault(false)
}

fun checkNewVersion(): LatestVersionInfo {
    if (!isNetworkAvailable(ksuApp)) return LatestVersionInfo()
    val url = "https://api.github.com/repos/WOOUMJ/WoouSU/releases/latest"
    // default null value if failed
    val defaultValue = LatestVersionInfo()
    runCatching {
        ksuApp.okhttpClient.newCall(
            Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .build()
        ).execute()
            .use { response ->
                if (!response.isSuccessful) {
                    return defaultValue
                }
                val body = response.body.string()
                val json = org.json.JSONObject(body)
                val changelog = json.optString("body")
                val releaseUrl = json.optString("html_url")

                var apkUrl: String? = null
                var apkVersion: Int? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (!name.endsWith(".apk", ignoreCase = true)) {
                            continue
                        }
                        apkUrl = asset.optString("browser_download_url")
                        apkVersion = Regex("\\d+").findAll(name).lastOrNull()?.value?.toIntOrNull()
                        break
                    }
                }

                // Prefer the version encoded in the tag (e.g. v30003), fall back to the APK name.
                val tagVersion = Regex("\\d+")
                    .find(json.optString("tag_name"))
                    ?.value
                    ?.toIntOrNull()
                val versionCode = tagVersion ?: apkVersion ?: return defaultValue
                val downloadUrl = apkUrl ?: releaseUrl
                if (downloadUrl.isEmpty()) {
                    return defaultValue
                }

                return LatestVersionInfo(
                    versionCode,
                    downloadUrl,
                    changelog
                )
            }
    }
    return defaultValue
}
