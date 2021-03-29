package com.woocommerce.android.cardreader.internal

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.util.Log
import com.stripe.stripeterminal.callable.TerminalListener
import com.stripe.stripeterminal.log.LogLevel
import com.stripe.stripeterminal.model.external.ConnectionStatus
import com.stripe.stripeterminal.model.external.PaymentStatus
import com.stripe.stripeterminal.model.external.Reader
import com.stripe.stripeterminal.model.external.ReaderEvent
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

/**
 * Implementation of CardReaderManager using StripeTerminalSDK.
 */
internal class CardReaderManagerImpl(
    private val terminal: TerminalWrapper,
    private val tokenProvider: TokenProvider,
    private val logWrapper: LogWrapper
) : CardReaderManager {
    companion object {
        private const val TAG = "CardReaderManager"
    }
    private lateinit var application: Application

    override val isInitialized: Boolean
        get() {
            return terminal.isInitialized()
        }

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

    private fun initStripeTerminal(logLevel: LogLevel) {
        val listener = object : TerminalListener {
            override fun onUnexpectedReaderDisconnect(reader: Reader) {
                // TODO cardreader: Not Implemented
                Log.d("CardReader", "onUnexpectedReaderDisconnect")
            }

            override fun onConnectionStatusChange(status: ConnectionStatus) {
                super.onConnectionStatusChange(status)
                // TODO cardreader: Not Implemented
                Log.d("CardReader", "onConnectionStatusChange: ${status.name}")
            }

            override fun onPaymentStatusChange(status: PaymentStatus) {
                super.onPaymentStatusChange(status)
                // TODO cardreader: Not Implemented
                Log.d("CardReader", "onPaymentStatusChange: ${status.name}")
            }

            override fun onReportLowBatteryWarning() {
                super.onReportLowBatteryWarning()
                // TODO cardreader: Not Implemented
                Log.d("CardReader", "onReportLowBatteryWarning")
            }

            override fun onReportReaderEvent(event: ReaderEvent) {
                super.onReportReaderEvent(event)
                // TODO cardreader: Not Implemented
                Log.d("CardReader", "onReportReaderEvent: $event.name")
            }
        }
        terminal.initTerminal(application, logLevel, tokenProvider, listener)
    }
}
