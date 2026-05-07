package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.internal.payments.actions.ProcessRefundAction
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.RefundConfig
import com.woocommerce.android.cardreader.payments.RefundParams
import com.woocommerce.android.cardreader.payments.toStripeRefundConfiguration
import com.woocommerce.android.cardreader.payments.toStripeRefundParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class InteracRefundManager(
    private val processRefundAction: ProcessRefundAction,
    private val refundErrorMapper: RefundErrorMapper,
    private val paymentsUtils: PaymentUtils,
) {
    fun refundInteracPayment(
        refundParameters: RefundParams,
        refundConfig: RefundConfig,
    ): Flow<CardInteracRefundStatus> = flow {
        emit(CardInteracRefundStatus.CollectingInteracRefund)
        val status = processRefundAction.processRefund(
            refundParameters.toStripeRefundParameters(paymentsUtils),
            refundConfig.toStripeRefundConfiguration()
        )
        when (status) {
            is ProcessRefundAction.ProcessRefundStatus.Success -> {
                emit(CardInteracRefundStatus.InteracRefundSuccess)
            }
            is ProcessRefundAction.ProcessRefundStatus.Failure -> {
                emit(refundErrorMapper.mapTerminalError(refundParameters, status.exception))
            }
        }
    }
}
