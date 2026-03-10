package io.piggydance.echospeak

import androidx.compose.runtime.Composable

// 字符串资源接口
interface StringResources {
    val appName: String
    val statusRecording: String
    val statusPlaying: String
    val statusIdle: String
    val modeIdle: String
    val modeRecording: String
    val modePlaying: String
}

// 预期函数，在平台特定代码中实现
@Composable
expect fun getStringResources(): StringResources
