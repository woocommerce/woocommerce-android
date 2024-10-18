package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import androidx.annotation.RequiresPermission
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.ReaderListener
import com.stripe.stripeterminal.external.callable.RefundCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.RefundConfiguration
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.SimulateReaderUpdate
import com.stripe.stripeterminal.external.models.SimulatedCard
import com.stripe.stripeterminal.external.models.SimulatedCardType
import com.stripe.stripeterminal.external.models.SimulatorConfiguration
import com.stripe.stripeterminal.log.LogLevel
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderImpl

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
    ) = Terminal.initTerminal(application, logLevel, tokenProvider, listener)

    @RequiresPermission(
        anyOf = [
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        ],
    )
    fun discoverReaders(
        config: DiscoveryConfiguration,
        discoveryListener: DiscoveryListener,
        callback: Callback
    ): Cancelable = Terminal.getInstance().discoverReaders(config, discoveryListener, callback)

    fun connectToReader(
        reader: Reader,
        configuration: ConnectionConfiguration.BluetoothConnectionConfiguration,
        callback: ReaderCallback,
        listener: ReaderListener
    ) = Terminal.getInstance().connectBluetoothReader(reader, configuration, listener, callback)

    fun connectToMobile(
        reader: Reader,
        configuration: ConnectionConfiguration.LocalMobileConnectionConfiguration,
        callback: ReaderCallback
    ) = Terminal.getInstance().connectLocalMobileReader(reader, configuration, callback)

    fun disconnectReader(callback: Callback) =
        Terminal.getInstance().disconnectReader(callback)

    fun clearCachedCredentials() = Terminal.getInstance().clearCachedCredentials()

    fun createPaymentIntent(params: PaymentIntentParameters, callback: PaymentIntentCallback) =
        Terminal.getInstance().createPaymentIntent(params, callback)

    fun collectPaymentMethod(
        paymentIntent: PaymentIntent,
        callback: PaymentIntentCallback
    ): Cancelable = Terminal.getInstance().collectPaymentMethod(paymentIntent, callback)

    fun processPayment(paymentIntent: PaymentIntent, callback: PaymentIntentCallback) =
        Terminal.getInstance().confirmPaymentIntent(paymentIntent, callback)

    fun cancelPayment(paymentIntent: PaymentIntent, callback: PaymentIntentCallback) =
        Terminal.getInstance().cancelPaymentIntent(paymentIntent, callback)

    fun refundPayment(
        refundParameters: RefundParameters,
        refundConfiguration: RefundConfiguration,
        callback: Callback
    ) = Terminal.getInstance().collectRefundPaymentMethod(refundParameters, refundConfiguration, callback)

    fun processRefund(callback: RefundCallback) =
        Terminal.getInstance().confirmRefund(callback)

    fun installSoftwareUpdate() = Terminal.getInstance().installAvailableUpdate()

    fun getConnectedReader(): CardReader? = Terminal.getInstance().connectedReader?.let { CardReaderImpl(it) }

    fun setupSimulator(updateFrequency: CardReaderManager.SimulatorUpdateFrequency, useInterac: Boolean) {
        Terminal.getInstance().simulatorConfiguration = SimulatorConfiguration(
            update = mapFrequencyOptions(updateFrequency),
            simulatedCard = SimulatedCard(if (useInterac) SimulatedCardType.INTERAC else SimulatedCardType.VISA)
        )
    }

    private fun mapFrequencyOptions(updateFrequency: CardReaderManager.SimulatorUpdateFrequency): SimulateReaderUpdate {
        return when (updateFrequency) {
            CardReaderManager.SimulatorUpdateFrequency.NEVER -> SimulateReaderUpdate.NONE
            CardReaderManager.SimulatorUpdateFrequency.ALWAYS -> SimulateReaderUpdate.LOW_BATTERY
            CardReaderManager.SimulatorUpdateFrequency.RANDOM -> SimulateReaderUpdate.RANDOM
        }
    }
}
