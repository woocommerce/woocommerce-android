package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.cardreader.CardReaderStatus.NotConnected
import com.woocommerce.android.cardreader.PaymentData
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.SoftwareUpdateStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CardReaderModule {
    @Provides
    @Singleton
    fun provideCardReaderManager(
        selectedSite: SelectedSite,
        payStore: WCPayStore,
        responseMapper: CapturePaymentResponseMapper
    ): CardReaderManager {
        return CardReaderManagerFactory.createCardReaderManager(object : CardReaderStore {
            override suspend fun getConnectionToken(): String {
                val result = payStore.fetchConnectionToken(selectedSite.get())
                return result.model?.token.orEmpty()
            }

            override suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse {
                val response = payStore.capturePayment(selectedSite.get(), paymentId, orderId)
                return responseMapper.mapResponse(response)
            }
        })
    }
}
