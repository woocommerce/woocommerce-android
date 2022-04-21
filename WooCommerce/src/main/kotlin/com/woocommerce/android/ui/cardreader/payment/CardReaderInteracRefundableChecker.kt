package com.woocommerce.android.ui.cardreader.payment

import com.woocommerce.android.extensions.CASH_ON_DELIVERY_PAYMENT_TYPE
import com.woocommerce.android.extensions.WOOCOMMERCE_BOOKINGS_PAYMENT_TYPE
import com.woocommerce.android.extensions.WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import java.math.BigDecimal
import javax.inject.Inject

class CardReaderInteracRefundableChecker @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val cardReaderPaymentCurrencySupportedChecker: CardReaderPaymentCurrencySupportedChecker,
) {
    suspend fun isRefundable(order: Order): Boolean {
        return with(order) {
            cardReaderPaymentCurrencySupportedChecker.isCurrencySupported(currency) &&
                Order.Status.Completed == status &&
                isOrderPaid &&
                order.total.compareTo(BigDecimal.ZERO) == 1 &&
                // Empty payment method explanation:
                // https://github.com/woocommerce/woocommerce/issues/29471
                (
                    paymentMethod == CASH_ON_DELIVERY_PAYMENT_TYPE ||
                        paymentMethod.isEmpty() ||
                        paymentMethod == WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE ||
                        paymentMethod == WOOCOMMERCE_BOOKINGS_PAYMENT_TYPE
                    ) &&
                !orderDetailRepository.hasSubscriptionProducts(order.getProductIds())
        }
    }
}
