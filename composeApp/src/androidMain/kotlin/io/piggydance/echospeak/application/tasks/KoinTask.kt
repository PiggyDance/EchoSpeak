package io.piggydance.echospeak.application.tasks

import android.content.Context
import com.rousetime.android_startup.AndroidStartup
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KoinTask : AndroidStartup<Unit>() {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            androidLogger()
            // 补充其他剩余的模块
        }
    }

    override fun callCreateOnMainThread() = true

    override fun waitOnMainThread() = true
}