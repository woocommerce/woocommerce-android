package com.woocommerce.android.cardreader

import com.woocommerce.android.cardreader.internal.CardReaderManagerImpl
import com.woocommerce.android.cardreader.internal.TokenProvider
import com.woocommerce.android.cardreader.internal.connection.BluetoothReaderListenerImpl
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.connection.TerminalListenerImpl
import com.woocommerce.android.cardreader.internal.connection.UpdateErrorMapper
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.firmware.SoftwareUpdateManager
import com.woocommerce.android.cardreader.internal.payments.AdditionalInfoMapper
import com.woocommerce.android.cardreader.internal.payments.PaymentErrorMapper
import com.woocommerce.android.cardreader.internal.payments.PaymentManager
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.cardreader.internal.payments.actions.CancelPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

object CardReaderManagerFactory {
    fun createCardReaderManager(cardReaderStore: CardReaderStore, logWrapper: LogWrapper): CardReaderManager {
        val terminal = TerminalWrapper()
        val batteryLevelProvider = { terminal.getConnectedReader()?.currentBatteryLevel }
        val bluetoothReaderListener = BluetoothReaderListenerImpl(
            logWrapper,
            AdditionalInfoMapper(),
            UpdateErrorMapper(batteryLevelProvider)
        )
        val terminalListener = TerminalListenerImpl(logWrapper)

        return CardReaderManagerImpl(
            terminal,
            TokenProvider(cardReaderStore),
            logWrapper,
            PaymentManager(
                terminal,
                cardReaderStore,
                CreatePaymentAction(PaymentIntentParametersFactory(), terminal, PaymentUtils(), logWrapper),
                CollectPaymentAction(terminal, logWrapper),
                ProcessPaymentAction(terminal, logWrapper),
                CancelPaymentAction(terminal),
                PaymentUtils(),
                PaymentErrorMapper(),
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
            terminalListener
        )
    }
}
