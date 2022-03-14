package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class CollectRefundAction(private val terminal: TerminalWrapper) {
    sealed class CollectRefundStatus {
        object Success : CollectRefundStatus()
        data class Failure(val exception: TerminalException) : CollectRefundStatus()
    }

    fun collectRefund(refundParameters: RefundParameters): Flow<CollectRefundStatus> {
        return callbackFlow {
            val cancelable = terminal.refundPayment(
                refundParameters,
                object : Callback {
                    override fun onSuccess() {
                        trySend(CollectRefundStatus.Success)
                        close()
                    }

                    override fun onFailure(e: TerminalException) {
                        trySend(CollectRefundStatus.Failure(e))
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
