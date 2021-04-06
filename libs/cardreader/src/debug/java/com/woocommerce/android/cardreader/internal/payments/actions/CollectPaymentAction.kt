package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.callable.Callback
import com.stripe.stripeterminal.callable.PaymentIntentCallback
import com.stripe.stripeterminal.callable.ReaderDisplayListener
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.ReaderDisplayMessage
import com.stripe.stripeterminal.model.external.ReaderInputOptions
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.DisplayMessageRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.ReaderInputRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.Success
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.TerminalFailure
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.TimedOut
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout

private const val TIMEOUT = 45000L

@ExperimentalCoroutinesApi
internal class CollectPaymentAction(private val terminal: TerminalWrapper, private val logWrapper: LogWrapper) {
    sealed class CollectPaymentStatus {
        data class DisplayMessageRequested(val msg: ReaderDisplayMessage) : CollectPaymentStatus()
        data class ReaderInputRequested(val options: ReaderInputOptions) : CollectPaymentStatus()
        data class Success(val paymentIntent: PaymentIntent) : CollectPaymentStatus()
        data class TerminalFailure(val exception: TerminalException) : CollectPaymentStatus()
        object TimedOut : CollectPaymentStatus()
    }

    fun collectPayment(paymentIntent: PaymentIntent): Flow<CollectPaymentStatus> {
        return callbackFlow<CollectPaymentStatus> {
            val cancelable = terminal.collectPaymentMethod(paymentIntent, object : ReaderDisplayListener {
                override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
                    logWrapper.d("CardReader", message.toString())
                    this@callbackFlow.sendBlocking(DisplayMessageRequested(message))
                }

                override fun onRequestReaderInput(options: ReaderInputOptions) {
                    logWrapper.d("CardReader", "Waiting for input: $options")
                    this@callbackFlow.sendBlocking(ReaderInputRequested(options))
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
                        this@callbackFlow.sendBlocking(TerminalFailure(e))
                        this@callbackFlow.close()
                    }
                })
            awaitClose {
                if (!cancelable.isCompleted) {
                    cancelable.cancel(noopCallback)
                }
            }
        }.timeout(TIMEOUT) {
            it.sendBlocking(TimedOut)
        }
    }

    /**
     * Wrapper for a coroutine which cancels the coroutine if it doesn't complete within `timeout`.
     *
     * `onTimeout` can be used to clean up resources or to inform the caller about the timeout.
     */
    private fun <T> Flow<T>.timeout(timeout: Long, onTimeout: suspend (ProducerScope<T>) -> Unit) =
        channelFlow {
            try {
                withTimeout(timeout) {
                    collect { send(it) }
                }
            } catch (e: TimeoutCancellationException) {
                onTimeout(this)
            }
        }
}

private val noopCallback = object : Callback {
    override fun onFailure(e: TerminalException) {
        // no-op
    }

    override fun onSuccess() {
        // no-op
    }
}
