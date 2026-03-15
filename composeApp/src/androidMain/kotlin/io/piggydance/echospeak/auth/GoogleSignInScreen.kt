package io.piggydance.echospeak.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Google 登录引导蒙层
 *
 * 在录音权限授予后弹出，引导用户用 Google 账号登录以显示头像。
 * 用户可以选择跳过，此时显示默认占位头像。
 */
@Composable
fun GoogleSignInOverlay(
    visible: Boolean,
    onSignInSuccess: (GoogleUser) -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSetupGuide by remember { mutableStateOf(false) }

    // 启动时检测配置状态
    LaunchedEffect(Unit) {
        if (!GoogleAuthManager.isConfigured) {
            showSetupGuide = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6080C1A))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* 阻止穿透 */ },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 头像占位圆
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1A3060), Color(0xFF0A1830))
                            )
                        )
                        .border(2.dp, Color(0xFF00C8FF).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("👤", fontSize = 46.sp)
                }

                Text(
                    text = "个性化你的体验",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "使用 Google 账号登录\n在声波可视化中显示你的头像",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )

                // 未配置时显示配置指引
                if (showSetupGuide) {
                    SetupGuideBox()
                }

                // Google 登录按钮（未配置时禁用）
                GoogleSignInButton(
                    isLoading = isLoading,
                    isDisabled = showSetupGuide,
                    onClick = {
                        if (!isLoading && !showSetupGuide) {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                when (val result = GoogleAuthManager.signIn(context)) {
                                    is SignInResult.Success -> {
                                        isLoading = false
                                        onSignInSuccess(result.user)
                                    }
                                    is SignInResult.Cancelled -> {
                                        isLoading = false
                                    }
                                    is SignInResult.Error -> {
                                        isLoading = false
                                        errorMessage = when (result.errorCode) {
                                            SignInError.NOT_CONFIGURED ->
                                                "未配置 Web Client ID，请查看 GoogleAuthManager.kt"
                                            SignInError.NO_CREDENTIAL ->
                                                "未找到 Google 账号。\n请确认：\n① 设备已登录 Google 账号\n② SHA-1 指纹已在 Google Cloud Console 注册"
                                            SignInError.UNKNOWN ->
                                                "登录失败：${result.detail.take(80)}"
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                // 错误提示
                errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF7070),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }

                // 跳过按钮
                Text(
                    text = "稍后再说",
                    modifier = Modifier
                        .clickable { onSkip() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.38f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 开发配置指引卡片 */
@Composable
private fun SetupGuideBox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1030), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFFF8C00).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "⚙️ 开发者：需完成 Google 配置",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF8C00),
        )
        Text(
            text = """
1. 前往 Google Cloud Console
   → APIs & Services → Credentials

2. 创建「OAuth 2.0 Web 应用」客户端

3. 将 SHA-1 指纹添加到 Android 应用
   （运行：./gradlew signingReport）

4. 将 Web Client ID 填入：
   GoogleAuthManager.WEB_CLIENT_ID
            """.trimIndent(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isDisabled) Color(0xFFCCCCCC) else Color.White
    val textColor = if (isDisabled) Color(0xFF999999) else Color(0xFF3C4043)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(
                if (!isDisabled) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF4285F4),
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "G",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDisabled) Color(0xFF999999) else Color(0xFF4285F4),
                )
                Text(
                    text = "使用 Google 账号登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}
