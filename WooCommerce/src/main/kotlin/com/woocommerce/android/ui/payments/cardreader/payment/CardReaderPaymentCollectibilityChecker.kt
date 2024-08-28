package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.extensions.CASH_ON_DELIVERY_PAYMENT_TYPE
import com.woocommerce.android.extensions.STRIPE_PAYMENTS_PAYMENT_TYPE
import com.woocommerce.android.extensions.WOOCOMMERCE_BOOKINGS_PAYMENT_TYPE
import com.woocommerce.android.extensions.WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Status.Custom
import com.woocommerce.android.model.Order.Status.Failed
import com.woocommerce.android.model.Order.Status.OnHold
import com.woocommerce.android.model.Order.Status.Pending
import com.woocommerce.android.model.Order.Status.Processing
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import java.math.BigDecimal
import javax.inject.Inject

class CardReaderPaymentCollectibilityChecker @Inject constructor(
    private val orderDetailRepository: OrderDetailRepository,
    private val cardReaderPaymentCurrencySupportedChecker: CardReaderPaymentCurrencySupportedChecker,
) {
    suspend fun isCollectable(order: Order): Boolean {
        return with(order) {
            cardReaderPaymentCurrencySupportedChecker.isCurrencySupported(currency) &&
                isStatusCollectable() &&
                !isOrderPaid &&
                order.total.compareTo(BigDecimal.ZERO) == 1 &&
                BigDecimal.ZERO.compareTo(order.refundTotal) == 0 &&
                isPaymentMethodCollectable() &&
                !orderDetailRepository.hasSubscriptionProducts(order.getProductIds())
        }
    }

    private fun Order.isPaymentMethodCollectable() =
        paymentMethod in arrayOf(
            // Empty payment method explanation:
            // https://github.com/woocommerce/woocommerce/issues/29471
            "",
            CASH_ON_DELIVERY_PAYMENT_TYPE,
            WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE,
            STRIPE_PAYMENTS_PAYMENT_TYPE,
            WOOCOMMERCE_BOOKINGS_PAYMENT_TYPE,
        )

    private fun Order.isStatusCollectable() = status in arrayOf(
        Pending,
        Processing,
        OnHold,
        Custom(Order.Status.AUTO_DRAFT),
        Failed,
    )
}
