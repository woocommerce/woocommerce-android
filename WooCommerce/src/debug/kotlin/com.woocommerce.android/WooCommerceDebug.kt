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
import com.woocommerce.android.cardreader.CardReaderManagerFactory
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.HiltAndroidApp
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.SERVER_ERROR
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

        override suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse {
            val response = payStore.capturePayment(selectedSite.get(), paymentId, orderId)
            return when (response.error?.type) {
                null -> CapturePaymentResponse.SUCCESS
                GENERIC_ERROR -> CapturePaymentResponse.GENERIC_ERROR
                PAYMENT_ALREADY_CAPTURED -> CapturePaymentResponse.PAYMENT_ALREADY_CAPTURED
                MISSING_ORDER -> CapturePaymentResponse.MISSING_ORDER
                CAPTURE_ERROR -> CapturePaymentResponse.CAPTURE_ERROR
                SERVER_ERROR -> CapturePaymentResponse.SERVER_ERROR
                NETWORK_ERROR -> CapturePaymentResponse.NETWORK_ERROR
            }
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
                .build()
        )
        WooLog.w(T.UTILS, "Strict mode enabled")
    }
}
