package io.piggydance.echospeak.application

import android.app.Application
import android.util.Log
import com.rousetime.android_startup.StartupManager
import io.piggydance.echospeak.application.tasks.AdTask
import io.piggydance.echospeak.application.tasks.FinalTask
import io.piggydance.echospeak.application.tasks.KoinTask
import io.piggydance.echospeak.application.tasks.SplashDelayTask
import io.piggydance.echospeak.application.tasks.StatsSdkTask
import io.piggydance.echospeak.auth.GoogleAuthManager


const val TAG = "EchoSpeakApplication"

class EchoSpeakApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate enter")
        // 恢复 Google 登录状态（从 SharedPreferences 读取缓存的用户信息）
        GoogleAuthManager.restoreSession(this)

        StartupManager.Builder()
            .addStartup(AdTask())
            .addStartup(FinalTask())
            .addStartup(SplashDelayTask())
            .addStartup(StatsSdkTask())
            .addStartup(KoinTask())
            .build(this)
            .start()
            .await()
        Log.d(TAG, "onCreate exit")
    }
}
