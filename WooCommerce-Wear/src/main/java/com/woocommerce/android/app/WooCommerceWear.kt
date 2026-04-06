package com.woocommerce.android.app

import android.app.Application
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import javax.inject.Inject

@HiltAndroidApp
open class WooCommerceWear : Application() {

    @Inject
    lateinit var crashLogging: Lazy<CrashLogging>

    override fun onCreate() {
        super.onCreate()
        crashLogging.get().initialize()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnexpectedError(event: OnUnexpectedError) {
        with(event) {
            crashLogging.get().sendReport(exception = exception, message = "FluxC: ${exception.message}: $description")
        }
    }
}
