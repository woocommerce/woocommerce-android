package com.woocommerce.android.di

import com.woocommerce.android.cardreader.CardReaderManagerFactory
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CapturePaymentResponseMapper
import com.woocommerce.android.util.WooLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CardReaderModule {
    @Provides
    @Singleton
    fun provideCardReaderManager(
        cardReaderStore: CardReaderStore,
        logWrapper: LogWrapper
    ) = CardReaderManagerFactory.createCardReaderManager(cardReaderStore, logWrapper)

    @Provides
    @Singleton
    fun provideCardReaderStore(
        selectedSite: SelectedSite,
        payStore: WCPayStore,
        responseMapper: CapturePaymentResponseMapper
    ) = object : CardReaderStore {
        override suspend fun getConnectionToken(): String {
            val result = payStore.fetchConnectionToken(selectedSite.get())
            return result.model?.token.orEmpty()
        }

        override suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse {
            val response = payStore.capturePayment(selectedSite.get(), paymentId, orderId)
            return responseMapper.mapResponse(response)
        }
    }

    @Provides
    @Singleton
    fun provideLogWrapper() = object : LogWrapper {
        private val TAG = WooLog.T.CARD_READER

        override fun w(tag: String, message: String) {
            WooLog.w(TAG, "$tag: $message")
        }

        override fun d(tag: String, message: String) {
            WooLog.d(TAG, "$tag: $message")
        }

        override fun e(tag: String, message: String) {
            WooLog.d(TAG, "$tag: $message")
        }
    }
}
