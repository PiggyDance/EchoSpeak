package io.piggydance.echospeak

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.*

// ─── 入口 ─────────────────────────────────────────────────────────────────────

@Preview
@Composable
fun App() {
    EchoSpeakTheme {
        val mainViewModel = koinViewModel<MainViewModel>()
        val state by mainViewModel.state.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080C1A))
        ) {
            SciFiAudioVisualizerWithRealData(
                modifier = Modifier.align(Alignment.Center)
            )
            StatusTextWithRealData(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }
}

// ─── expect/actual 接口 ────────────────────────────────────────────────────────

/** androidMain actual 负责订阅真实音频数据并提供头像 Composable */
@Composable
expect fun SciFiAudioVisualizerWithRealData(modifier: Modifier = Modifier)

@Composable
expect fun StatusTextWithRealData(modifier: Modifier = Modifier)

// ─── 颜色配置 ──────────────────────────────────────────────────────────────────

internal data class ModeColors(
    val primary: Color,
    val secondary: Color,
    val glow: Color,
)

internal fun modeColors(audioMode: AudioMode) = when (audioMode) {
    AudioMode.IDLE      -> ModeColors(Color(0xFF2A4080), Color(0xFF1A2A5A), Color(0xFF3060C0))
    AudioMode.LISTENING -> ModeColors(Color(0xFF00C8FF), Color(0xFF0080CC), Color(0xFF00A8E0))
    AudioMode.RECORDING -> ModeColors(Color(0xFFFF3A6E), Color(0xFFCC1A4A), Color(0xFFFF6090))
    AudioMode.PLAYING   -> ModeColors(Color(0xFF00E87A), Color(0xFF00A854), Color(0xFF40FFA0))
}

// ─── 圆形频谱可视化 ────────────────────────────────────────────────────────────

/**
 * 圆形极坐标频谱可视化
 *
 * @param audioMode    当前音频模式
 * @param spectrum     60 个频段的幅度（0~1）
 * @param avatarRadius 中心头像圆的半径（dp），Canvas 中间留出该区域给 avatarContent
 * @param avatarContent 叠在中心的头像 Composable（可为 null，此时绘制默认占位球）
 */
@Composable
fun SciFiAudioVisualizer(
    audioMode: AudioMode,
    spectrum: List<Float>,
    avatarRadius: Dp = 64.dp,
    modifier: Modifier = Modifier,
    avatarContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val colors = modeColors(audioMode)

    // 呼吸动画（驱动光晕和待机状态下的条形基线）
    val infiniteTransition = rememberInfiniteTransition()
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (audioMode) {
                    AudioMode.IDLE      -> 3200
                    AudioMode.LISTENING -> 2000
                    AudioMode.RECORDING -> 700
                    AudioMode.PLAYING   -> 1100
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    // 频谱平滑
    val smoothed = rememberSmoothedSpectrum(spectrum, audioMode)
    // 整体响度（0~1），用于光晕扩张
    val overallVolume = remember(smoothed) {
        smoothed.average().toFloat().coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // ── Canvas：绘制圆形频谱 + 光晕 ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val avatarPx = avatarRadius.toPx()

            // 外层大光晕
            drawGlow(center, avatarPx, breathScale, overallVolume, colors, audioMode)

            // 圆形频谱条
            drawRadialSpectrum(center, avatarPx, smoothed, breathScale, colors, audioMode)

            // 头像圆圈描边 + 内圈
            drawAvatarRing(center, avatarPx, breathScale, overallVolume, colors, audioMode)
        }

        // ── 中心头像层（叠在 Canvas 上方） ──
        Box(
            modifier = Modifier
                .size(avatarRadius * 2)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarContent != null) {
                avatarContent()
            } else {
                // 无头像时：绘制默认占位（纯色渐变圆）
                DefaultAvatarPlaceholder(colors, audioMode)
            }
        }
    }
}

// ─── 频谱平滑 ──────────────────────────────────────────────────────────────────

@Composable
internal fun rememberSmoothedSpectrum(
    rawSpectrum: List<Float>,
    audioMode: AudioMode,
): List<Float> {
    val smoothFactor = when (audioMode) {
        AudioMode.IDLE      -> 0.05f
        AudioMode.LISTENING -> 0.28f
        AudioMode.RECORDING -> 0.50f
        AudioMode.PLAYING   -> 0.44f
    }
    val prev = remember { mutableStateOf(List(60) { 0f }) }
    val smoothed = prev.value.zip(rawSpectrum).map { (p, r) ->
        p + (r - p) * smoothFactor
    }
    prev.value = smoothed
    return smoothed
}

// ─── 绘制函数 ──────────────────────────────────────────────────────────────────

/** 外层呼吸光晕 */
private fun DrawScope.drawGlow(
    center: Offset,
    avatarPx: Float,
    breathScale: Float,
    volume: Float,
    colors: ModeColors,
    audioMode: AudioMode,
) {
    val boost = if (audioMode != AudioMode.IDLE) volume * 0.35f else 0f
    val glowRadius = avatarPx * (2.8f + boost) * breathScale

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to colors.glow.copy(alpha = 0f),
                0.5f to colors.glow.copy(alpha = 0.07f * breathScale),
                0.8f to colors.primary.copy(alpha = 0.13f * breathScale),
                1.0f to Color.Transparent,
            ),
            center = center,
            radius = glowRadius,
        ),
        center = center,
        radius = glowRadius,
    )
}

/** 圆形极坐标频谱条 */
private fun DrawScope.drawRadialSpectrum(
    center: Offset,
    avatarPx: Float,
    spectrum: List<Float>,
    breathScale: Float,
    colors: ModeColors,
    audioMode: AudioMode,
) {
    val barCount = spectrum.size  // 60
    val angleStep = (2 * PI / barCount).toFloat()

    // 频谱条起始半径 = 头像半径 + 小间隙
    val innerRadius = avatarPx + 10f
    // 最大条长
    val maxBarLength = minOf(size.width, size.height) * 0.22f
    // 静默基线长度
    val idleBar = if (audioMode == AudioMode.IDLE) avatarPx * 0.06f * breathScale else 2f
    // 描边宽度
    val strokeWidth = (size.width * 0.012f).coerceIn(3f, 9f)

    spectrum.forEachIndexed { i, amp ->
        // 从顶部(-PI/2)开始顺时针
        val angle = -PI.toFloat() / 2f + i * angleStep

        val barLen = if (audioMode == AudioMode.IDLE) {
            idleBar
        } else {
            val mapped = sqrt(amp.coerceIn(0f, 1f))
            (idleBar + mapped * maxBarLength).coerceAtMost(maxBarLength)
        }

        val startX = center.x + cos(angle) * innerRadius
        val startY = center.y + sin(angle) * innerRadius
        val endX   = center.x + cos(angle) * (innerRadius + barLen)
        val endY   = center.y + sin(angle) * (innerRadius + barLen)

        // 幅度越大越亮
        val alpha = (0.35f + amp * 0.65f).coerceIn(0.3f, 1f)
        val barColor = lerpColor(colors.secondary, colors.primary, amp).copy(alpha = alpha)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    barColor.copy(alpha = alpha * 0.4f),
                    barColor,
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** 头像圆圈描边 */
private fun DrawScope.drawAvatarRing(
    center: Offset,
    avatarPx: Float,
    breathScale: Float,
    volume: Float,
    colors: ModeColors,
    audioMode: AudioMode,
) {
    val ringAlpha = if (audioMode == AudioMode.IDLE) 0.4f
    else (0.6f + volume * 0.4f).coerceIn(0.4f, 1f)

    // 外描边（发光）
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                colors.glow.copy(alpha = ringAlpha),
                colors.primary.copy(alpha = ringAlpha * 0.6f),
                colors.glow.copy(alpha = ringAlpha),
            ),
            center = center,
        ),
        center = center,
        radius = avatarPx,
        style = Stroke(width = 3f * breathScale),
    )

    // 细内描边（白色高光）
    drawCircle(
        color = Color.White.copy(alpha = ringAlpha * 0.25f),
        center = center,
        radius = avatarPx - 2f,
        style = Stroke(width = 1f),
    )
}

// ─── 默认头像占位 ──────────────────────────────────────────────────────────────

@Composable
private fun DefaultAvatarPlaceholder(colors: ModeColors, audioMode: AudioMode) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.15f),
                    0.5f to colors.primary.copy(alpha = 0.5f),
                    1.0f to colors.secondary.copy(alpha = 0.9f),
                ),
                center = center,
                radius = size.minDimension / 2f,
            ),
            center = center,
            radius = size.minDimension / 2f,
        )
    }
    Text(
        text = "👤",
        fontSize = 48.sp,
        modifier = Modifier.wrapContentSize(Alignment.Center),
    )
}

// ─── 状态文字 ──────────────────────────────────────────────────────────────────

@Composable
fun StatusText(
    audioMode: AudioMode,
    modifier: Modifier = Modifier,
) {
    val stringRes = getStringResources()
    val statusText = when (audioMode) {
        AudioMode.LISTENING -> stringRes.statusListening
        AudioMode.RECORDING -> stringRes.statusRecording
        AudioMode.PLAYING   -> stringRes.statusPlaying
        AudioMode.IDLE      -> stringRes.statusIdle
    }
    val colors = modeColors(audioMode)

    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        )
    )

    Text(
        text = statusText,
        modifier = modifier,
        color = colors.primary.copy(alpha = if (audioMode != AudioMode.IDLE) alpha else 0.4f),
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            shadow = Shadow(
                color = colors.glow.copy(alpha = 0.85f),
                blurRadius = 14f,
            )
        )
    )
}

// ─── 音频模式枚举 ──────────────────────────────────────────────────────────────

enum class AudioMode {
    IDLE,
    LISTENING,
    RECORDING,
    PLAYING,
}

// ─── 颜色插值 ──────────────────────────────────────────────────────────────────

internal fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red   = start.red   + (end.red   - start.red)   * t,
        green = start.green + (end.green - start.green) * t,
        blue  = start.blue  + (end.blue  - start.blue)  * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t,
    )
}
