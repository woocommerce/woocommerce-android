package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isCashPayment
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.payments.refunds.PaymentChargeRepository
import com.woocommerce.android.ui.payments.toCardBrandDisplayName
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosGetPaymentMethod @Inject constructor(
    private val paymentChargeRepository: PaymentChargeRepository,
    private val loadPaymentGateway: WooPosLoadPaymentGateway,
    private val resourceProvider: ResourceProvider
) {
    suspend operator fun invoke(order: Order): Result<String> {
        val paymentGatewayResult = loadPaymentGateway.invoke(order)
        if (paymentGatewayResult.isFailure) {
            return Result.failure(paymentGatewayResult.exceptionOrNull()!!)
        }

        val paymentGateway = paymentGatewayResult.getOrThrow()
        val manualRefundMethod = resourceProvider.getString(R.string.order_refunds_manual_refund)

        if (!order.paymentMethod.isCashPayment && (!paymentGateway.isEnabled || !paymentGateway.supportsRefunds)) {
            val result = if (paymentGateway.title.isNotBlank()) {
                resourceProvider.getString(R.string.order_refunds_method, manualRefundMethod, paymentGateway.title)
            } else {
                manualRefundMethod
            }
            return Result.success(result)
        }

        val refundMethod = enrichRefundMethodWithCardDetails(
            order,
            paymentGateway.title.ifBlank { paymentGateway.methodTitle }
        )
        return Result.success(refundMethod)
    }

    private suspend fun enrichRefundMethodWithCardDetails(order: Order, refundMethod: String): String {
        val chargeId = order.chargeId
        return if (chargeId != null) {
            loadCardDetails(chargeId, refundMethod)
        } else {
            refundMethod
        }
    }

    private suspend fun loadCardDetails(chargeId: String, refundMethod: String): String {
        return when (val result = paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)) {
            is PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success -> {
                val brand = result.cardBrand.toCardBrandDisplayName()
                val last4 = result.cardLast4.orEmpty()
                val creditCardRefundDefaultText =
                    resourceProvider.getString(R.string.order_refunds_credit_card_refund)
                "${refundMethod.ifBlank { creditCardRefundDefaultText }} ($brand **** $last4)"
            }
            PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error -> {
                refundMethod
            }
        }
    }
}
