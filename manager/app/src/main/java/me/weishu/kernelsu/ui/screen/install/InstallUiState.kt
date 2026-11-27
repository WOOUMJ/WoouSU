package me.weishu.kernelsu.ui.screen.install

import android.net.Uri
import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.util.LkmSelection

/** Where the KernelPatch (KPM) kernel patch is applied. */
enum class KpmTargetMode {
    /** Patch the kernel inside the image currently being patched. */
    FollowPatchedKernel,

    /** Only patch a separately selected boot image. */
    SeparateBoot,
}

@Immutable
internal data class InstallUiState(
    val installMethod: InstallMethod?,
    val lkmSelection: LkmSelection,
    val partitionSelectionIndex: Int,
    val displayPartitions: List<String>,
    val remotePartitionSelectionIndex: Int,
    val remoteDisplayPartitions: List<String>,
    val currentKmi: String,
    val slotSuffix: String,
    val installMethodOptions: List<InstallMethod>,
    val canSelectPartition: Boolean,
    val advancedOptionsShown: Boolean,
    val allowShell: Boolean,
    val enableAdb: Boolean,
    val forceBackup: Boolean,
    val canForceBackup: Boolean,
    val enableKpm: Boolean,
    val kpmTargetMode: KpmTargetMode,
    val kpmBootUri: Uri?,
)

@Immutable
internal data class InstallScreenActions(
    val onBack: () -> Unit,
    val onSelectMethod: (InstallMethod) -> Unit,
    val onDownloadFile: () -> Unit,
    val onSelectBootImage: () -> Unit,
    val onUploadLkm: () -> Unit,
    val onClearLkm: () -> Unit,
    val onSelectPartition: (Int) -> Unit,
    val onNext: () -> Unit,
    val onAdvancedOptionsClicked: () -> Unit,
    val onSelectAllowShell: (Boolean) -> Unit,
    val onSelectEnableAdb: (Boolean) -> Unit,
    val onSelectForceBackup: (Boolean) -> Unit,
    val onSelectEnableKpm: (Boolean) -> Unit,
    val onSelectKpmTargetMode: (KpmTargetMode) -> Unit,
    val onSelectKpmBoot: () -> Unit,
    val onClearKpmBoot: () -> Unit,
)
