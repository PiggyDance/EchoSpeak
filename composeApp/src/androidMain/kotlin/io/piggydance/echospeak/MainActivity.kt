package io.piggydance.echospeak

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    private val voiceEchoController = VoiceEchoController()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // 使用带引导页的权限处理组件
            OnboardingPermissionHandler {
                // 在获得权限后才启动语音回声控制器和显示应用界面
                StartVoiceEchoEffect()
                App()
            }
        }
    }

    @Composable
    private fun StartVoiceEchoEffect() {
        // 使用 DisposableEffect 确保在权限授予后立即启动
        // 此函数只在 RecordAudioPermissionHandler 权限授予后被调用，所以这里是安全的
        DisposableEffect(Unit) {
            try {
                voiceEchoController.start(peekAvailableContext())
            } catch (e: SecurityException) {
                // 理论上不会发生，因为只有在权限授予后才会调用此函数
                e.printStackTrace()
            }
            onDispose {
                // 不在这里 release,因为 onDestroy 会处理
            }
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