package io.piggydance.echospeak

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.piggydance.echospeak.audio.AudioVisualizerManager

@Composable
actual fun SciFiAudioVisualizerWithRealData(modifier: Modifier) {
    // 订阅真实音频数据
    val visualizerData by AudioVisualizerManager.visualizerData.collectAsStateWithLifecycle()
    
    // 将 AudioVisualizerManager.AudioMode 转换为 App.AudioMode
    val audioMode = when (visualizerData.mode) {
        AudioVisualizerManager.AudioMode.IDLE -> AudioMode.IDLE
        AudioVisualizerManager.AudioMode.RECORDING -> AudioMode.RECORDING
        AudioVisualizerManager.AudioMode.PLAYING -> AudioMode.PLAYING
    }
    
    SciFiAudioVisualizer(
        audioMode = audioMode,
        spectrum = visualizerData.spectrum,
        modifier = modifier
    )
}

@Composable
actual fun StatusTextWithRealData(modifier: Modifier) {
    // 订阅真实音频数据
    val visualizerData by AudioVisualizerManager.visualizerData.collectAsStateWithLifecycle()
    
    // 将 AudioVisualizerManager.AudioMode 转换为 App.AudioMode
    val audioMode = when (visualizerData.mode) {
        AudioVisualizerManager.AudioMode.IDLE -> AudioMode.IDLE
        AudioVisualizerManager.AudioMode.RECORDING -> AudioMode.RECORDING
        AudioVisualizerManager.AudioMode.PLAYING -> AudioMode.PLAYING
    }
    
    StatusText(
        audioMode = audioMode,
        modifier = modifier
    )
}
