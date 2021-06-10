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
    fun provideCardReaderManager(): CardReaderManager = object : CardReaderManager {
        override val isInitialized: Boolean = false
        override val readerStatus: StateFlow<CardReaderStatus> = MutableStateFlow(NotConnected)

        override fun initialize(app: Application) {}

        override fun discoverReaders(isSimulated: Boolean): Flow<CardReaderDiscoveryEvents> = flow {}

        override suspend fun connectToReader(cardReader: CardReader): Boolean = false
        override suspend fun disconnectReader(): Boolean = false

        override suspend fun collectPayment(
            paymentDescription: String,
            orderId: Long,
            amount: BigDecimal,
            currency: String,
            customerEmail: String?
        ): Flow<CardPaymentStatus> = flow {}

        override suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus> =
            flow {}

        override suspend fun updateSoftware(): Flow<SoftwareUpdateStatus> = flow {}

        override suspend fun softwareUpdateAvailability(): Flow<SoftwareUpdateAvailability> = flow {}

        override suspend fun clearCachedCredentials() {}
    }
}
