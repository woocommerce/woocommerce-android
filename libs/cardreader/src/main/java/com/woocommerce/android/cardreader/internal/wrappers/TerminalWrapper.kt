package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import androidx.annotation.RequiresPermission
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.CollectPaymentIntentConfiguration
import com.stripe.stripeterminal.external.models.CollectRefundConfiguration
import com.stripe.stripeterminal.external.models.ConfirmPaymentIntentConfiguration
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.LocaleConfig
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.ProcessRefundParameters
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderSupportResult
import com.stripe.stripeterminal.external.models.Refund
import com.stripe.stripeterminal.external.models.SimulatedCard
import com.stripe.stripeterminal.external.models.SimulatedCardType
import com.stripe.stripeterminal.external.models.SimulatorConfiguration
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration.Color
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import java.util.concurrent.atomic.AtomicReference
import com.stripe.stripeterminal.ktx.cancelPaymentIntent
import com.stripe.stripeterminal.ktx.connectReader
import com.stripe.stripeterminal.ktx.createPaymentIntent
import com.stripe.stripeterminal.ktx.disconnectReader
import com.stripe.stripeterminal.ktx.discoverReaders
import com.stripe.stripeterminal.ktx.processRefund
import com.stripe.stripeterminal.log.LogLevel
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Injectable wrapper for Stripe's Terminal object.
 */
internal class TerminalWrapper(private val terminal: () -> Terminal = { Terminal.getInstance() }) {
    fun isInitialized() = Terminal.isInitialized()
    fun getLifecycleObserver() = TerminalApplicationDelegateWrapper()
    fun initTerminal(
        application: Application,
        logLevel: LogLevel,
        tokenProvider: ConnectionTokenProvider,
        listener: TerminalListener
    ) = Terminal.init(
        application,
        logLevel,
        tokenProvider,
        listener,
        null,
        LocaleConfig.CardLanguagePreferenceIfAvailable
    )

    @RequiresPermission(
        anyOf = [
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        ],
    )
    fun discoverReaders(config: DiscoveryConfiguration): Flow<List<Reader>> =
        terminal().discoverReaders(config)

    suspend fun connectToReader(
        reader: Reader,
        configuration: ConnectionConfiguration
    ): Reader = terminal().connectReader(reader, configuration)

    suspend fun connectToMobile(
        reader: Reader,
        configuration: ConnectionConfiguration.TapToPayConnectionConfiguration
    ): Reader = terminal().connectReader(reader, configuration)

    suspend fun disconnectReader() = terminal().disconnectReader()

    fun clearCachedCredentials() {
        terminal().clearCachedCredentials()
    }

    suspend fun createPaymentIntent(params: PaymentIntentParameters): PaymentIntent =
        terminal().createPaymentIntent(params, null)

    suspend fun retrievePaymentIntent(clientSecret: String): PaymentIntent =
        suspendCancellableCoroutine { cont ->
            terminal().retrievePaymentIntent(
                clientSecret,
                object : PaymentIntentCallback {
                    override fun onSuccess(paymentIntent: PaymentIntent) {
                        if (cont.isActive) cont.resume(paymentIntent)
                    }

                    override fun onFailure(e: TerminalException) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }
            )
        }

    private val activePaymentCommand = AtomicReference<PaymentCommand?>(null)

    suspend fun processPaymentIntent(paymentIntent: PaymentIntent): PaymentIntent = paymentCommand { callback ->
        terminal().processPaymentIntent(
            paymentIntent,
            CollectPaymentIntentConfiguration.Builder().build(),
            ConfirmPaymentIntentConfiguration.Builder().build(),
            callback,
        )
    }

    suspend fun collectPaymentMethod(paymentIntent: PaymentIntent): PaymentIntent = paymentCommand { callback ->
        terminal().collectPaymentMethod(
            paymentIntent,
            callback,
            CollectPaymentIntentConfiguration.Builder().updatePaymentIntent(true).build(),
        )
    }

    suspend fun confirmPaymentIntent(paymentIntent: PaymentIntent): PaymentIntent = paymentCommand { callback ->
        terminal().confirmPaymentIntent(
            paymentIntent,
            callback,
            ConfirmPaymentIntentConfiguration.Builder().build(),
        )
    }

    suspend fun cancelPaymentCommandAndAwait(): Boolean {
        val command = activePaymentCommand.get() ?: return true
        val handle = command.handle ?: return false
        val cancelled = suspendCancellableCoroutine { continuation ->
            handle.cancel(object : Callback {
                override fun onSuccess() { if (continuation.isActive) continuation.resume(true) }
                override fun onFailure(e: TerminalException) { if (continuation.isActive) continuation.resume(false) }
            })
        }
        if (cancelled) command.finished.await()
        return cancelled
    }

    private suspend fun paymentCommand(start: (PaymentIntentCallback) -> Cancelable): PaymentIntent =
        suspendCancellableCoroutine { continuation ->
            val command = PaymentCommand()
            if (!activePaymentCommand.compareAndSet(null, command)) {
                continuation.resumeWithException(TerminalException(TerminalErrorCode.READER_BUSY, "Payment command pending"))
                return@suspendCancellableCoroutine
            }
            command.handle = start(object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    completePaymentCommand(command)
                    if (continuation.isActive) continuation.resume(paymentIntent)
                }
                override fun onFailure(e: TerminalException) {
                    completePaymentCommand(command)
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            })
            continuation.invokeOnCancellation {
                command.handle?.cancel(object : Callback {
                    override fun onSuccess() = Unit
                    override fun onFailure(e: TerminalException) = Unit
                })
            }
        }

    private fun completePaymentCommand(command: PaymentCommand) {
        activePaymentCommand.compareAndSet(command, null)
        command.finished.complete(Unit)
    }

    private class PaymentCommand {
        @Volatile var handle: Cancelable? = null
        val finished = CompletableDeferred<Unit>()
    }

    suspend fun cancelPayment(paymentIntent: PaymentIntent): PaymentIntent =
        terminal().cancelPaymentIntent(paymentIntent)

    suspend fun processRefund(
        refundParameters: ProcessRefundParameters,
        refundConfiguration: CollectRefundConfiguration
    ): Refund = terminal().processRefund(refundParameters, refundConfiguration)

    fun installSoftwareUpdate() = terminal().installAvailableUpdate()

    fun getConnectedReader(): CardReader? = terminal().connectedReader?.let { CardReaderImpl(it) }

    fun setupSimulator(
        useInterac: Boolean,
        useEftpos: Boolean,
    ) {
        terminal().simulatorConfiguration = SimulatorConfiguration(
            simulatedCard = SimulatedCard(
                when {
                    useEftpos -> SimulatedCardType.EFTPOS_AU_DEBIT
                    useInterac -> SimulatedCardType.INTERAC
                    else -> SimulatedCardType.VISA
                }
            )
        )
    }

    fun supportsReadersOfType(
        deviceType: DeviceType,
        discoveryConfiguration: DiscoveryConfiguration,
    ): ReaderSupportResult = terminal().supportsReadersOfType(deviceType, discoveryConfiguration)

    fun setupTapToPayUx(config: CardReaderManager.TapToPayUxConfig) {
        val uxConfig = TapToPayUxConfiguration.Builder()
            .tapZone(TapToPayUxConfiguration.TapZone.Default)
            .colorScheme(
                TapToPayUxConfiguration.ColorScheme.Builder()
                    .primary(Color.Resource(config.primaryColor))
                    .success(Color.Resource(config.successColor))
                    .error(Color.Resource(config.errorColor))
                    .build()
            )
            .theme(
                if (config.isDarkMode) {
                    TapToPayUxConfiguration.Theme.DARK
                } else {
                    TapToPayUxConfiguration.Theme.LIGHT
                }
            )
            .build()

        terminal().setTapToPayUxConfiguration(uxConfig)
    }
}
