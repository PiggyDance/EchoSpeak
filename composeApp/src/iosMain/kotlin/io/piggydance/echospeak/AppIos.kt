package io.piggydance.echospeak

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun SciFiAudioVisualizerWithRealData(modifier: Modifier) {
    // iOS 实现：使用空闲模式和空数据
    SciFiAudioVisualizer(
        audioMode = AudioMode.IDLE,
        spectrum = List(60) { 0f },
        modifier = modifier
    )
}

@Composable
actual fun StatusTextWithRealData(modifier: Modifier) {
    // iOS 实现：显示空闲状态
    StatusText(
        audioMode = AudioMode.IDLE,
        modifier = modifier
    )
}
