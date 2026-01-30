package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isCashPayment
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.refunds.PaymentChargeRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCGatewayStore
import javax.inject.Inject

class WooPosGetPaymentMethod @Inject constructor(
    private val paymentChargeRepository: PaymentChargeRepository,
    private val gatewayStore: WCGatewayStore,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(order: Order): String {
        val paymentGateway = loadPaymentGateway(order)
        val manualRefundMethod = resourceProvider.getString(R.string.order_refunds_manual_refund)

        if (!order.paymentMethod.isCashPayment && (!paymentGateway.isEnabled || !paymentGateway.supportsRefunds)) {
            return if (paymentGateway.title.isNotBlank()) {
                resourceProvider.getString(R.string.order_refunds_method, manualRefundMethod, paymentGateway.title)
            } else {
                manualRefundMethod
            }
        }

        return enrichRefundMethodWithCardDetails(
            order,
            paymentGateway.title.ifBlank { paymentGateway.methodTitle }
        )
    }

    private suspend fun loadPaymentGateway(order: Order): PaymentGateway = withContext(coroutineDispatchers.io) {
        gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
            ?: PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
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
                val brand = result.cardBrand.orEmpty().replaceFirstChar { it.uppercase() }
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

    companion object {
        private const val REFUND_METHOD_MANUAL = "manual"
    }
}
