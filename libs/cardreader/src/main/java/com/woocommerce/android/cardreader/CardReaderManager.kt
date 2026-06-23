package com.woocommerce.android.cardreader

import androidx.annotation.ColorRes
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.CompositeConnectionTokenProvider
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CreatePaymentIntentResult
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RefundConfig
import com.woocommerce.android.cardreader.payments.RefundParams
import com.woocommerce.android.cardreader.payments.RetrieveAndCollectResult
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
    val connectionTokenProvider: CompositeConnectionTokenProvider

    fun initialize(
        updateFrequency: SimulatorUpdateFrequency,
        useInterac: Boolean,
        useEftpos: Boolean,
        isDebug: Boolean,
    )

    fun reinitializeSimulatedTerminal(
        updateFrequency: SimulatorUpdateFrequency,
        useInterac: Boolean,
        useEftpos: Boolean,
    )

    fun discoverReaders(
        isSimulated: Boolean,
        cardReaderTypesToDiscover: CardReaderTypesToDiscover,
    ): Flow<CardReaderDiscoveryEvents>

    fun setupTapToPayUx(config: TapToPayUxConfig)

    /**
     * Checks whether the Stripe Terminal SDK considers this device capable of acting as a
     * Tap-to-Pay reader (e.g. has a Trusted Execution Environment and hardware-backed key
     * attestation). Returns [TapToPaySupportResult.TerminalNotInitialized] if Terminal has
     * not been initialized yet — callers should fall back to their pre-init heuristics.
     */
    fun isTapToPaySupportedOnDevice(isSimulated: Boolean): TapToPaySupportResult

    suspend fun startConnectionToReader(cardReader: CardReader, locationId: String)
    suspend fun disconnectReader(): Boolean
    fun cancelReconnection()

    suspend fun collectPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus>

    /**
     * Creates a Stripe PaymentIntent without collecting or capturing it. Used by the tablet
     * side of the remote Tap-to-Pay flow, where the phone does the collect + process step
     * and the tablet captures on completion.
     */
    suspend fun createPaymentIntent(paymentInfo: PaymentInfo): CreatePaymentIntentResult

    /**
     * Retrieves a PaymentIntent by its client secret and drives Stripe `processPaymentIntent`
     * (collect + confirm) on the locally connected reader. Used by the phone side of the remote
     * Tap-to-Pay flow. The returned status is the PaymentIntent's status after processing —
     * typically `requires_capture`.
     */
    suspend fun retrieveAndCollectPayment(clientSecret: String, paymentInfo: PaymentInfo): RetrieveAndCollectResult

    suspend fun refundInteracPayment(
        refundParams: RefundParams,
        refundConfig: RefundConfig
    ): Flow<CardInteracRefundStatus>

    suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus>
    fun cancelPayment(paymentData: PaymentData)

    suspend fun startAsyncSoftwareUpdate()
    suspend fun clearCachedCredentials()
    fun cancelOngoingFirmwareUpdate()

    enum class SimulatorUpdateFrequency {
        NEVER,
        ALWAYS,
        LOW_BATTERY_ERROR,
        LOW_BATTERY_SUCCEED_CONNECT,
        RANDOM,
        OPTIONAL_UPDATE_AVAILABLE,
    }

    data class TapToPayUxConfig(
        @ColorRes val primaryColor: Int,
        @ColorRes val successColor: Int,
        @ColorRes val errorColor: Int,
        val isDarkMode: Boolean,
    )
}
