package com.woocommerce.android.cardreader.internal

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.stripe.stripeterminal.log.LogLevel
import com.woocommerce.android.cardreader.BuildConfig
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.connection.TerminalListenerImpl
import com.woocommerce.android.cardreader.internal.firmware.SoftwareUpdateManager
import com.woocommerce.android.cardreader.internal.payments.InteracRefundManager
import com.woocommerce.android.cardreader.internal.payments.PaymentManager
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RefundParams
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of CardReaderManager using StripeTerminalSDK.
 */
@Suppress("LongParameterList")
internal class CardReaderManagerImpl(
    private var application: Application,
    private val terminal: TerminalWrapper,
    private val tokenProvider: TokenProvider,
    private val logWrapper: LogWrapper,
    private val paymentManager: PaymentManager,
    private val interacRefundManager: InteracRefundManager,
    private val connectionManager: ConnectionManager,
    private val softwareUpdateManager: SoftwareUpdateManager,
    private val terminalListener: TerminalListenerImpl,
) : CardReaderManager {
    companion object {
        private const val TAG = "CardReaderManager"
    }

    override val initialized: Boolean
        get() {
            return terminal.isInitialized()
        }

    override val readerStatus = terminalListener.readerStatus

    override val softwareUpdateStatus = connectionManager.softwareUpdateStatus

    override val softwareUpdateAvailability = connectionManager.softwareUpdateAvailability

    override val batteryStatus = connectionManager.batteryStatus

    override val displayBluetoothCardReaderMessages = connectionManager.displayBluetoothCardReaderMessages

    override fun initialize() {
        if (!terminal.isInitialized()) {
            terminal.getLifecycleObserver().onCreate(application)

            application.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) {}

                override fun onLowMemory() {}

                override fun onTrimMemory(level: Int) {
                    terminal.getLifecycleObserver().onTrimMemory(application, level)
                }
            })

            val logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.ERROR

            initStripeTerminal(logLevel)

            terminal.setupSimulator()
        } else {
            logWrapper.w(TAG, "CardReaderManager is already initialized")
        }
    }

    override fun discoverReaders(
        isSimulated: Boolean,
        cardReaderTypesToDiscover: CardReaderTypesToDiscover,
    ): Flow<CardReaderDiscoveryEvents> {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        return connectionManager.discoverReaders(isSimulated, cardReaderTypesToDiscover)
    }

    override fun startConnectionToReader(cardReader: CardReader, locationId: String) {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        connectionManager.startConnectionToReader(cardReader, locationId)
    }

    override suspend fun disconnectReader(): Boolean {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        if (terminal.getConnectedReader() == null) return false
        return connectionManager.disconnectReader()
    }

    override suspend fun collectPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus> {
        resetBluetoothDisplayMessage()
        return paymentManager.acceptPayment(paymentInfo)
    }

    override suspend fun refundInteracPayment(refundParams: RefundParams): Flow<CardInteracRefundStatus> {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        resetBluetoothDisplayMessage()
        return interacRefundManager.refundInteracPayment(refundParams)
    }

    private fun resetBluetoothDisplayMessage() {
        connectionManager.resetBluetoothCardReaderDisplayMessage()
    }

    override suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus> =
        paymentManager.retryPayment(orderId, paymentData)

    override fun cancelPayment(paymentData: PaymentData) = paymentManager.cancelPayment(paymentData)

    private fun initStripeTerminal(logLevel: LogLevel) {
        terminal.initTerminal(application, logLevel, tokenProvider, terminalListener)
    }

    override suspend fun startAsyncSoftwareUpdate() {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        softwareUpdateManager.startAsyncSoftwareUpdate()
    }

    override suspend fun clearCachedCredentials() {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        terminal.clearCachedCredentials()
    }

    override fun cancelOngoingFirmwareUpdate() {
        softwareUpdateManager.cancelOngoingFirmwareUpdate()
    }
}
