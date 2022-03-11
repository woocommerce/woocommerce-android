package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class CollectRefundAction(private val terminal: TerminalWrapper) {
    sealed class CollectRefundStatus {
        object Success : CollectRefundStatus()
        data class Failure(val exception: TerminalException) : CollectRefundStatus()
    }

    fun collectRefund(refundParameters: RefundParameters): Flow<CollectRefundStatus> {
        return callbackFlow {
            terminal.refundPayment(
                refundParameters,
                object : Callback {
                    override fun onSuccess() {
                        trySend(CollectRefundStatus.Success)
                    }

                    override fun onFailure(e: TerminalException) {
                        trySend(CollectRefundStatus.Failure(e))
                    }
                }
            )
        }
    }
}
