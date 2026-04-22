package com.woocommerce.android.cardreader

import android.app.Application
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.connection.CompositeConnectionTokenProvider
import com.woocommerce.android.cardreader.internal.CardReaderManagerImpl
import com.woocommerce.android.cardreader.internal.TokenProvider
import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.connection.TapToPayReaderListenerImpl
import com.woocommerce.android.cardreader.internal.connection.TerminalListenerImpl
import com.woocommerce.android.cardreader.internal.connection.UpdateErrorMapper
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.firmware.SoftwareUpdateManager
import com.woocommerce.android.cardreader.internal.payments.AdditionalInfoMapper
import com.woocommerce.android.cardreader.internal.payments.InteracRefundManager
import com.woocommerce.android.cardreader.internal.payments.PaymentErrorMapper
import com.woocommerce.android.cardreader.internal.payments.PaymentManager
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.cardreader.internal.payments.RefundErrorMapper
import com.woocommerce.android.cardreader.internal.payments.actions.CancelPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessRefundAction
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.PaymentMethodTypeMapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

object CardReaderManagerFactory {
    fun createCardReaderManager(
        application: Application,
        cardReaderStore: CardReaderStore,
        logWrapper: LogWrapper
    ): CardReaderManager {
        val terminal = TerminalWrapper()
        val terminalListener = TerminalListenerImpl(logWrapper)
        val batteryLevelProvider = { terminal.getConnectedReader()?.currentBatteryLevel }
        val bluetoothReaderListener = BluetoothReaderListenerImpl(
            logWrapper,
            AdditionalInfoMapper(),
            UpdateErrorMapper(batteryLevelProvider),
            terminalListener
        )
        val tapToPayReaderListener = TapToPayReaderListenerImpl(logWrapper, terminalListener)
        val cardReaderConfigFactory = CardReaderConfigFactory()
        val paymentUtils = PaymentUtils(logWrapper)
        val compositeTokenProvider = CompositeConnectionTokenProvider(TokenProvider(cardReaderStore))

        return CardReaderManagerImpl(
            application,
            terminal,
            compositeTokenProvider,
            logWrapper,
            PaymentManager(
                terminal,
                cardReaderStore,
                CreatePaymentAction(
                    PaymentIntentParametersFactory(PaymentMethodTypeMapper()),
                    terminal,
                    logWrapper,
                    cardReaderConfigFactory,
                    paymentUtils,
                ),
                ProcessPaymentIntentAction(terminal, logWrapper),
                CancelPaymentAction(terminal),
                paymentUtils,
                PaymentErrorMapper(),
                cardReaderConfigFactory
            ),
            InteracRefundManager(
                ProcessRefundAction(terminal),
                RefundErrorMapper(),
                paymentUtils,
            ),
            ConnectionManager(
                terminal,
                bluetoothReaderListener,
                tapToPayReaderListener,
                DiscoverReadersAction(terminal, logWrapper),
                terminalListener,
                application,
                logWrapper,
            ),
            SoftwareUpdateManager(
                terminal,
                bluetoothReaderListener,
                logWrapper,
            ),
            terminalListener,
        )
    }
}
