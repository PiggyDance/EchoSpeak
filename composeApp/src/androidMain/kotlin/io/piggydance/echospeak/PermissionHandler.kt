package io.piggydance.echospeak

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

/**
 * 权限请求处理组件
 *
 * 使用 Google Accompanist Permissions 库来处理运行时权限
 * 直接使用系统权限对话框,不使用自定义UI
 *
 * @param permission 需要请求的权限
 * @param permissionName 权限的友好名称（如"录音权限"）
 * @param content 获得权限后显示的内容
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permission: String,
    permissionName: String = "该权限",
    content: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(permission)
    val context = LocalContext.current

    // 首次自动弹出权限对话框
    LaunchedEffect(permissionState.status.isGranted) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    when {
        permissionState.status.isGranted -> {
            // 权限已授予,显示主内容
            content()
        }
        else -> {
            // 没有权限,显示空白界面和浮动按钮
            Box(modifier = Modifier.fillMaxSize()) {
                // 检查是否是"不再询问"状态
                val isDenied = permissionState.status is PermissionStatus.Denied
                val shouldShowRationale = (permissionState.status as? PermissionStatus.Denied)?.shouldShowRationale ?: false
                
                // 底部浮动按钮
                FloatingActionButton(
                    onClick = {
                        if (isDenied && !shouldShowRationale) {
                            // 永久拒绝,跳转设置
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } else {
                            // 弹出系统权限对话框
                            permissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isDenied && !shouldShowRationale) "去设置" else "授权",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 录音权限请求组件
 * 这是一个便捷函数,专门用于请求录音权限
 */
@Composable
fun RecordAudioPermissionHandler(
    content: @Composable () -> Unit
) {
    PermissionHandler(
        permission = Manifest.permission.RECORD_AUDIO,
        permissionName = "录音权限",
        content = content
    )
}
