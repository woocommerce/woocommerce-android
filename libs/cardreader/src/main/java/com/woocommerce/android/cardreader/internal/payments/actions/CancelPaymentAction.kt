package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class CancelPaymentAction(private val terminal: TerminalWrapper) {
    @OptIn(DelicateCoroutinesApi::class)
    fun cancelPayment(paymentIntent: PaymentIntent) {
        // Usage of GlobalScope is intentional since the app should always try to cancel the payment intent
        GlobalScope.launch {
            runCatching { terminal.cancelPayment(paymentIntent) }
        }
    }
}
