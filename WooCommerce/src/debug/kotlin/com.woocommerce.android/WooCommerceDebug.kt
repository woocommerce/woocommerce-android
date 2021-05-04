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
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.cardreader.CardReaderManagerFactory
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.HiltAndroidApp
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Inject

@HiltAndroidApp
class WooCommerceDebug : WooCommerce() {
    @Inject lateinit var payStore: WCPayStore
    @Inject lateinit var selectedSite: SelectedSite

    override val cardReaderManager = CardReaderManagerFactory.createCardReaderManager(object : CardReaderStore {
        override suspend fun getConnectionToken(): String {
            val result = payStore.fetchConnectionToken(selectedSite.get())
            return result.model?.token.orEmpty()
        }

        override suspend fun capturePaymentIntent(orderId: Long, paymentId: String): Boolean {
            return !payStore.capturePayment(selectedSite.get(), paymentId, orderId).isError
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
        enableStrictMode()
    }

    /**
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    private fun enableStrictMode() {
        // return if the build is not a debug build
        if (!BuildConfig.DEBUG) {
            WooLog.e(T.UTILS, "You should not call enableStrictMode() on a non debug build")
            return
        }

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
                .build()
        )
        WooLog.w(T.UTILS, "Strict mode enabled")
    }
}
