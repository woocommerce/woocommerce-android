package com.woocommerce.android.app

import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping.withDefaults
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader

class WooCommerceWearDebug : WooCommerceWear() {
    override fun onCreate() {
        if (FlipperUtils.shouldEnableFlipper(this)) {
            SoLoader.init(this, false)
            AndroidFlipperClient.getInstance(this).apply {
                addPlugin(InspectorFlipperPlugin(applicationContext, withDefaults()))
                addPlugin(NetworkFlipperPlugin())
                addPlugin(DatabasesFlipperPlugin(this@WooCommerceWearDebug))
                addPlugin(SharedPreferencesFlipperPlugin(this@WooCommerceWearDebug))
            }.start()
        }

        super.onCreate()
    }
}
