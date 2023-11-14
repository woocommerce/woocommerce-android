package com.woocommerce.android.cardreader

import android.app.Application
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.internal.CardReaderManagerImpl
import com.woocommerce.android.cardreader.internal.TokenProvider
import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
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
import com.woocommerce.android.cardreader.internal.payments.actions.CollectInteracRefundAction
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessInteracRefundAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
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
        val batteryLevelProvider = { terminal.getConnectedReader()?.currentBatteryLevel }
        val bluetoothReaderListener = BluetoothReaderListenerImpl(
            logWrapper,
            AdditionalInfoMapper(),
            UpdateErrorMapper(batteryLevelProvider)
        )
        val terminalListener = TerminalListenerImpl(logWrapper)
        val cardReaderConfigFactory = CardReaderConfigFactory()

        return CardReaderManagerImpl(
            application,
            terminal,
            TokenProvider(cardReaderStore),
            logWrapper,
            PaymentManager(
                terminal,
                cardReaderStore,
                CreatePaymentAction(
                    PaymentIntentParametersFactory(PaymentMethodTypeMapper()),
                    terminal,
                    logWrapper,
                    cardReaderConfigFactory,
                    PaymentUtils
                ),
                CollectPaymentAction(terminal, logWrapper),
                ProcessPaymentAction(terminal, logWrapper),
                CancelPaymentAction(terminal),
                PaymentUtils,
                PaymentErrorMapper(),
                cardReaderConfigFactory
            ),
            InteracRefundManager(
                CollectInteracRefundAction(terminal),
                ProcessInteracRefundAction(terminal),
                RefundErrorMapper(),
                PaymentUtils
            ),
            ConnectionManager(
                terminal,
                bluetoothReaderListener,
                DiscoverReadersAction(terminal, logWrapper),
                terminalListener,
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
