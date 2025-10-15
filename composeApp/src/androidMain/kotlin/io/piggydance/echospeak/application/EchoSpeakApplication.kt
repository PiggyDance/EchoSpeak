package io.piggydance.echospeak.application

import android.app.Application
import android.util.Log
import com.rousetime.android_startup.StartupManager
import io.piggydance.echospeak.application.tasks.AdTask
import io.piggydance.echospeak.application.tasks.FinalTask
import io.piggydance.echospeak.application.tasks.SplashDelayTask
import io.piggydance.echospeak.application.tasks.StatsSdkTask


const val TAG = "EchoSpeakApplication"

class EchoSpeakApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "onCreate enter")
        StartupManager.Builder()
            .addStartup(AdTask())
            .addStartup(FinalTask())
            .addStartup(SplashDelayTask())
            .addStartup(StatsSdkTask())
            .build(this)
            .start()
            .await()
        Log.d(TAG, "onCreate exit")
    }
}
