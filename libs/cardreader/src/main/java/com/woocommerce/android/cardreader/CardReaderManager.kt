package com.woocommerce.android.cardreader

import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderStatus
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for consumers who want to start accepting POC card payments.
 */
@Suppress("TooManyFunctions")
interface CardReaderManager {
    val initialized: Boolean
    val readerStatus: StateFlow<CardReaderStatus>
    val softwareUpdateStatus: Flow<SoftwareUpdateStatus>
    val softwareUpdateAvailability: Flow<SoftwareUpdateAvailability>
    val batteryStatus: Flow<CardReaderBatteryStatus>
    val displayBluetoothCardReaderMessages: Flow<BluetoothCardReaderMessages>

    fun initialize()
    fun discoverReaders(
        isSimulated: Boolean,
        cardReaderTypesToDiscover: CardReaderTypesToDiscover,
    ): Flow<CardReaderDiscoveryEvents>

    fun startConnectionToReader(cardReader: CardReader, locationId: String)
    suspend fun disconnectReader(): Boolean

    suspend fun collectPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus>
    suspend fun refundInteracPayment(refundParams: RefundParams): Flow<CardInteracRefundStatus>

    suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus>
    fun cancelPayment(paymentData: PaymentData)

    suspend fun startAsyncSoftwareUpdate()
    suspend fun clearCachedCredentials()
    fun cancelOngoingFirmwareUpdate()
}
