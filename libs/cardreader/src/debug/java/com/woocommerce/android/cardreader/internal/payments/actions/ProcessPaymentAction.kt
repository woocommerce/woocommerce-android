package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.callable.PaymentIntentCallback
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class ProcessPaymentAction(private val terminal: TerminalWrapper, private val logWrapper: LogWrapper) {
    sealed class ProcessPaymentStatus {
        data class Success(val paymentIntent: PaymentIntent) : ProcessPaymentStatus()
        data class Failure(val exception: TerminalException) : ProcessPaymentStatus()
    }

    fun processPayment(paymentIntent: PaymentIntent): Flow<ProcessPaymentStatus> {
        return callbackFlow {
            terminal.processPayment(paymentIntent, object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    logWrapper.d("CardReader", "Processing payment succeeded")
                    this@callbackFlow.sendBlocking(Success(paymentIntent))
                    this@callbackFlow.close()
                }

                override fun onFailure(exception: TerminalException) {
                    logWrapper.d("CardReader", "Processing payment failed")
                    this@callbackFlow.sendBlocking(Failure(exception))
                    this@callbackFlow.close()
                }
            })
            awaitClose()
        }
    }
}
