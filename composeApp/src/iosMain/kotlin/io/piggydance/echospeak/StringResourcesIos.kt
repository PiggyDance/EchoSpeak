package io.piggydance.echospeak

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// iOS 平台实现 (简单版本，可以后续扩展为从 .strings 文件读取)
class IosStringResources : StringResources {
    override val appName: String = "EchoSpeak"
    override val statusRecording: String = "● Recording..."
    override val statusPlaying: String = "▶ Playing..."
    override val statusIdle: String = "Standby"
    override val modeIdle: String = "Idle"
    override val modeRecording: String = "Recording"
    override val modePlaying: String = "Playing"
}

@Composable
actual fun getStringResources(): StringResources {
    return remember {
        IosStringResources()
    }
}
