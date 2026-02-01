package io.piggydance.echospeak

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    private val voiceEchoController = VoiceEchoController()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        voiceEchoController.start(peekAvailableContext())
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        // 应用进入前台时保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        // 应用退到后台时取消屏幕常亮
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        voiceEchoController.release()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}