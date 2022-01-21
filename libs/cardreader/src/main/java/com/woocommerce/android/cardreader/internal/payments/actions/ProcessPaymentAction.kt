package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus.Success
import com.woocommerce.android.cardreader.internal.sendAndLog
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class ProcessPaymentAction(private val terminal: TerminalWrapper, private val logWrapper: LogWrapper) {
    sealed class ProcessPaymentStatus {
        data class Success(val paymentIntent: PaymentIntent) : ProcessPaymentStatus()
        data class Failure(val exception: TerminalException) : ProcessPaymentStatus()
    }

    fun processPayment(paymentIntent: PaymentIntent): Flow<ProcessPaymentStatus> {
        return callbackFlow {
            terminal.processPayment(
                paymentIntent,
                object : PaymentIntentCallback {
                    override fun onSuccess(paymentIntent: PaymentIntent) {
                        logWrapper.d(LOG_TAG, "Processing payment succeeded")
                        this@callbackFlow.sendAndLog(Success(paymentIntent), logWrapper)
                        this@callbackFlow.close()
                    }

                    override fun onFailure(exception: TerminalException) {
                        logWrapper.e(
                            LOG_TAG,
                            "Processing payment failed. " +
                                "Message: ${exception.errorMessage}, DeclineCode: ${exception.apiError?.declineCode}"
                        )
                        this@callbackFlow.sendAndLog(Failure(exception), logWrapper)
                        this@callbackFlow.close()
                    }
                }
            )
            awaitClose()
        }
    }
}
