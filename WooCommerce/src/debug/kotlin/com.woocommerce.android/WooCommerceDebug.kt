package com.woocommerce.android

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
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
        if (FlipperUtils.shouldEnableFlipper(this) &&
            SystemVersionUtils.isAtLeastM()
        ) {
            SoLoader.init(this, false)
            AndroidFlipperClient.getInstance(this).apply {
                addPlugin(InspectorFlipperPlugin(applicationContext, DescriptorMapping.withDefaults()))
                addPlugin(NetworkFlipperPlugin())
                addPlugin(DatabasesFlipperPlugin(this@WooCommerceDebug))
                addPlugin(SharedPreferencesFlipperPlugin(this@WooCommerceDebug))
            }.start()
        }
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
}
