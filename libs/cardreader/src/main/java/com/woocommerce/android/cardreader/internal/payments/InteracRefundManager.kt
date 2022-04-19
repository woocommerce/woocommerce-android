package com.woocommerce.android.cardreader.internal.payments

import com.woocommerce.android.cardreader.internal.payments.actions.CollectInteracRefundAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessInteracRefundAction
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.RefundParams
import com.woocommerce.android.cardreader.payments.toStripeRefundParameters
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
        collectInteracRefund(refundParameters)
    }

    private suspend fun FlowCollector<CardInteracRefundStatus>.collectInteracRefund(
        refundParameters: RefundParams
    ) {
        emit(CardInteracRefundStatus.CollectingInteracRefund)
        collectInteracRefundAction.collectRefund(
            refundParameters.toStripeRefundParameters(paymentUtils)
        ).collect { refundStatus ->
            when (refundStatus) {
                CollectInteracRefundAction.CollectInteracRefundStatus.Success -> {
                    processInteracRefund(refundParameters)
                }
                is CollectInteracRefundAction.CollectInteracRefundStatus.Failure -> {
                    emit(refundErrorMapper.mapTerminalError(refundParameters, refundStatus.exception))
                }
            }
        }
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
