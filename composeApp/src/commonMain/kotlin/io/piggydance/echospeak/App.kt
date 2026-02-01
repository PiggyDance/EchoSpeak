package io.piggydance.echospeak

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.*

@Preview
@Composable
fun App() {
    MaterialTheme {
        val mainViewModel = koinViewModel<MainViewModel>()
        val state by mainViewModel.state.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E27),
                            Color(0xFF1A1F3A),
                            Color(0xFF0A0E27)
                        )
                    )
                )
        ) {
            // 背景粒子效果
            ParticleBackground()
            
            // 主音频可视化（使用真实音频数据）
            SciFiAudioVisualizerWithRealData()
            
            // 状态文字
            StatusTextWithRealData(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }
}

@Composable
expect fun SciFiAudioVisualizerWithRealData(modifier: Modifier = Modifier)

@Composable
expect fun StatusTextWithRealData(modifier: Modifier = Modifier)

@Composable
fun SciFiAudioVisualizer(
    audioMode: AudioMode,
    spectrum: List<Float>,
    modifier: Modifier = Modifier
) {
    val isActive = audioMode != AudioMode.IDLE
    
    // 动画状态
    val infiniteTransition = rememberInfiniteTransition()
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // 脉冲动画
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = minOf(size.width, size.height) * 0.25f
        
        // 绘制外圈光环
        drawGlowingRings(
            center = Offset(centerX, centerY),
            baseRadius = baseRadius,
            rotation = rotation,
            pulse = pulse,
            isActive = isActive,
            audioMode = audioMode
        )
        
        // 绘制音频波形圆环（使用真实频谱数据）
        drawCircularWaveform(
            center = Offset(centerX, centerY),
            radius = baseRadius * 0.8f,
            waveformData = spectrum,
            rotation = rotation,
            isActive = isActive,
            audioMode = audioMode
        )
        
        // 绘制中心核心
        drawCenterCore(
            center = Offset(centerX, centerY),
            radius = baseRadius * 0.3f,
            pulse = pulse,
            audioMode = audioMode
        )
    }
}

private fun DrawScope.drawGlowingRings(
    center: Offset,
    baseRadius: Float,
    rotation: Float,
    pulse: Float,
    isActive: Boolean,
    audioMode: AudioMode
) {
    // 根据音频模式选择颜色
    val colors = when (audioMode) {
        AudioMode.RECORDING -> listOf(
            Color(0xFFFF0080), // 录音：粉红色系
            Color(0xFFFF3399),
            Color(0xFFFF66B2)
        )
        AudioMode.PLAYING -> listOf(
            Color(0xFF00FF80), // 播放：绿色系
            Color(0xFF33FF99),
            Color(0xFF66FFB2)
        )
        AudioMode.IDLE -> listOf(
            Color(0xFF00F5FF), // 空闲：青色系
            Color(0xFF00D9FF),
            Color(0xFF0099FF)
        )
    }
    
    // 绘制3个旋转光环
    for (i in 0..2) {
        val radius = baseRadius * (1.2f + i * 0.15f) * if (isActive) pulse else 1f
        val alpha = if (isActive) 0.6f - i * 0.15f else 0.2f
        val angleOffset = rotation + i * 120f
        
        // 绘制光环弧线
        for (segment in 0..5) {
            val startAngle = angleOffset + segment * 60f
            val sweepAngle = 40f
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        colors[i].copy(alpha = 0f),
                        colors[i].copy(alpha = alpha),
                        colors[i].copy(alpha = 0f)
                    ),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

private fun DrawScope.drawCircularWaveform(
    center: Offset,
    radius: Float,
    waveformData: List<Float>,
    rotation: Float,
    isActive: Boolean,
    audioMode: AudioMode
) {
    val barCount = waveformData.size
    val angleStep = 360f / barCount
    
    waveformData.forEachIndexed { index, amplitude ->
        val angle = (rotation + index * angleStep) * PI / 180f
        val barHeight = radius * 0.4f * amplitude * if (isActive) 1f else 0.1f
        
        val innerRadius = radius
        val outerRadius = radius + barHeight
        
        val startX = center.x + cos(angle).toFloat() * innerRadius
        val startY = center.y + sin(angle).toFloat() * innerRadius
        val endX = center.x + cos(angle).toFloat() * outerRadius
        val endY = center.y + sin(angle).toFloat() * outerRadius
        
        // 根据音频模式选择渐变色
        val (startColor, endColor) = when (audioMode) {
            AudioMode.RECORDING -> Pair(
                Color(0xFFFF0080),  // 粉红
                Color(0xFFFF00FF)   // 紫色
            )
            AudioMode.PLAYING -> Pair(
                Color(0xFF00FF80),  // 绿色
                Color(0xFF00FFFF)   // 青色
            )
            AudioMode.IDLE -> Pair(
                Color(0xFF00F5FF),  // 青色
                Color(0xFF0080FF)   // 蓝色
            )
        }
        
        val color = lerp(startColor, endColor, amplitude)
        
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    color.copy(alpha = 0.3f),
                    color.copy(alpha = 0.9f)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCenterCore(
    center: Offset,
    radius: Float,
    pulse: Float,
    audioMode: AudioMode
) {
    val coreRadius = radius * pulse
    
    // 确定核心颜色
    val coreColor = when (audioMode) {
        AudioMode.RECORDING -> Color(0xFFFF0080)  // 录音：粉红色
        AudioMode.PLAYING -> Color(0xFF00FF80)    // 播放：绿色
        AudioMode.IDLE -> Color(0xFF4080FF)       // 空闲：蓝色
    }
    
    // 绘制发光核心
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreColor.copy(alpha = 0.8f),
                coreColor.copy(alpha = 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = coreRadius * 1.5f
        ),
        center = center,
        radius = coreRadius * 1.5f
    )
    
    // 绘制实心核心
    drawCircle(
        color = coreColor,
        center = center,
        radius = coreRadius * 0.6f
    )
    
    // 绘制核心边框
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        center = center,
        radius = coreRadius * 0.6f,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )
}

@Composable
fun ParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    
    // 粒子位置动画
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val particleCount = 50
        
        for (i in 0 until particleCount) {
            val x = (size.width * (i % 10) / 10f + particleOffset * (i % 3 - 1)) % size.width
            val y = (size.height * (i / 10) / 5f + particleOffset * 0.5f * (i % 2)) % size.height
            val alpha = (sin(particleOffset * 0.01f + i) + 1f) / 2f * 0.3f
            
            drawCircle(
                color = Color(0xFF00F5FF).copy(alpha = alpha),
                center = Offset(x, y),
                radius = 2f
            )
        }
    }
}

@Composable
fun StatusText(
    audioMode: AudioMode,
    modifier: Modifier = Modifier
) {
    val statusText = when (audioMode) {
        AudioMode.RECORDING -> "● 正在录音..."
        AudioMode.PLAYING -> "▶ 正在播放..."
        AudioMode.IDLE -> "待机中"
    }
    
    val statusColor = when (audioMode) {
        AudioMode.RECORDING -> Color(0xFFFF0080)
        AudioMode.PLAYING -> Color(0xFF00FF80)
        AudioMode.IDLE -> Color(0xFF4080FF)
    }
    
    val isActive = audioMode != AudioMode.IDLE
    
    // 闪烁动画
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Text(
        text = statusText,
        modifier = modifier,
        color = statusColor.copy(alpha = if (isActive) alpha else 0.6f),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = statusColor,
                blurRadius = 10f
            )
        )
    )
}

// 音频模式枚举
enum class AudioMode {
    IDLE,       // 空闲
    RECORDING,  // 录音中
    PLAYING     // 播放中
}

// 颜色插值函数
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}
