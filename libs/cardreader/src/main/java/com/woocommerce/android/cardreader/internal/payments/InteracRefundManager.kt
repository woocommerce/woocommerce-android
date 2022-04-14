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
    private val refundErrorMapper: RefundErrorMapper,
    private val paymentUtils: PaymentUtils,
) {
    fun refundInteracPayment(refundParameters: RefundParams): Flow<CardInteracRefundStatus> = flow {
        emit(CardInteracRefundStatus.InitializingInteracRefund)
        val collectRefundStatus = collectInteracRefund(refundParameters)
        if (collectRefundStatus == CollectInteracRefundAction.CollectInteracRefundStatus.Success) {
            processInteracRefund(refundParameters)
        }
    }

    private suspend fun FlowCollector<CardInteracRefundStatus>.collectInteracRefund(
        refundParameters: RefundParams
    ): CollectInteracRefundAction.CollectInteracRefundStatus {
        var collectInteracRefundStatus: CollectInteracRefundAction.CollectInteracRefundStatus =
            CollectInteracRefundAction.CollectInteracRefundStatus.Success
        emit(CardInteracRefundStatus.CollectingInteracRefund)
        collectInteracRefundAction.collectRefund(
            RefundParameters.Builder(
                chargeId = refundParameters.chargeId,
                amount = paymentUtils.convertBigDecimalInDollarsToLongInCents(refundParameters.amount),
                currency = refundParameters.currency
            ).build()
        ).collect { refundStatus ->
            collectInteracRefundStatus = when (refundStatus) {
                CollectInteracRefundAction.CollectInteracRefundStatus.Success -> {
                    CollectInteracRefundAction.CollectInteracRefundStatus.Success
                }
                is CollectInteracRefundAction.CollectInteracRefundStatus.Failure -> {
                    emit(refundErrorMapper.mapTerminalError(refundParameters, refundStatus.exception))
                    CollectInteracRefundAction.CollectInteracRefundStatus.Failure(refundStatus.exception)
                }
            }
        }
        return collectInteracRefundStatus
    }

    private suspend fun FlowCollector<CardInteracRefundStatus>.processInteracRefund(refundParameters: RefundParams) {
        emit(CardInteracRefundStatus.ProcessingInteracRefund)
        processInteracRefundAction.processRefund().collect { status ->
            when (status) {
                is ProcessInteracRefundAction.ProcessRefundStatus.Success -> {
                    emit(CardInteracRefundStatus.InteracRefundSuccess)
                }
                is ProcessInteracRefundAction.ProcessRefundStatus.Failure -> {
                    emit(refundErrorMapper.mapTerminalError(refundParameters, status.exception))
                }
            }
        }
    }
}
