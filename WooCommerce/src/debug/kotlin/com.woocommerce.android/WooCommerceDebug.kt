package com.woocommerce.android

import com.android.volley.VolleyLog
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import com.woocommerce.android.cardreader.CardReaderManagerFactory
import com.woocommerce.android.cardreader.CardReaderStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WooCommerceDebug : WooCommerce() {
    override val cardReaderManager = CardReaderManagerFactory.createCardReaderManager(object : CardReaderStore {
        override suspend fun getConnectionToken(): String {
            val result = payStore.fetchConnectionToken(selectedSite.get())
            return result.model?.token.orEmpty()
        }

        override suspend fun capturePaymentIntent(id: String): Boolean {
            return false
        }
    })

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
        super.onCreate()

        // Disables Volley debug logging on release build and prevents the "Marker added to finished log" crash
        // https://github.com/woocommerce/woocommerce-android/issues/817
        if (!BuildConfig.DEBUG) {
            VolleyLog.DEBUG = false
        }
    }
}
