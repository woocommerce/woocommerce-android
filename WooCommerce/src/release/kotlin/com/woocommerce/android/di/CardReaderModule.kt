package com.woocommerce.android.di

import com.woocommerce.android.cardreader.CardReaderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.annotation.Nullable
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CardReaderModule {
    @Provides
    @Nullable
    @Singleton
    fun provideCardReaderManager(): CardReaderManager = object : CardReaderManager {
        override val isInitialized: Boolean = false
        override val readerStatus: StateFlow<CardReaderStatus> = MutableStateFlow(CardReaderStatus.NotConnected)

        override fun initialize(app: Application) {}

        override fun discoverReaders(isSimulated: Boolean): Flow<CardReaderDiscoveryEvents> = flow {}

        override suspend fun connectToReader(cardReader: CardReader): Boolean = false

        override suspend fun collectPayment(
            orderId: Long,
            amount: BigDecimal,
            currency: String
        ): Flow<CardPaymentStatus> = flow {}

        override suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus> =
            flow {}

        override suspend fun updateSoftware(): Flow<SoftwareUpdateStatus> = flow {}
    }
}
