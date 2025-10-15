package io.piggydance.echospeak.application.tasks

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.rousetime.android_startup.AndroidStartup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 广告SDK加载任务
class AdTask : AndroidStartup<Unit>() {
    override fun create(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(context)
        }
    }

    override fun callCreateOnMainThread() = false

    override fun waitOnMainThread() = false
}
