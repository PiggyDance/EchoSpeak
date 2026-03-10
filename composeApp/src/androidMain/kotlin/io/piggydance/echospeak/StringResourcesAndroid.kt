package io.piggydance.echospeak

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Android 平台实现
class AndroidStringResources(private val context: Context) : StringResources {
    override val appName: String
        get() = context.getString(R.string.app_name)
    
    override val statusListening: String
        get() = context.getString(R.string.status_listening)
    
    override val statusRecording: String
        get() = context.getString(R.string.status_recording)
    
    override val statusPlaying: String
        get() = context.getString(R.string.status_playing)
    
    override val statusIdle: String
        get() = context.getString(R.string.status_idle)
    
    override val modeIdle: String
        get() = context.getString(R.string.mode_idle)
    
    override val modeListening: String
        get() = context.getString(R.string.mode_listening)
    
    override val modeRecording: String
        get() = context.getString(R.string.mode_recording)
    
    override val modePlaying: String
        get() = context.getString(R.string.mode_playing)
}

@Composable
actual fun getStringResources(): StringResources {
    val context = LocalContext.current
    return remember(context) {
        AndroidStringResources(context)
    }
}
