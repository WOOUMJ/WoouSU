package me.weishu.kernelsu.ui.screen.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.magica.MagicaService
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    var hasActivated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage && !hasActivated) {
            hasActivated = true
            viewModel.refresh()
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            viewModel.refresh()
        }
        initialResumeHandled.value = true
        onPauseOrDispose { }
    }

    val actions = HomeActions(
        onInstallClick = { navigator.push(Route.Install) },
        onOpenUrl = uriHandler::openUri,
        onJailbreakClick = {
            loadingDialog.showLoading()
            context.startService(Intent(context, MagicaService::class.java))
            // Manager will be force-stopped and restarted by late-load on success.
            // If that doesn't happen within timeout, jailbreak likely failed.
            scope.launch(Dispatchers.IO) {
                delay(30_000.milliseconds)
                withContext(Dispatchers.Main) {
                    loadingDialog.hide()
                    Toast.makeText(context, R.string.jailbreak_timeout, Toast.LENGTH_LONG).show()
                }
            }
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> HomePagerMaterial(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}

internal enum class CharityTarget { Tencent, Alipay }

private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
private const val ALIPAY_CHARITY_URL = "https://ur.alipay.com/_4TE10qwJXLbURkQGPK4x31"
private const val TENCENT_CHARITY_URL = "https://gongyi.qq.com/"

private fun isPackageInstalled(context: Context, packageName: String): Boolean =
    runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess

private fun openUri(context: Context, uri: String): Boolean = runCatching {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}.isSuccess

private fun openUriInPackage(context: Context, uri: String, packageName: String): Boolean = runCatching {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}.isSuccess

internal fun openCharity(context: Context, target: CharityTarget) {
    when (target) {
        CharityTarget.Alipay -> {
            val deepLink =
                "alipays://platformapi/startapp?appId=20000067&url=${Uri.encode(ALIPAY_CHARITY_URL)}"
            val opened = isPackageInstalled(context, ALIPAY_PACKAGE) &&
                (openUriInPackage(context, deepLink, ALIPAY_PACKAGE) || openUri(context, deepLink))
            if (!opened && !openUri(context, ALIPAY_CHARITY_URL)) {
                Toast.makeText(context, R.string.home_support_unavailable, Toast.LENGTH_SHORT).show()
            }
        }

        CharityTarget.Tencent -> {
            if (!openUri(context, TENCENT_CHARITY_URL)) {
                Toast.makeText(context, R.string.home_support_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
