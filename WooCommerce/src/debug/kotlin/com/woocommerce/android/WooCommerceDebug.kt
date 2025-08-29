package com.woocommerce.android

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.webkit.WebView
import com.woocommerce.android.apifaker.ApiFakerUiHelper
import com.woocommerce.android.util.SystemVersionUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WooCommerceDebug : WooCommerce() {
    @Inject
    lateinit var apiFakerUiHelper: ApiFakerUiHelper

    override fun onCreate() {
        enableWebContentDebugging()
        super.onCreate()
        enableStrictMode()
        apiFakerUiHelper.attachToApplication(this)
    }

    /**
     * enables "strict mode" for testing
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )

        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .apply {
                    if (SystemVersionUtils.isAtLeastP()) {
                        detectNonSdkApiUsage()
                    }
                }
                .build()
        )
        WooLog.w(T.UTILS, "Strict mode enabled")
    }

    /**
     * Tap 2 pay Stripe library uses webview, and apparently they also enable debugging of it (at least in the current
     * beta version). This method changes directory where logs are stored, otherwise it crashes with
     * Caused by: java.lang.RuntimeException: Using WebView from more than one process at once with the same data
     * directory is not supported. https://crbug.com/558377 :
     * Current process com.stripe.cots.aidlservice (pid 7378), lock owner com.woocommerce.android (pid 6427)
     *
     * https://developer.android.com/reference/android/webkit/WebView.html#setDataDirectorySuffix(java.lang.String)
     * > This means that different processes in the same application cannot directly share WebView-related data,
     * > since the data directories must be distinct.
     */
    private fun enableWebContentDebugging() {
        if (SystemVersionUtils.isAtLeastP()) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }
        WebView.setWebContentsDebuggingEnabled(true)
    }
}
