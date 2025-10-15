package io.piggydance.echospeak.application.tasks

import android.content.Context
import com.rousetime.android_startup.AndroidStartup

class StatsSdkTask : AndroidStartup<Unit>() {
    override fun create(context: Context) {

    }

    override fun callCreateOnMainThread() = false

    override fun waitOnMainThread() = false
}