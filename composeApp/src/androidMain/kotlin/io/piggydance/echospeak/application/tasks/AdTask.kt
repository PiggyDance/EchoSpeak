package io.piggydance.echospeak.application.tasks

import android.content.Context
import com.rousetime.android_startup.AndroidStartup

// 广告SDK加载任务
class AdTask : AndroidStartup<Unit>() {
    override fun create(context: Context) {

    }

    override fun callCreateOnMainThread() = true

    override fun waitOnMainThread() = true
}
