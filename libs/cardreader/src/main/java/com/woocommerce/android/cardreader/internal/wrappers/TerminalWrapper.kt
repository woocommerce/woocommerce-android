package com.woocommerce.android.cardreader.internal.wrappers

import android.app.Application
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.BluetoothReaderListener
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.RefundCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.SimulateReaderUpdate
import com.stripe.stripeterminal.external.models.SimulatorConfiguration
import com.stripe.stripeterminal.log.LogLevel
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

    fun discoverReaders(
        config: DiscoveryConfiguration,
        discoveryListener: DiscoveryListener,
        callback: Callback
    ): Cancelable =
        Terminal.getInstance().discoverReaders(config, discoveryListener, callback)

    fun connectToReader(
        reader: Reader,
        configuration: ConnectionConfiguration.BluetoothConnectionConfiguration,
        callback: ReaderCallback,
        listener: BluetoothReaderListener
    ) = Terminal.getInstance().connectBluetoothReader(reader, configuration, listener, callback)

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
        Terminal.getInstance().processPayment(paymentIntent, callback)

    fun cancelPayment(paymentIntent: PaymentIntent, callback: PaymentIntentCallback) =
        Terminal.getInstance().cancelPaymentIntent(paymentIntent, callback)

    fun refundPayment(refundParameters: RefundParameters, callback: Callback) =
        Terminal.getInstance().collectRefundPaymentMethod(refundParameters, callback)

    fun processRefund(callback: RefundCallback) =
        Terminal.getInstance().processRefund(callback)

    fun installSoftwareUpdate() = Terminal.getInstance().installAvailableUpdate()

    fun getConnectedReader(): CardReader? = Terminal.getInstance().connectedReader?.let { CardReaderImpl(it) }

    fun setupSimulator() {
        Terminal.getInstance().simulatorConfiguration = SimulatorConfiguration(
            update = SimulateReaderUpdate.RANDOM
        )
    }
}
