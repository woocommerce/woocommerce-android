package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.RefundParameters
import com.woocommerce.android.cardreader.internal.payments.actions.CollectInteracRefundAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessInteracRefundAction
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.RefundParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

internal class InteracRefundManager(
    private val collectInteracRefundAction: CollectInteracRefundAction,
    private val processInteracRefundAction: ProcessInteracRefundAction,
) {
    fun refundInteracPayment(refundParameters: RefundParams): Flow<CardInteracRefundStatus> = flow {
        collectInteracRefund(refundParameters)
    }

    private suspend fun FlowCollector<CardInteracRefundStatus>.collectInteracRefund(
        refundParameters: RefundParams
    ) {
        emit(CardInteracRefundStatus.CollectingInteracRefund)
        collectInteracRefundAction.collectRefund(
            RefundParameters.Builder(
                chargeId = refundParameters.chargeId,
                amount = refundParameters.amount.toLong(),
                currency = refundParameters.currency
            ).build()
        ).collect { refundStatus ->
            when (refundStatus) {
                CollectInteracRefundAction.CollectInteracRefundStatus.Success -> {
                    processInteracRefund()
                }
                is CollectInteracRefundAction.CollectInteracRefundStatus.Failure -> {
                    emit(CardInteracRefundStatus.InteracRefundFailure(refundStatus.exception))
                }
            }
        }
    }

    private suspend fun FlowCollector<CardInteracRefundStatus>.processInteracRefund() {
        emit(CardInteracRefundStatus.ProcessingInteracRefund)
        processInteracRefundAction.processRefund().collect { status ->
            when (status) {
                is ProcessInteracRefundAction.ProcessRefundStatus.Success -> {
                    emit(CardInteracRefundStatus.InteracRefundSuccess)
                }
                is ProcessInteracRefundAction.ProcessRefundStatus.Failure -> {
                    emit(CardInteracRefundStatus.InteracRefundFailure(status.exception))
                }
            }
        }
    }
}
