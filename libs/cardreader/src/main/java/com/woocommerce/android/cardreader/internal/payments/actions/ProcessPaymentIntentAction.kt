package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction.ProcessPaymentIntentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction.ProcessPaymentIntentStatus.Success
import com.woocommerce.android.cardreader.internal.sendAndLog
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class ProcessPaymentIntentAction(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper
) {
    sealed class ProcessPaymentIntentStatus {
        data class Success(val paymentIntent: PaymentIntent) : ProcessPaymentIntentStatus()
        data class Failure(val exception: TerminalException) : ProcessPaymentIntentStatus()
    }

    fun processPaymentIntent(paymentIntent: PaymentIntent): Flow<ProcessPaymentIntentStatus> {
        return callbackFlow {
            logWrapper.d(LOG_TAG, "Processing payment intent")
            val cancelable = terminal.processPaymentIntent(
                paymentIntent,
                object : PaymentIntentCallback {
                    override fun onSuccess(paymentIntent: PaymentIntent) {
                        logWrapper.d(LOG_TAG, "Processing payment intent succeeded")
                        this@callbackFlow.sendAndLog(Success(paymentIntent), logWrapper)
                        this@callbackFlow.close()
                    }

                    override fun onFailure(e: TerminalException) {
                        logWrapper.e(
                            LOG_TAG,
                            "Processing payment intent failed. " +
                                "Message: ${e.errorMessage}, DeclineCode: ${e.apiError?.declineCode}"
                        )
                        this@callbackFlow.sendAndLog(Failure(e), logWrapper)
                        this@callbackFlow.close()
                    }
                }
            )
            awaitClose {
                if (!cancelable.isCompleted) cancelable.cancel(noop)
            }
        }
    }
}

private val noop = object : Callback {
    override fun onFailure(e: TerminalException) = Unit

    override fun onSuccess() = Unit
}
