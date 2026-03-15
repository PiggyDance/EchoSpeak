package io.piggydance.echospeak

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import io.piggydance.echospeak.audio.VadType
import io.piggydance.echospeak.auth.GoogleAuthManager
import io.piggydance.echospeak.auth.GoogleSignInOverlay

class MainActivity : ComponentActivity() {
    private val voiceEchoController = VoiceEchoController(
        vadType = VadType.YAMNET,
    )

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("echospeak_main", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // TODO: Google 登录暂时关闭，相关代码保留以备后续启用
            // 如需重新开启登录流程：
            //   1. 将下方 googleSignInDone 改为读取 SharedPreferences
            //   2. 取消注释 GoogleSignInOverlay 代码块
            //   3. 确保 GoogleAuthManager.WEB_CLIENT_ID 已正确配置
            @Suppress("ConstantConditionIf")
            val googleSignInDone = true // 暂时跳过登录，直接进入主界面

            OnboardingPermissionHandler {
                // 权限已授予，启动语音服务
                StartVoiceEchoEffect()
                App()

                // Google 登录引导（暂时关闭）
                // GoogleSignInOverlay(
                //     visible = !googleSignInDone,
                //     onSignInSuccess = {
                //         prefs.edit().putBoolean("google_sign_in_done", true).apply()
                //         googleSignInDone = true
                //     },
                //     onSkip = {
                //         prefs.edit().putBoolean("google_sign_in_done", true).apply()
                //         googleSignInDone = true
                //     },
                // )
            }
        }
    }

    @Composable
    private fun StartVoiceEchoEffect() {
        DisposableEffect(Unit) {
            try {
                voiceEchoController.start(peekAvailableContext())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            onDispose { }
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
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
