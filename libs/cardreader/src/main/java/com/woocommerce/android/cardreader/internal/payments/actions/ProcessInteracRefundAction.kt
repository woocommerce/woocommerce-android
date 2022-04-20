package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.RefundCallback
import com.stripe.stripeterminal.external.models.Refund
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class ProcessInteracRefundAction(private val terminal: TerminalWrapper) {
    sealed class ProcessRefundStatus {
        data class Success(val refund: Refund) : ProcessRefundStatus()
        data class Failure(val exception: TerminalException) : ProcessRefundStatus()
    }

    fun processRefund(): Flow<ProcessRefundStatus> {
        return callbackFlow {
            terminal.processRefund(object : RefundCallback {
                override fun onSuccess(refund: Refund) {
                    trySend(ProcessRefundStatus.Success(refund))
                    close()
                }

                override fun onFailure(e: TerminalException) {
                    trySend(ProcessRefundStatus.Failure(e))
                    close()
                }
            })
            awaitClose()
        }
    }
}
