package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.models.CollectRefundConfiguration
import com.stripe.stripeterminal.external.models.Refund
import com.stripe.stripeterminal.external.models.RefundParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

internal class ProcessRefundAction(private val terminal: TerminalWrapper) {
    sealed class ProcessRefundStatus {
        data class Success(val refund: Refund) : ProcessRefundStatus()
        data class Failure(val exception: TerminalException) : ProcessRefundStatus()
    }

    suspend fun processRefund(
        refundParameters: RefundParameters,
        refundConfiguration: CollectRefundConfiguration
    ): ProcessRefundStatus {
        return try {
            val refund = terminal.processRefund(refundParameters, refundConfiguration)
            ProcessRefundStatus.Success(refund)
        } catch (e: TerminalException) {
            ProcessRefundStatus.Failure(e)
        }
    }
}
