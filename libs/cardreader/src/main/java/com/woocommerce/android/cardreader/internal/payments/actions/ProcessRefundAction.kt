package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.RefundCallback
import com.stripe.stripeterminal.external.models.CollectRefundConfiguration
import com.stripe.stripeterminal.external.models.Refund
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class ProcessRefundAction(private val terminal: TerminalWrapper) {
    sealed class ProcessRefundStatus {
        data class Success(val refund: Refund) : ProcessRefundStatus()
        data class Failure(val exception: TerminalException) : ProcessRefundStatus()
    }

    fun processRefund(
        refundParameters: RefundParameters,
        refundConfiguration: CollectRefundConfiguration
    ): Flow<ProcessRefundStatus> {
        return callbackFlow {
            val cancelable = terminal.processRefund(
                refundParameters,
                refundConfiguration,
                object : RefundCallback {
                    override fun onSuccess(refund: Refund) {
                        trySend(ProcessRefundStatus.Success(refund))
                        close()
                    }

                    override fun onFailure(e: TerminalException) {
                        trySend(ProcessRefundStatus.Failure(e))
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
