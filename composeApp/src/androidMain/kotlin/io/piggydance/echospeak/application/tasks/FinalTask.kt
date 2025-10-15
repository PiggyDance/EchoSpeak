package io.piggydance.echospeak.application.tasks

import android.content.Context
import com.rousetime.android_startup.AndroidStartup
import com.rousetime.android_startup.Startup

/**
 * 所有任务完成后, 执行此任务.
 */
class FinalTask : AndroidStartup<Unit>() {
    override fun callCreateOnMainThread() = true

    override fun waitOnMainThread() = true

    override fun dependencies(): List<Class<out Startup<*>>>? {
        return listOf(SplashDelayTask::class.java)
    }

    override fun create(context: Context) {
        // 所有任务完成后, 执行此任务.
    }
}
