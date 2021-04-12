package com.woocommerce.android

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
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponentDebug

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

    override val component: AppComponent by lazy {
        DaggerAppComponentDebug.builder()
            .application(this)
            .build()
    }

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
    }
}
