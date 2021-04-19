package com.woocommerce.android.cardreader.internal

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.DiscoveryListener
import com.stripe.stripeterminal.callable.ReaderCallback
import com.stripe.stripeterminal.callable.TerminalListener
import com.stripe.stripeterminal.log.LogLevel
import com.stripe.stripeterminal.model.external.ConnectionStatus
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTED
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTING
import com.stripe.stripeterminal.model.external.ConnectionStatus.NOT_CONNECTED
import com.stripe.stripeterminal.model.external.DeviceType
import com.stripe.stripeterminal.model.external.DiscoveryConfiguration
import com.stripe.stripeterminal.model.external.PaymentStatus
import com.stripe.stripeterminal.model.external.Reader
import com.stripe.stripeterminal.model.external.ReaderEvent
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStatus
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
    private val paymentManager: PaymentManager
) : CardReaderManager {
    companion object {
        private const val TAG = "CardReaderManager"
    }
    private lateinit var application: Application

    override val isInitialized: Boolean
        get() { return terminal.isInitialized() }

    @ExperimentalCoroutinesApi
    override val discoveryEvents: MutableStateFlow<CardReaderDiscoveryEvents> = MutableStateFlow(
        CardReaderDiscoveryEvents.NotStarted
    )

    override val readerStatus: MutableStateFlow<CardReaderStatus> = MutableStateFlow(CardReaderStatus.NOT_CONNECTED)
    private val foundReaders = mutableSetOf<Reader>()

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

    override fun startDiscovery(isSimulated: Boolean) {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        val config = DiscoveryConfiguration(0, DeviceType.CHIPPER_2X, isSimulated)
        discoveryEvents.value = CardReaderDiscoveryEvents.Started
        terminal.discoverReaders(config, object : DiscoveryListener {
            override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                foundReaders.addAll(readers)
                discoveryEvents.value = CardReaderDiscoveryEvents.ReadersFound(readers.mapNotNull { it.serialNumber })
            }
        }, object : Callback {
            override fun onFailure(e: TerminalException) {
                discoveryEvents.value = CardReaderDiscoveryEvents.Failed(e.toString())
            }

            override fun onSuccess() {}
        })
    }

    override fun connectToReader(readerId: String) {
        if (!terminal.isInitialized()) throw IllegalStateException("Terminal not initialized")
        foundReaders.find { it.serialNumber == readerId }?.let {
            terminal.connectToReader(it, object : ReaderCallback {
                override fun onFailure(e: TerminalException) {
                    logWrapper.d("CardReader", "connecting to reader failed: ${e.errorMessage}")
                }

                override fun onSuccess(reader: Reader) {
                    logWrapper.d("CardReader", "connecting to reader succeeded")
                }
            })
        } ?: logWrapper.e("CardReader", "Connecting to reader failed: reader not found")
    }

    override suspend fun collectPayment(amount: Int, currency: String): Flow<CardPaymentStatus> =
        paymentManager.acceptPayment(amount, currency)

    private fun initStripeTerminal(logLevel: LogLevel) {
        val listener = object : TerminalListener {
            override fun onUnexpectedReaderDisconnect(reader: Reader) {
                readerStatus.value = CardReaderStatus.NOT_CONNECTED
                logWrapper.d("CardReader", "onUnexpectedReaderDisconnect")
            }

            override fun onConnectionStatusChange(status: ConnectionStatus) {
                super.onConnectionStatusChange(status)
                readerStatus.value = when (status) {
                    NOT_CONNECTED -> CardReaderStatus.NOT_CONNECTED
                    CONNECTING -> CardReaderStatus.CONNECTING
                    CONNECTED -> CardReaderStatus.CONNECTED
                }
                logWrapper.d("CardReader", "onConnectionStatusChange: ${status.name}")
            }

            override fun onPaymentStatusChange(status: PaymentStatus) {
                super.onPaymentStatusChange(status)
                // TODO cardreader: Not Implemented
                logWrapper.d("CardReader", "onPaymentStatusChange: ${status.name}")
            }

            override fun onReportLowBatteryWarning() {
                super.onReportLowBatteryWarning()
                // TODO cardreader: Not Implemented
                logWrapper.d("CardReader", "onReportLowBatteryWarning")
            }

            override fun onReportReaderEvent(event: ReaderEvent) {
                super.onReportReaderEvent(event)
                // TODO cardreader: Not Implemented
                logWrapper.d("CardReader", "onReportReaderEvent: $event.name")
            }
        }
        terminal.initTerminal(application, logLevel, tokenProvider, listener)
    }
}
