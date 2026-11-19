package me.weishu.kernelsu.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.topjohnwu.superuser.ShellUtils
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.magica.BootCompletedReceiver
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.screen.modulerepo.RepoSort
import me.weishu.kernelsu.ui.util.execKsud
import me.weishu.kernelsu.ui.util.getFeaturePersistValue
import me.weishu.kernelsu.ui.util.getFeatureValue
import me.weishu.kernelsu.ui.util.getFeatureStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val SETTINGS_PREFS = "settings"
private const val KEY_USE_SOFT_REBOOT = "soft_reboot"

/** Prefer soft reboot: always in jailbreak mode, or when the setting is enabled. */
fun isSoftRebootPreferred(): Boolean =
    Natives.isLateLoadMode || ksuApp.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_USE_SOFT_REBOOT, false)

class SettingsRepositoryImpl : SettingsRepository {

    private val prefs by lazy {
        ksuApp.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    }

    override var uiMode: String
        get() = prefs.getString("ui_mode", UiMode.DEFAULT_VALUE) ?: UiMode.DEFAULT_VALUE
        set(value) = prefs.edit { putString("ui_mode", value) }

    override var checkUpdate: Boolean
        get() = prefs.getBoolean("check_update", true)
        set(value) = prefs.edit { putBoolean("check_update", value) }

    override var checkModuleUpdate: Boolean
        get() = prefs.getBoolean("module_check_update", true)
        set(value) = prefs.edit { putBoolean("module_check_update", value) }

    override var themeMode: Int
        get() = prefs.getInt("color_mode", 0)
        set(value) = prefs.edit { putInt("color_mode", value) }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean("miuix_monet", false)
        set(value) = prefs.edit { putBoolean("miuix_monet", value) }

    override var keyColor: Int
        get() = prefs.getInt("key_color", 0)
        set(value) = prefs.edit { putInt("key_color", value) }

    override var colorStyle: String
        get() = prefs.getString("color_style", PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        set(value) = prefs.edit { putString("color_style", value) }

    override var colorSpec: String
        get() = prefs.getString("color_spec", ColorSpec.SpecVersion.SPEC_2025.name) ?: ColorSpec.SpecVersion.SPEC_2025.name
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var enableSwipeDismiss: Boolean
        get() = prefs.getBoolean("enable_swipe_dismiss", true)
        set(value) = prefs.edit { putBoolean("enable_swipe_dismiss", value) }

    override var pagerInterceptionMode: Int
        get() = prefs.getInt("pager_interception_mode", 1)
        set(value) = prefs.edit { putInt("pager_interception_mode", value.coerceIn(0, 2)) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var enableNavigationBadge: Boolean
        get() = prefs.getBoolean("enable_navigation_badge", true)
        set(value) = prefs.edit { putBoolean("enable_navigation_badge", value) }

    override var navigationRailExpanded: Boolean
        get() = prefs.getBoolean("nav_rail_expanded", false)
        set(value) = prefs.edit { putBoolean("nav_rail_expanded", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }

    override var moduleDescriptionMaxLines: Int
        get() = prefs.getInt("module_description_max_lines", 4)
        set(value) = prefs.edit { putInt("module_description_max_lines", value) }

    override var enableWebDebugging: Boolean
        get() = prefs.getBoolean("enable_web_debugging", false)
        set(value) = prefs.edit { putBoolean("enable_web_debugging", value) }

    override var moduleSortEnabledFirst: Boolean
        get() = prefs.getBoolean("module_sort_enabled_first", false)
        set(value) = prefs.edit { putBoolean("module_sort_enabled_first", value) }

    override var moduleSortActionFirst: Boolean
        get() = prefs.getBoolean("module_sort_action_first", false)
        set(value) = prefs.edit { putBoolean("module_sort_action_first", value) }

    override var moduleRepoSortOrder: String
        get() = RepoSort.fromStored(
            name = prefs.getString("module_repo_sort", null),
            legacyOrdinal = if (prefs.contains("module_repo_sort_order")) prefs.getInt("module_repo_sort_order", 0) else null,
        ).name
        set(value) = prefs.edit {
            putString("module_repo_sort", value)
            remove("module_repo_sort_order")
        }

    override var superuserShowSystemApps: Boolean
        get() = prefs.getBoolean("show_system_apps", false)
        set(value) = prefs.edit { putBoolean("show_system_apps", value) }

    override var superuserShowOnlyPrimaryUserApps: Boolean
        get() = prefs.getBoolean("show_only_primary_user_apps", false)
        set(value) = prefs.edit { putBoolean("show_only_primary_user_apps", value) }

    override var superuserSortOption: Int
        get() = prefs.getInt("superuser_sort_option", 0)
        set(value) = prefs.edit { putInt("superuser_sort_option", value) }

    override var suLogFilters: Set<String>?
        get() = prefs.getStringSet("sulog_filters", null)?.toSet()
        set(filters) = prefs.edit { putStringSet("sulog_filters", filters) }

    override var autoJailbreak: Boolean
        get() = prefs.getBoolean("auto_jailbreak", false)
        set(value) {
            runCatching {
                ksuApp.packageManager.setComponentEnabledSetting(
                    ComponentName(ksuApp, BootCompletedReceiver::class.java),
                    if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }.onFailure {
                Log.e("Settings", "failed to change boot receiver state to $value", it)
            }
            prefs.edit {
                putBoolean("auto_jailbreak", value)
            }
        }

    override var useSoftReboot: Boolean
        get() = prefs.getBoolean(KEY_USE_SOFT_REBOOT, false)
        set(value) = prefs.edit { putBoolean(KEY_USE_SOFT_REBOOT, value) }

    override var hideVersionMismatch: Boolean
        get() = prefs.getBoolean("hide_version_mismatch", false)
        set(value) = prefs.edit { putBoolean("hide_version_mismatch", value) }

    override var backgroundImagePath: String
        get() = prefs.getString("background_image_path", "") ?: ""
        set(value) = prefs.edit { putString("background_image_path", value) }

    override var backgroundOpacity: Float
        get() = prefs.getFloat("background_opacity", 0.7f).coerceIn(0.05f, 1f)
        set(value) = prefs.edit { putFloat("background_opacity", value.coerceIn(0.05f, 1f)) }

    override var uiOpacity: Float
        get() = prefs.getFloat("ui_opacity", 0.5f).coerceIn(0.05f, 1f)
        set(value) = prefs.edit { putFloat("ui_opacity", value.coerceIn(0.05f, 1f)) }

    override var statusText: String
        get() = prefs.getString("status_text", "") ?: ""
        set(value) = prefs.edit { putString("status_text", value) }

    override var statusImagePath: String
        get() = prefs.getString("status_image_path", "") ?: ""
        set(value) = prefs.edit { putString("status_image_path", value) }

    override val intentToken: String
        get() {
        val existing = prefs.getString(INTENT_TOKEN_KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val token = ByteArray(32).also(secureRandom::nextBytes)
            .joinToString(separator = "") { "%02x".format(it) }
        prefs.edit { putString(INTENT_TOKEN_KEY, token) }
        return token
    }

    override suspend fun getSuCompatStatus(): String = getFeatureStatus("su_compat")

    override suspend fun getSuCompatPersistValue(): Long? = getFeaturePersistValue("su_compat")

    override fun isSuEnabled(): Boolean = Natives.isSuEnabled()

    override fun setSuEnabled(enabled: Boolean): Boolean = Natives.setSuEnabled(enabled)

    override fun setSuCompatModePref(mode: Int) = prefs.edit { putInt("su_compat_mode", mode) }

    override fun getSuCompatModePref(): Int = prefs.getInt("su_compat_mode", 0)

    override suspend fun getKernelUmountStatus(): String = getFeatureStatus("kernel_umount")

    override fun isKernelUmountEnabled(): Boolean = Natives.isKernelUmountEnabled()

    override fun setKernelUmountEnabled(enabled: Boolean): Boolean = Natives.setKernelUmountEnabled(enabled)

    override suspend fun getSelinuxHideStatus(): String = getFeatureStatus("selinux_hide")

    override fun isSelinuxHideEnabled(): Boolean = Natives.isSelinuxHideEnabled()

    override fun setSelinuxHideEnabled(enabled: Boolean): Int = Natives.setSelinuxHideEnabled(enabled)

    override suspend fun getSulogStatus(): String = getFeatureStatus("sulog")

    override suspend fun getSulogPersistValue(): Long? = getFeaturePersistValue("sulog")

    override suspend fun getSulogValue(): Long? = getFeatureValue("sulog")

    override fun setSulogEnabled(enabled: Boolean): Boolean = execKsud("feature set sulog ${if (enabled) 1 else 0}", true)

    override suspend fun getAdbRootStatus(): String = getFeatureStatus("adb_root")

    override suspend fun getAdbRootPersistValue(): Long? = getFeaturePersistValue("adb_root")

    override suspend fun getAdbRootValue(): Long? = getFeatureValue("adb_root")

    override fun setAdbRootEnabled(enabled: Boolean): Boolean =
        if (execKsud("feature set adb_root ${if (enabled) 1 else 0}", true)) {
            ShellUtils.fastCmd("setprop ctl.restart adbd")
            true
        } else {
            false
        }

    override suspend fun getShellSuAdbOnlyStatus(): String = getFeatureStatus("shell_su_adb_only")

    override suspend fun getShellSuAdbOnlyPersistValue(): Long? = getFeaturePersistValue("shell_su_adb_only")

    override suspend fun getShellSuAdbOnlyValue(): Long? = getFeatureValue("shell_su_adb_only")

    override fun setShellSuAdbOnlyEnabled(enabled: Boolean): Boolean =
        execKsud("feature set shell_su_adb_only ${if (enabled) 1 else 0}", true)

    override fun isDefaultUmountModules(): Boolean = Natives.isDefaultUmountModules()

    override fun setDefaultUmountModules(enabled: Boolean): Boolean = Natives.setDefaultUmountModules(enabled)

    override fun isLkmMode(): Boolean = Natives.isLkmMode

    override fun execKsudFeatureSave() {
        execKsud("feature save", true)
    }

    override fun exportConfig(uri: Uri): Boolean = runCatching {
        val root = JSONObject()
        val entries = JSONObject()
        for ((key, value) in prefs.all) {
            if (key == INTENT_TOKEN_KEY) continue
            when (value) {
                is Boolean -> entries.put(key, JSONObject().put("type", "b").put("value", value))
                is Float -> entries.put(key, JSONObject().put("type", "f").put("value", value.toDouble()))
                is Int -> entries.put(key, JSONObject().put("type", "i").put("value", value))
                is Long -> entries.put(key, JSONObject().put("type", "l").put("value", value))
                is String -> entries.put(key, JSONObject().put("type", "s").put("value", value))
                is Set<*> -> entries.put(
                    key,
                    JSONObject().put("type", "ss").put("value", JSONArray(value.map { it.toString() }))
                )
                else -> {}
            }
        }
        root.put("version", 1).put("entries", entries)

        val images = JSONObject()
        fun packImage(name: String, path: String) {
            val file = File(path)
            if (path.isNotEmpty() && file.isFile) {
                images.put(name, file.name)
            }
        }
        packImage("background", backgroundImagePath)
        packImage("status", statusImagePath)
        root.put("images", images)

        ksuApp.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                zip.putNextEntry(ZipEntry(CONFIG_JSON_ENTRY))
                zip.write(root.toString().toByteArray())
                zip.closeEntry()
                fun writeImage(name: String, path: String) {
                    val file = File(path)
                    if (path.isNotEmpty() && file.isFile) {
                        zip.putNextEntry(ZipEntry("$CONFIG_IMAGE_DIR/$name/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
                writeImage("background", backgroundImagePath)
                writeImage("status", statusImagePath)
            }
        } ?: return false
        true
    }.getOrElse {
        Log.e("Settings", "export config failed", it)
        false
    }

    override fun importConfig(uri: Uri): Boolean = runCatching {
        var rootJson: JSONObject? = null
        val imageFiles = mutableMapOf<String, File>()
        ksuApp.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == CONFIG_JSON_ENTRY -> {
                            rootJson = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                        }
                        name.startsWith("$CONFIG_IMAGE_DIR/") -> {
                            val parts = name.substringAfter("$CONFIG_IMAGE_DIR/").split('/', limit = 2)
                            if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                                val target = File(File(ksuApp.filesDir, CONFIG_IMAGE_DIR), "import_${parts[0]}_${System.currentTimeMillis()}_${parts[1]}")
                                target.parentFile?.mkdirs()
                                target.outputStream().use { zip.copyTo(it) }
                                imageFiles[parts[0]] = target
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } ?: return false
        val root = rootJson ?: return false

        val entries = root.optJSONObject("entries") ?: return false
        prefs.edit {
            for (key in prefs.all.keys) {
                if (key != INTENT_TOKEN_KEY) remove(key)
            }
            for (key in entries.keys()) {
                val e = entries.optJSONObject(key) ?: continue
                when (e.optString("type")) {
                    "b" -> putBoolean(key, e.optBoolean("value"))
                    "f" -> putFloat(key, e.optDouble("value", 0.0).toFloat())
                    "i" -> putInt(key, e.optInt("value"))
                    "l" -> putLong(key, e.optLong("value"))
                    "s" -> putString(key, e.optString("value"))
                    "ss" -> {
                        val arr = e.optJSONArray("value") ?: continue
                        putStringSet(key, List(arr.length()) { arr.optString(it) }.toSet())
                    }
                }
            }
            imageFiles["background"]?.let { putString("background_image_path", it.absolutePath) }
            imageFiles["status"]?.let { putString("status_image_path", it.absolutePath) }
        }
        true
    }.getOrElse {
        Log.e("Settings", "import config failed", it)
        false
    }

    private companion object {
        private const val INTENT_TOKEN_KEY = "intent_token"
        private const val CONFIG_JSON_ENTRY = "woousu_config.json"
        private const val CONFIG_IMAGE_DIR = "images"
        private val secureRandom = SecureRandom()
    }
}
