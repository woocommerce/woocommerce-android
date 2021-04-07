package com.woocommerce.android.cardreader

import com.woocommerce.android.cardreader.internal.CardReaderManagerImpl
import com.woocommerce.android.cardreader.internal.TokenProvider
import com.woocommerce.android.cardreader.internal.connection.ConnectionManager
import com.woocommerce.android.cardreader.internal.payments.PaymentManager
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

object CardReaderManagerFactory {
    fun createCardReaderManager(cardReaderStore: CardReaderStore): CardReaderManager {
        val terminal = TerminalWrapper()
        val logWrapper = LogWrapper()
        return CardReaderManagerImpl(
            terminal,
            TokenProvider(cardReaderStore),
            logWrapper,
            PaymentManager(
                cardReaderStore,
                CreatePaymentAction(PaymentIntentParametersFactory(), terminal, logWrapper),
                CollectPaymentAction(terminal, logWrapper),
                ProcessPaymentAction(terminal, logWrapper)
            ),
            ConnectionManager(terminal, logWrapper)
        )
    }
}
