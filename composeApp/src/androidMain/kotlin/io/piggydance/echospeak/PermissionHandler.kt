package io.piggydance.echospeak

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * 权限请求处理组件
 *
 * 使用 Google Accompanist Permissions 库来处理运行时权限
 * 直接使用系统权限对话框,不使用自定义UI
 *
 * @param permission 需要请求的权限
 * @param permissionName 权限的友好名称（如"录音权限"）
 * @param autoRequest 是否自动请求权限（默认true）
 * @param content 获得权限后显示的内容
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

    // 根据 autoRequest 参数决定是否自动弹出权限对话框
    LaunchedEffect(permissionState.status.isGranted, autoRequest) {
        if (!permissionState.status.isGranted && autoRequest) {
            permissionState.launchPermissionRequest()
        }
    }

    when {
        permissionState.status.isGranted -> {
            // 权限已授予,显示主内容
            content()
        }
        else -> {
            // 没有权限,显示引导蒙层
            Box(modifier = Modifier.fillMaxSize()) {
                // 显示引导蒙层
                OnboardingOverlay(
                    visible = true,
                    onRequestPermission = {
                        // 直接弹出系统权限请求对话框
                        permissionState.launchPermissionRequest()
                    },
                    onDismiss = { /* 不允许关闭,强制引导用户授权 */ }
                )
            }
        }
    }
}

/**
 * 录音权限请求组件
 * 这是一个便捷函数,专门用于请求录音权限
 *
 * @param autoRequest 是否自动请求权限（默认true）
 * @param content 获得权限后显示的内容
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
