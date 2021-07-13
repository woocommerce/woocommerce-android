package com.woocommerce.android.cardreader.internal

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.stripe.stripeterminal.log.LogLevel
import com.woocommerce.android.cardreader.BuildConfig
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.cardreader.PaymentData
import com.woocommerce.android.cardreader.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.firmware.SoftwareUpdateManager
import com.woocommerce.android.cardreader.internal.payments.PaymentManager
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

/**
 * Implementation of CardReaderManager using StripeTerminalSDK.
 */
internal class CardReaderManagerImpl(
    private val terminal: TerminalWrapper,
    private val tokenProvider: TokenProvider,
    private val logWrapper: LogWrapper,
    private val paymentManager: PaymentManager,
    private val connectionManager: ConnectionManager,
    private val softwareUpdateManager: SoftwareUpdateManager
) : CardReaderManager {
    companion object {
        private const val TAG = "CardReaderManager"
    }

    private lateinit var application: Application

    override val isInitialized: Boolean
        get() {
            return terminal.isInitialized()
        }

    override val readerStatus: MutableStateFlow<CardReaderStatus> = connectionManager.readerStatus

    override fun initialize(app: Application) {
        if (!terminal.isInitialized()) {
            application = app

            // Register the observer for all lifecycle hooks
            app.registerActivityLifecycleCallbacks(terminal.getLifecycleObserver())

            app.registerComponentCallbacks(object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) {}

                override fun onLowMemory() {}

                override fun onTrimMemory(level: Int) {
                    terminal.getLifecycleObserver().onTrimMemory(level, application)
                }
            })

            val logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.ERROR

            initStripeTerminal(logLevel)
        } else {
            logWrapper.w(TAG, "CardReaderManager is already initialized")
        }
    }

    override fun discoverReaders(isSimulated: Boolean): Flow<CardReaderDiscoveryEvents> {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        return connectionManager.discoverReaders(isSimulated)
    }

    override suspend fun connectToReader(cardReader: CardReader): Boolean {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        return connectionManager.connectToReader(cardReader)
    }

    override suspend fun disconnectReader(): Boolean {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        if (terminal.getConnectedReader() == null) return false
        return connectionManager.disconnectReader()
    }

    override suspend fun collectPayment(
        paymentDescription: String,
        orderId: Long,
        amount: BigDecimal,
        currency: String,
        customerEmail: String?
    ): Flow<CardPaymentStatus> =
        paymentManager.acceptPayment(paymentDescription, orderId, amount, currency, customerEmail)

    override suspend fun retryCollectPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus> =
        paymentManager.retryPayment(orderId, paymentData)

    override fun cancelPayment(paymentData: PaymentData) = paymentManager.cancelPayment(paymentData)

    private fun initStripeTerminal(logLevel: LogLevel) {
        terminal.initTerminal(application, logLevel, tokenProvider, connectionManager)
    }

    override suspend fun softwareUpdateAvailability(): Flow<SoftwareUpdateAvailability> =
        softwareUpdateManager.softwareUpdateStatus()

    override suspend fun updateSoftware(): Flow<SoftwareUpdateStatus> = softwareUpdateManager.updateSoftware()

    override suspend fun clearCachedCredentials() {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        terminal.clearCachedCredentials()
    }
}
