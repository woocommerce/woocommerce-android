package com.woocommerce.android.cardreader.internal

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.stripe.stripeterminal.log.LogLevel
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.payments.PaymentManager
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Implementation of CardReaderManager using StripeTerminalSDK.
 */
@ExperimentalCoroutinesApi
internal class CardReaderManagerImpl(
    private val terminal: TerminalWrapper,
    private val tokenProvider: TokenProvider,
    private val logWrapper: LogWrapper,
    private val paymentManager: PaymentManager,
    private val connectionManager: ConnectionManager
) : CardReaderManager {
    companion object {
        private const val TAG = "CardReaderManager"
    }
    private lateinit var application: Application

    override val isInitialized: Boolean
        get() { return terminal.isInitialized() }

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

            // TODO cardreader: Set LogLevel depending on build flavor.
            // Choose the level of messages that should be logged to your console
            val logLevel = LogLevel.VERBOSE

            initStripeTerminal(logLevel)
        } else {
            logWrapper.w(TAG, "CardReaderManager is already initialized")
        }
    }

    override fun startDiscovery(isSimulated: Boolean): Flow<CardReaderDiscoveryEvents> {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        return connectionManager.startDiscovery(isSimulated)
    }

    override fun connectToReader(readerId: String) {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        connectionManager.connectToReader(readerId)
    }

    override suspend fun collectPayment(amount: Int, currency: String): Flow<CardPaymentStatus> =
        paymentManager.acceptPayment(amount, currency)

    private fun initStripeTerminal(logLevel: LogLevel) {
        terminal.initTerminal(application, logLevel, tokenProvider, connectionManager)
    }
}
