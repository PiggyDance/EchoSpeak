package io.piggydance.echospeak

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 引导蒙层组件
 * 以半透明蒙层的形式覆盖在主界面上，引导用户授予权限
 */
@Composable
fun OnboardingOverlay(
    visible: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // 半透明黑色背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* 阻止点击穿透 */ },
            contentAlignment = Alignment.Center
        ) {
            // 引导卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 顶部图标
                    Text(
                        text = "🎤",
                        fontSize = 64.sp
                    )
                    
                    // 标题
                    Text(
                        text = "欢迎使用 EchoSpeak",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    // 说明文字
                    Text(
                        text = "实时语音回声助手\n帮助你练习发音和演讲",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 权限说明
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "需要以下权限：",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        PermissionItem(
                            icon = "🎙️",
                            title = "录音权限",
                            description = "用于捕捉和回放你的声音"
                        )
                    }
                    
                    // 隐私说明
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🔒", fontSize = 20.sp)
                            Text(
                                text = "所有录音仅在本地处理，不会上传",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    // 授权按钮
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "授予权限并开始",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 权限项展示
 */
@Composable
private fun PermissionItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * SharedPreferences 工具类，用于保存引导状态
 */
private const val PREFS_NAME = "echospeak_prefs"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

private fun isOnboardingCompleted(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
}

private fun setOnboardingCompleted(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
}

/**
 * 带引导蒙层的权限处理组件
 * 首次启动时显示引导蒙层，授予权限后不再显示
 */
@Composable
fun OnboardingPermissionHandler(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // 检查是否已完成引导
    var hasCompletedOnboarding by remember {
        mutableStateOf(isOnboardingCompleted(context))
    }
    var shouldRequestPermission by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 权限处理和主内容
        RecordAudioPermissionHandler(
            autoRequest = shouldRequestPermission,
            content = content
        )
        
        // 引导蒙层（只在首次启动且未完成引导时显示）
        OnboardingOverlay(
            visible = !hasCompletedOnboarding,
            onRequestPermission = {
                // 用户点击授权按钮
                shouldRequestPermission = true
                hasCompletedOnboarding = true
                // 保存引导已完成的状态
                setOnboardingCompleted(context)
            },
            onDismiss = {
                // 可选：允许用户跳过引导
                hasCompletedOnboarding = true
                setOnboardingCompleted(context)
            }
        )
    }
}
