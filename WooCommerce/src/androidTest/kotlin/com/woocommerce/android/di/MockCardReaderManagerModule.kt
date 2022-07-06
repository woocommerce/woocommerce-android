package com.woocommerce.android.di

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RefundParams
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CardReaderManagerModule::class]
)
@Module
class MockCardReaderManagerModule {
    @Suppress("EmptyFunctionBlock")
    @Provides
    @Singleton
    fun provideCardReaderManager() = object : CardReaderManager {
        private val cardReader = object : CardReader {
            override val id: String
                get() = "ADEE123"
            override val type: String
                get() = "reader"
            override val currentBatteryLevel: Float
                get() = 1f
            override val firmwareVersion: String
                get() = "1.0"
            override val locationId: String
                get() = "US"
        }

        override val initialized: Boolean
            get() = true
        override val readerStatus: StateFlow<CardReaderStatus>
            get() = MutableStateFlow(Connected(cardReader))
        override val softwareUpdateStatus: Flow<SoftwareUpdateStatus>
            get() = flowOf(SoftwareUpdateStatus.Success)
        override val softwareUpdateAvailability: Flow<SoftwareUpdateAvailability>
            get() = emptyFlow()
        override val batteryStatus: Flow<CardReaderBatteryStatus>
            get() = emptyFlow()
        override val displayBluetoothCardReaderMessages: Flow<BluetoothCardReaderMessages>
            get() = emptyFlow()

        override fun initialize() {
            // NO-OP
        }

        override fun discoverReaders(
            isSimulated: Boolean,
            cardReaderTypesToDiscover: CardReaderTypesToDiscover
        ): Flow<CardReaderDiscoveryEvents> {
            return emptyFlow()
        }

        override fun startConnectionToReader(cardReader: CardReader, locationId: String) {}

        override suspend fun disconnectReader(): Boolean = true

        override suspend fun collectPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus> =
            flowOf(CardPaymentStatus.CollectingPayment)

        override suspend fun refundInteracPayment(refundParams: RefundParams): Flow<CardInteracRefundStatus> {
            return emptyFlow()
        }

        override suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus> {
            return emptyFlow()
        }

        override fun cancelPayment(paymentData: PaymentData) {}

        override suspend fun startAsyncSoftwareUpdate() {}

        override suspend fun clearCachedCredentials() {}

        override fun cancelOngoingFirmwareUpdate() {}
    }
}
