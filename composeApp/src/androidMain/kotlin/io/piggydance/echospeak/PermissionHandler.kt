package io.piggydance.echospeak

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * 权限请求处理组件
 *
 * 根据系统的实际返回结果（shouldShowRationale）判断状态，不手动追踪拒绝次数：
 *
 * - shouldShowRationale = true  → 用户拒绝过，系统仍允许再次弹窗 → 显示正常引导
 * - shouldShowRationale = false + 已请求过 → 系统判定永久拒绝 → 引导去系统设置
 *
 * 用 [hasRequested] 仅在本次 Session 内区分"从未请求"和"永久拒绝"
 * （两种情况的 shouldShowRationale 都是 false，必须有此标志才能区分）
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permission: String,
    permissionName: String = "该权限",
    autoRequest: Boolean = true,
    content: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(permission)
    val context = LocalContext.current

    // 本次 Session 内是否已经发起过授权请求（不持久化到磁盘）
    var hasRequested by remember { mutableStateOf(false) }

    // 完全依赖系统字段判断：永久拒绝 = 未授权 + 系统不再弹窗 + 本次已请求过
    val isPermanentlyDenied = !permissionState.status.isGranted
        && !permissionState.status.shouldShowRationale
        && hasRequested

    LaunchedEffect(permissionState.status.isGranted, autoRequest) {
        if (!permissionState.status.isGranted && autoRequest) {
            hasRequested = true
            permissionState.launchPermissionRequest()
        }
    }

    when {
        permissionState.status.isGranted -> {
            content()
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                OnboardingOverlay(
                    visible = true,
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRequestPermission = {
                        if (isPermanentlyDenied) {
                            // 系统已封禁弹窗，只能去系统设置手动开启
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } else {
                            // 系统仍允许弹窗，直接弹授权对话框
                            hasRequested = true
                            permissionState.launchPermissionRequest()
                        }
                    },
                    onDismiss = { /* 不允许关闭，强制引导用户授权 */ }
                )
            }
        }
    }
}

/**
 * 录音权限请求组件（便捷封装）
 */
@Composable
fun RecordAudioPermissionHandler(
    autoRequest: Boolean = true,
    content: @Composable () -> Unit
) {
    PermissionHandler(
        permission = Manifest.permission.RECORD_AUDIO,
        permissionName = "录音权限",
        autoRequest = autoRequest,
        content = content
    )
}
