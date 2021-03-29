package com.woocommerce.android.cardreader.internal

import android.app.Application
import android.util.Log
import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.DiscoveryListener
import com.stripe.stripeterminal.callable.TerminalListener
import com.stripe.stripeterminal.log.LogLevel
import com.stripe.stripeterminal.model.external.ConnectionStatus
import com.stripe.stripeterminal.model.external.DeviceType
import com.stripe.stripeterminal.model.external.DiscoveryConfiguration
import com.stripe.stripeterminal.model.external.PaymentStatus
import com.stripe.stripeterminal.model.external.Reader
import com.stripe.stripeterminal.model.external.ReaderEvent
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Implementation of CardReaderManager using StripeTerminalSDK.
 */
internal class CardReaderManagerImpl(
    private val terminal: TerminalWrapper,
    private val tokenProvider: TokenProvider
) : CardReaderManager {
    private lateinit var application: Application

    @ExperimentalCoroutinesApi
    override val discoveryEvents: MutableStateFlow<CardReaderDiscoveryEvents> = MutableStateFlow(
        CardReaderDiscoveryEvents.NotStarted
    )

    override fun isInitialized(): Boolean {
        return terminal.isInitialized()
    }

    override fun initialize(app: Application) {
        if (!terminal.isInitialized()) {
            application = app

            // Register the observer for all lifecycle hooks
            app.registerActivityLifecycleCallbacks(terminal.getLifecycleObserver())

            // TODO cardreader: Set LogLevel depending on build flavor.
            // Choose the level of messages that should be logged to your console
            val logLevel = LogLevel.VERBOSE

            initStripeTerminal(logLevel)
        }
    }

    override fun startDiscovery(isSimulated: Boolean) {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        val config = DiscoveryConfiguration(0, DeviceType.CHIPPER_2X, isSimulated)
        discoveryEvents.value = CardReaderDiscoveryEvents.Started
        terminal.discoverReaders(config, object : DiscoveryListener {
            override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                discoveryEvents.value = CardReaderDiscoveryEvents.ReadersFound(readers.mapNotNull { it.serialNumber })
            }
        }, object : Callback {
            override fun onFailure(e: TerminalException) {
                discoveryEvents.value = CardReaderDiscoveryEvents.Failed(e.toString())
            }

            override fun onSuccess() {}
        })
    }

    override fun onTrimMemory(level: Int) {
        if (terminal.isInitialized()) {
            terminal.getLifecycleObserver().onTrimMemory(level, application)
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
