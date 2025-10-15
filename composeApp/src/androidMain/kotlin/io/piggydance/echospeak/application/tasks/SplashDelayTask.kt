package io.piggydance.echospeak.application.tasks

import android.content.Context
import com.rousetime.android_startup.AndroidStartup

/**
 * APP启动后,启动此延迟任务. 保证Splash闪屏页面至少展示一秒钟.
 */
class SplashDelayTask : AndroidStartup<Unit>() {
    override fun callCreateOnMainThread() = false

    override fun waitOnMainThread() = false

    override fun create(context: Context) {
        // 延迟1秒
        Thread.sleep(1000)
    }
}
