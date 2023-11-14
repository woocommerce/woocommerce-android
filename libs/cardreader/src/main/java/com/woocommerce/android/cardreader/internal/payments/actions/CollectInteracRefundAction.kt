package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class CollectInteracRefundAction(private val terminal: TerminalWrapper) {
    sealed class CollectInteracRefundStatus {
        object Success : CollectInteracRefundStatus()
        data class Failure(val exception: TerminalException) : CollectInteracRefundStatus()
    }

    fun collectRefund(refundParameters: RefundParameters): Flow<CollectInteracRefundStatus> {
        return callbackFlow {
            val cancelable = terminal.refundPayment(
                refundParameters,
                object : Callback {
                    override fun onSuccess() {
                        trySend(CollectInteracRefundStatus.Success)
                        close()
                    }

                    override fun onFailure(e: TerminalException) {
                        trySend(CollectInteracRefundStatus.Failure(e))
                        close()
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
