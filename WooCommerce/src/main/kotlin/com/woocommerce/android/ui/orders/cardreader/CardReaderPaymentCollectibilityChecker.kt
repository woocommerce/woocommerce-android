package com.woocommerce.android.ui.orders.cardreader

import com.woocommerce.android.extensions.CASH_ON_DELIVERY_PAYMENT_TYPE
import com.woocommerce.android.extensions.WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import java.math.BigDecimal
import javax.inject.Inject

class CardReaderPaymentCollectibilityChecker @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository
) {
    fun isCollectable(order: Order): Boolean {
        return with(order) {
            currency.equals("USD", ignoreCase = true) &&
                (listOf(Order.Status.Pending, Order.Status.Processing, Order.Status.OnHold)).any { it == status } &&
                !isOrderPaid &&
                order.total.compareTo(BigDecimal.ZERO) == 1 &&
                BigDecimal.ZERO.compareTo(order.refundTotal) == 0 &&
                // Empty payment method explanation:
                // https://github.com/woocommerce/woocommerce/issues/29471
                (
                    paymentMethod == CASH_ON_DELIVERY_PAYMENT_TYPE ||
                        paymentMethod.isEmpty() ||
                        paymentMethod == WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE
                    ) &&
                !orderDetailRepository.hasSubscriptionProducts(order.getProductIds())
        }
    }
}
