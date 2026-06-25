package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import androidx.annotation.RequiresPermission
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.CollectPaymentIntentConfiguration
import com.stripe.stripeterminal.external.models.CollectRefundConfiguration
import com.stripe.stripeterminal.external.models.ConfirmPaymentIntentConfiguration
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderSupportResult
import com.stripe.stripeterminal.external.models.Refund
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.SimulateReaderUpdate
import com.stripe.stripeterminal.external.models.SimulatedCard
import com.stripe.stripeterminal.external.models.SimulatedCardType
import com.stripe.stripeterminal.external.models.SimulatorConfiguration
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration.Color
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.ktx.cancelPaymentIntent
import com.stripe.stripeterminal.ktx.collectPaymentMethod
import com.stripe.stripeterminal.ktx.confirmPaymentIntent
import com.stripe.stripeterminal.ktx.connectReader
import com.stripe.stripeterminal.ktx.createPaymentIntent
import com.stripe.stripeterminal.ktx.disconnectReader
import com.stripe.stripeterminal.ktx.discoverReaders
import com.stripe.stripeterminal.ktx.processPaymentIntent
import com.stripe.stripeterminal.ktx.processRefund
import com.stripe.stripeterminal.log.LogLevel
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Injectable wrapper for Stripe's Terminal object.
 */
internal class TerminalWrapper {
    fun isInitialized() = Terminal.isInitialized()
    fun getLifecycleObserver() = TerminalApplicationDelegateWrapper()
    fun initTerminal(
        application: Application,
        logLevel: LogLevel,
        tokenProvider: ConnectionTokenProvider,
        listener: TerminalListener
    ) = Terminal.init(application, logLevel, tokenProvider, listener, null)

    @RequiresPermission(
        anyOf = [
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        ],
    )
    fun discoverReaders(config: DiscoveryConfiguration): Flow<List<Reader>> =
        Terminal.getInstance().discoverReaders(config)

    suspend fun connectToReader(
        reader: Reader,
        configuration: ConnectionConfiguration.BluetoothConnectionConfiguration
    ): Reader = Terminal.getInstance().connectReader(reader, configuration)

    suspend fun connectToMobile(
        reader: Reader,
        configuration: ConnectionConfiguration.TapToPayConnectionConfiguration
    ): Reader = Terminal.getInstance().connectReader(reader, configuration)

    suspend fun disconnectReader() = Terminal.getInstance().disconnectReader()

    fun clearCachedCredentials() {
        Terminal.getInstance().clearCachedCredentials()
    }

    suspend fun createPaymentIntent(params: PaymentIntentParameters): PaymentIntent =
        Terminal.getInstance().createPaymentIntent(params, null)

    suspend fun retrievePaymentIntent(clientSecret: String): PaymentIntent =
        suspendCancellableCoroutine { cont ->
            Terminal.getInstance().retrievePaymentIntent(
                clientSecret,
                object : PaymentIntentCallback {
                    override fun onSuccess(paymentIntent: PaymentIntent) {
                        cont.resume(paymentIntent)
                    }

                    override fun onFailure(e: TerminalException) {
                        cont.resumeWithException(e)
                    }
                }
            )
        }

    suspend fun processPaymentIntent(paymentIntent: PaymentIntent): PaymentIntent =
        Terminal.getInstance().processPaymentIntent(
            paymentIntent,
            CollectPaymentIntentConfiguration.Builder().build(),
            ConfirmPaymentIntentConfiguration.Builder().build()
        )

    suspend fun collectPaymentMethod(paymentIntent: PaymentIntent): PaymentIntent =
        Terminal.getInstance().collectPaymentMethod(
            paymentIntent,
            CollectPaymentIntentConfiguration.Builder()
                .updatePaymentIntent(true)
                .build()
        )

    suspend fun confirmPaymentIntent(paymentIntent: PaymentIntent): PaymentIntent =
        Terminal.getInstance().confirmPaymentIntent(
            paymentIntent,
            ConfirmPaymentIntentConfiguration.Builder().build()
        )

    suspend fun cancelPayment(paymentIntent: PaymentIntent): PaymentIntent =
        Terminal.getInstance().cancelPaymentIntent(paymentIntent)

    suspend fun processRefund(
        refundParameters: RefundParameters,
        refundConfiguration: CollectRefundConfiguration
    ): Refund = Terminal.getInstance().processRefund(refundParameters, refundConfiguration)

    fun installSoftwareUpdate() = Terminal.getInstance().installAvailableUpdate()

    fun getConnectedReader(): CardReader? = Terminal.getInstance().connectedReader?.let { CardReaderImpl(it) }

    fun setupSimulator(
        updateFrequency: CardReaderManager.SimulatorUpdateFrequency,
        useInterac: Boolean,
        useEftpos: Boolean,
    ) {
        Terminal.getInstance().simulatorConfiguration = SimulatorConfiguration(
            update = mapFrequencyOptions(updateFrequency),
            simulatedCard = SimulatedCard(
                when {
                    useEftpos -> SimulatedCardType.EFTPOS_AU_DEBIT
                    useInterac -> SimulatedCardType.INTERAC
                    else -> SimulatedCardType.VISA
                }
            )
        )
    }

    private fun mapFrequencyOptions(updateFrequency: CardReaderManager.SimulatorUpdateFrequency): SimulateReaderUpdate {
        return when (updateFrequency) {
            CardReaderManager.SimulatorUpdateFrequency.NEVER -> SimulateReaderUpdate.NONE
            CardReaderManager.SimulatorUpdateFrequency.ALWAYS -> SimulateReaderUpdate.REQUIRED
            CardReaderManager.SimulatorUpdateFrequency.LOW_BATTERY_ERROR -> SimulateReaderUpdate.LOW_BATTERY
            CardReaderManager.SimulatorUpdateFrequency.LOW_BATTERY_SUCCEED_CONNECT -> {
                SimulateReaderUpdate.LOW_BATTERY_SUCCEED_CONNECT
            }
            CardReaderManager.SimulatorUpdateFrequency.RANDOM -> SimulateReaderUpdate.RANDOM
            CardReaderManager.SimulatorUpdateFrequency.OPTIONAL_UPDATE_AVAILABLE ->
                SimulateReaderUpdate.UPDATE_AVAILABLE
        }
    }

    fun supportsReadersOfType(
        deviceType: DeviceType,
        discoveryConfiguration: DiscoveryConfiguration,
    ): ReaderSupportResult = Terminal.getInstance().supportsReadersOfType(deviceType, discoveryConfiguration)

    fun setupTapToPayUx(config: CardReaderManager.TapToPayUxConfig) {
        val uxConfig = TapToPayUxConfiguration.Builder()
            .tapZone(TapToPayUxConfiguration.TapZone.Default)
            .colors(
                TapToPayUxConfiguration.ColorScheme.Builder()
                    .primary(Color.Resource(config.primaryColor))
                    .success(Color.Resource(config.successColor))
                    .error(Color.Resource(config.errorColor))
                    .build()
            )
            .darkMode(
                if (config.isDarkMode) {
                    TapToPayUxConfiguration.DarkMode.DARK
                } else {
                    TapToPayUxConfiguration.DarkMode.LIGHT
                }
            )
            .build()

        Terminal.getInstance().setTapToPayUxConfiguration(uxConfig)
    }
}
