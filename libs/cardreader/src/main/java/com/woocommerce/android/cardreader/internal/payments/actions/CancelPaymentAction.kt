package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class CancelPaymentAction(private val terminal: TerminalWrapper) {
    fun cancelPayment(paymentIntent: PaymentIntent) {
        // Usage of GlobalScope is intentional since the app should always try to cancel the payment intent
        GlobalScope.launch {
            terminal.cancelPayment(paymentIntent, noopCallback)
        }
    }
}

private val noopCallback = object : PaymentIntentCallback {
    override fun onFailure(e: TerminalException) {
        // noop
    }

    override fun onSuccess(paymentIntent: PaymentIntent) {
        // noop
    }
}
