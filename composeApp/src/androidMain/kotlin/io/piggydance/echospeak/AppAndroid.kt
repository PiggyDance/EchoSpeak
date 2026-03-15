package io.piggydance.echospeak

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.CircleShape
import io.piggydance.echospeak.audio.AudioVisualizerManager
import io.piggydance.echospeak.auth.GoogleAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
actual fun SciFiAudioVisualizerWithRealData(modifier: Modifier) {
    val visualizerData by AudioVisualizerManager.visualizerData.collectAsStateWithLifecycle()

    // TODO: Google 登录暂时关闭，头像展示挂起，恢复时取消以下注释并删除 photoUrl = null
    // val currentUser by GoogleAuthManager.currentUser.collectAsStateWithLifecycle()
    // val photoUrl = currentUser?.photoUrl
    val photoUrl: String? = null

    val audioMode = visualizerData.mode.toAppMode()

    // 异步加载头像，无额外依赖（photoUrl 为 null 时直接返回 null bitmap）
    val avatarBitmap by rememberAvatarBitmap(photoUrl)

    SciFiAudioVisualizer(
        audioMode = audioMode,
        spectrum = visualizerData.spectrum,
        avatarRadius = 68.dp,
        modifier = modifier,
        avatarContent = {
            if (avatarBitmap != null) {
                // Google 头像（登录后展示）
                Image(
                    bitmap = avatarBitmap!!,
                    contentDescription = "用户头像",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                // 未登录时：渐变占位圆
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1A3060), Color(0xFF08142E))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "👤", fontSize = 40.sp)
                }
            }
        }
    )
}

/** 用 HttpURLConnection 异步下载头像，转为 ImageBitmap，无额外依赖 */
@Composable
private fun rememberAvatarBitmap(url: String?): State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        if (url.isNullOrBlank()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Composable
actual fun StatusTextWithRealData(modifier: Modifier) {
    val visualizerData by AudioVisualizerManager.visualizerData.collectAsStateWithLifecycle()
    StatusText(
        audioMode = visualizerData.mode.toAppMode(),
        modifier = modifier,
    )
}

// ─── 模式转换 ──────────────────────────────────────────────────────────────────

private fun AudioVisualizerManager.AudioMode.toAppMode() = when (this) {
    AudioVisualizerManager.AudioMode.IDLE      -> AudioMode.IDLE
    AudioVisualizerManager.AudioMode.LISTENING -> AudioMode.LISTENING
    AudioVisualizerManager.AudioMode.RECORDING -> AudioMode.RECORDING
    AudioVisualizerManager.AudioMode.PLAYING   -> AudioMode.PLAYING
}
