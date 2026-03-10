package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Success
import com.woocommerce.android.cardreader.internal.sendAndLog
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class CollectPaymentAction(private val terminal: TerminalWrapper, private val logWrapper: LogWrapper) {
    sealed class CollectPaymentStatus {
        data class Success(val paymentIntent: PaymentIntent) : CollectPaymentStatus()
        data class Failure(val exception: TerminalException) : CollectPaymentStatus()
    }

    fun collectPayment(paymentIntent: PaymentIntent): Flow<CollectPaymentStatus> {
        return callbackFlow {
            val cancelable = terminal.collectPaymentMethod(
                paymentIntent,
                object : PaymentIntentCallback {
                    override fun onSuccess(paymentIntent: PaymentIntent) {
                        logWrapper.d(LOG_TAG, "Payment collected")
                        this@callbackFlow.sendAndLog(Success(paymentIntent), logWrapper)
                        this@callbackFlow.close()
                    }

                    override fun onFailure(e: TerminalException) {
                        logWrapper.d(LOG_TAG, "Payment collection failed")
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
    override fun onFailure(e: TerminalException) {
        // noop
    }

    override fun onSuccess() {
        // noop
    }
}
