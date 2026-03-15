package io.piggydance.echospeak

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ─── 颜色常量 ──────────────────────────────────────────────────────────────────

private val BgDeep   = Color(0xFF060A18)
private val BgMid    = Color(0xFF0D1428)
private val Cyan     = Color(0xFF00C8FF)
private val CyanDim  = Color(0xFF0080CC)
private val White80  = Color.White.copy(alpha = 0.80f)
private val White50  = Color.White.copy(alpha = 0.50f)
private val White30  = Color.White.copy(alpha = 0.30f)
private val White12  = Color.White.copy(alpha = 0.12f)

// ─── 引导主界面 ────────────────────────────────────────────────────────────────

@Composable
fun OnboardingOverlay(
    visible: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    isPermanentlyDenied: Boolean = false,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(400)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(BgDeep, BgMid, BgDeep))
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* 阻止穿透 */ },
        ) {
            // 背景装饰圆环
            BackgroundRings()

            // 主内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp)
                    .padding(top = 80.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 上半：图标 + 标题 + 副标题
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Spacer(Modifier.height(32.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "EchoSpeak",
                            style = MaterialTheme.typography.displaySmall.copy(
                                shadow = Shadow(
                                    color = Cyan.copy(alpha = 0.6f),
                                    blurRadius = 20f,
                                )
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (isPermanentlyDenied)
                                "麦克风权限已被系统禁止\n请前往设置手动开启"
                            else
                                "实时语音回声助手\n听见自己，精进表达",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isPermanentlyDenied) Color(0xFFFF8A80) else White50,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp,
                        )
                    }
                }

                // 中段：权限说明卡片
                PermissionCard(isPermanentlyDenied = isPermanentlyDenied)

                // 下半：按钮
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GlowButton(
                        text = if (isPermanentlyDenied) "前往系统设置开启" else "授予权限并开始",
                        onClick = onRequestPermission,
                    )

                    // 提示文字
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isPermanentlyDenied) {
                            Text("⚙️", fontSize = 13.sp)
                            Text(
                                text = "设置 → 应用 → EchoSpeak → 权限 → 麦克风",
                                style = MaterialTheme.typography.bodySmall,
                                color = White30,
                            )
                        } else {
                            Text("🔒", fontSize = 13.sp)
                            Text(
                                text = "所有录音仅在本地处理，绝不上传",
                                style = MaterialTheme.typography.bodySmall,
                                color = White30,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 背景装饰圆环 ──────────────────────────────────────────────────────────────

@Composable
private fun BackgroundRings() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.28f   // 对齐图标位置

        for (i in 0..2) {
            val r = 130f + i * 70f
            val alpha = 0.12f - i * 0.03f
            drawCircle(
                color = Cyan.copy(alpha = alpha),
                center = Offset(cx, cy),
                radius = r,
                style = Stroke(width = 1.5f),
            )
        }

        // 旋转光点
        val dotR = 195f
        val angle = Math.toRadians(rotation.toDouble())
        val dx = cos(angle).toFloat() * dotR
        val dy = sin(angle).toFloat() * dotR
        drawCircle(
            color = Cyan.copy(alpha = 0.55f),
            center = Offset(cx + dx, cy + dy),
            radius = 4f,
        )
        drawCircle(
            color = Cyan.copy(alpha = 0.20f),
            center = Offset(cx + dx, cy + dy),
            radius = 12f,
        )

        // 对角反向光点
        val angle2 = Math.toRadians((rotation + 180f).toDouble())
        val dx2 = cos(angle2).toFloat() * dotR
        val dy2 = sin(angle2).toFloat() * dotR
        drawCircle(
            color = Cyan.copy(alpha = 0.35f),
            center = Offset(cx + dx2, cy + dy2),
            radius = 3f,
        )
    }
}

// ─── 脉冲麦克风图标 ────────────────────────────────────────────────────────────

@Composable
private fun PulsingMicIcon() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp),
    ) {
        // 最外层光晕
        Box(
            modifier = Modifier
                .size((100 * pulse).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Cyan.copy(alpha = 0.08f * pulse), Color.Transparent)
                    )
                )
        )
        // 中层环
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyanDim.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .border(1.dp, Cyan.copy(alpha = 0.3f), CircleShape)
        )
        // 实心核心圆
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A3060), Color(0xFF08142E))
                    )
                )
                .border(1.5.dp, Cyan.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎤", fontSize = 28.sp)
        }
    }
}

// ─── 权限说明卡片 ──────────────────────────────────────────────────────────────

@Composable
private fun PermissionCard(isPermanentlyDenied: Boolean = false) {
    val accentColor = if (isPermanentlyDenied) Color(0xFFFF6B6B) else Cyan
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(White12)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.45f),
                        accentColor.copy(alpha = 0.10f),
                        accentColor.copy(alpha = 0.45f),
                    )
                ),
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = if (isPermanentlyDenied) "权限已被系统禁止" else "需要以下权限",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                letterSpacing = 1.5.sp,
            )
            PermissionRow(
                icon = if (isPermanentlyDenied) "🚫" else "🎙️",
                title = "麦克风",
                desc = if (isPermanentlyDenied)
                    "请前往系统设置手动开启麦克风权限"
                else
                    "录制你的声音并实时回放",
            )
        }
    }
}

@Composable
private fun PermissionRow(icon: String, title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Cyan.copy(alpha = 0.12f))
                .border(1.dp, Cyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = White80,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = White50,
            )
        }
    }
}

// ─── 发光主按钮 ────────────────────────────────────────────────────────────────

@Composable
private fun GlowButton(text: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)   // 放在 clip 之前，保证整个矩形区域都可点击
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(CyanDim, Cyan, CyanDim)
                )
            )
            .border(
                1.5.dp,
                Cyan.copy(alpha = glowAlpha),
                RoundedCornerShape(18.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    blurRadius = 4f,
                )
            ),
            fontWeight = FontWeight.Bold,
            color = Color(0xFF060A18),
            letterSpacing = 0.5.sp,
        )
    }
}

// ─── SharedPreferences 工具 ────────────────────────────────────────────────────

private const val PREFS_NAME = "echospeak_prefs"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

private fun isOnboardingCompleted(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDING_COMPLETED, false)

private fun setOnboardingCompleted(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()

// ─── 权限处理容器 ──────────────────────────────────────────────────────────────

@Composable
fun OnboardingPermissionHandler(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasCompletedOnboarding by remember { mutableStateOf(isOnboardingCompleted(context)) }
    var shouldRequestPermission by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        RecordAudioPermissionHandler(
            autoRequest = shouldRequestPermission,
            content = content,
        )
        OnboardingOverlay(
            visible = !hasCompletedOnboarding,
            onRequestPermission = {
                shouldRequestPermission = true
                hasCompletedOnboarding = true
                setOnboardingCompleted(context)
            },
            onDismiss = {
                hasCompletedOnboarding = true
                setOnboardingCompleted(context)
            },
        )
    }
}
