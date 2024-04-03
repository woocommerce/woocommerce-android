package com.woocommerce.android

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.webkit.WebView
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import com.woocommerce.android.util.SystemVersionUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WooCommerceDebug : WooCommerce() {
    override fun onCreate() {
        if (FlipperUtils.shouldEnableFlipper(this)) {
            SoLoader.init(this, false)
            AndroidFlipperClient.getInstance(this).apply {
                addPlugin(InspectorFlipperPlugin(applicationContext, DescriptorMapping.withDefaults()))
                addPlugin(NetworkFlipperPlugin())
                addPlugin(DatabasesFlipperPlugin(this@WooCommerceDebug))
                addPlugin(SharedPreferencesFlipperPlugin(this@WooCommerceDebug))
            }.start()
        }

        enableWebContentDebugging()
        super.onCreate()
        enableStrictMode()
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
