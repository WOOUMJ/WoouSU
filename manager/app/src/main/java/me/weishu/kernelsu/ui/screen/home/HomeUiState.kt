package me.weishu.kernelsu.ui.screen.home

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo

@Immutable
data class HomeUiState(
    val kernelVersion: KernelVersion,
    val ksuVersion: Int?,
    val managerUAPIVersion: Int,
    val kernelUAPIVersion: Int?,
    val lkmMode: Boolean?,
    val isLkmBundled: Boolean,
    val isManager: Boolean,
    val isManagerPrBuild: Boolean,
    val isKernelPrBuild: Boolean,
    val requiresNewKernel: Boolean,
    val requiresNewManager: Boolean,
    val isRootAvailable: Boolean,
    val isSafeMode: Boolean,
    val isLateLoadMode: Boolean,
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentManagerVersionCode: Long,
    val systemInfo: SystemInfo,
    val hideVersionMismatch: Boolean = false,
    val backgroundImagePath: String = "",
    val backgroundOpacity: Float = 0.35f,
) {
    val isSELinuxPermissive: Boolean
        get() = systemInfo.selinuxStatus == "Permissive"

    val showLkmUpdate: Boolean
        get() {
            // 管理器版本号会随界面更新上涨，但内嵌 LKM 只有重新编译才会变，
            // 因此不能拿管理器版本号当“内嵌 LKM 版本”，否则每次界面更新都会误报。
            val driverVersion = ksuVersion ?: return false
            val bundledLkmVersion = BuildConfig.BUNDLED_LKM_VERSION_CODE
            return isManager &&
                    lkmMode == true &&
                    isLkmBundled &&
                    bundledLkmVersion > 0 &&
                    driverVersion < bundledLkmVersion &&
                    !requiresNewKernel &&
                    !requiresNewManager
        }

    // Jailbreak mode runs on locked bootloaders, so flashing a boot image would brick the device.
    val canInstallKernelUpdate: Boolean
        get() = lkmMode == true && !isLateLoadMode

    val showCustomLkmBadge: Boolean
        get() = lkmMode == true && !isLkmBundled

    val showRootWarning: Boolean
        get() = ksuVersion != null && !isRootAvailable

    val showManagerPrBuildWarning: Boolean
        get() = isManager && isManagerPrBuild

    val showKernelPrBuildWarning: Boolean
        get() = isManager && !isManagerPrBuild && isKernelPrBuild

    val showVersionMismatch: Boolean
        get() = isManager &&
                ksuVersion != null &&
                ksuVersion.toLong() != currentManagerVersionCode &&
                // LKM 模式下内核版本由内嵌 LKM 决定，是否可更新已由 showLkmUpdate 判断
                lkmMode != true &&
                !showLkmUpdate

    val hasUpdate: Boolean
        get() = latestVersionInfo.versionCode > currentManagerVersionCode
}

@Immutable
data class HomeActions(
    val onInstallClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onJailbreakClick: () -> Unit = {},
)
