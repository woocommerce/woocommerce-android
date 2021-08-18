package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.external.callable.ReaderDisplayListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.DisplayMessageRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.ReaderInputRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class CollectPaymentAction(private val terminal: TerminalWrapper, private val logWrapper: LogWrapper) {
    sealed class CollectPaymentStatus {
        data class DisplayMessageRequested(val msg: ReaderDisplayMessage) : CollectPaymentStatus()
        data class ReaderInputRequested(val options: ReaderInputOptions) : CollectPaymentStatus()
        data class Success(val paymentIntent: PaymentIntent) : CollectPaymentStatus()
        data class Failure(val exception: TerminalException) : CollectPaymentStatus()
    }

    fun collectPayment(paymentIntent: PaymentIntent): Flow<CollectPaymentStatus> {
        return callbackFlow {
            val cancelable = terminal.collectPaymentMethod(
                paymentIntent,
                object : ReaderDisplayListener {
                    override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
                        logWrapper.d("CardReader", message.toString())
                        this@callbackFlow.sendBlockingIfOpen(DisplayMessageRequested(message))
                    }

                    override fun onRequestReaderInput(options: ReaderInputOptions) {
                        logWrapper.d("CardReader", "Waiting for input: $options")
                        this@callbackFlow.sendBlockingIfOpen(ReaderInputRequested(options))
                    }
                },
                object : PaymentIntentCallback {
                    override fun onSuccess(paymentIntent: PaymentIntent) {
                        logWrapper.d("CardReader", "Payment collected")
                        this@callbackFlow.sendBlocking(Success(paymentIntent))
                        this@callbackFlow.close()
                    }

                    override fun onFailure(e: TerminalException) {
                        logWrapper.d("CardReader", "Payment collection failed")
                        this@callbackFlow.sendBlocking(Failure(e))
                        this@callbackFlow.close()
                    }
                }
            )
            awaitClose {
                if (!cancelable.isCompleted) cancelable.cancel(noop)
            }
        }
    }

    private fun <E> SendChannel<E>.sendBlockingIfOpen(element: E) {
        if (!isClosedForSend) sendBlocking(element)
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
