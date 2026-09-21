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
import java.math.BigDecimal
import javax.inject.Inject

class CardReaderPaymentCollectibilityChecker @Inject constructor(
    private val cardReaderPaymentCurrencySupportedChecker: CardReaderPaymentCurrencySupportedChecker,
    private val orderSubscriptionChecker: OrderSubscriptionChecker,
) {
    /**
     * [checkSubscription] runs the (possibly networked) order-subscription check. It's on by default
     * for the payment method selection screen, where a loading/error state can be shown. The
     * controller's final gate and POS pass `false`: POS orders can't be subscriptions, and the
     * selection screen already blocks subscription orders before the controller is reached — so
     * re-running it there only adds latency and risks failing closed on a flaky network.
     */
    suspend fun isCollectable(
        order: Order,
        allowCancelledStatus: Boolean = false,
        checkSubscription: Boolean = true,
    ): Boolean {
        return with(order) {
            cardReaderPaymentCurrencySupportedChecker.isCurrencySupported(currency) &&
                isStatusCollectable(allowCancelledStatus) &&
                !isOrderPaid &&
                order.total.compareTo(BigDecimal.ZERO) == 1 &&
                BigDecimal.ZERO.compareTo(order.refundTotal) == 0 &&
                isPaymentMethodCollectable() &&
                (!checkSubscription || orderSubscriptionChecker.isOrderFreeOfSubscriptions(order))
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

    private fun Order.isStatusCollectable(allowCancelledStatus: Boolean) = status in arrayOf(
        Pending,
        Processing,
        OnHold,
        Custom(Order.Status.AUTO_DRAFT),
        Failed,
    ) || (allowCancelledStatus && status == Order.Status.Cancelled)
}
