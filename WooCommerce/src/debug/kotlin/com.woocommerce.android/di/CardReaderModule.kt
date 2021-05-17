package com.woocommerce.android.di

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderManagerFactory
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CardReaderModule {
    @Provides
    @Singleton
    fun provideCardReaderManager(selectedSite: SelectedSite, payStore: WCPayStore): CardReaderManager {
        return CardReaderManagerFactory.createCardReaderManager(object : CardReaderStore {
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
    }
}
