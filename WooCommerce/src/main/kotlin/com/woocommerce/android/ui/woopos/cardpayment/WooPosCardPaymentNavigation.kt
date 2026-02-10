package com.woocommerce.android.ui.woopos.cardpayment

import androidx.navigation.NavController
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val CARD_PAYMENT_ROUTE_ORDER_ID_KEY = "orderId"
const val CARD_PAYMENT_ROUTE_SOURCE_KEY = "source"
const val CARD_PAYMENT_ROUTE =
    "$HOME_ROUTE/card_payment/{$CARD_PAYMENT_ROUTE_ORDER_ID_KEY}" +
        "?$CARD_PAYMENT_ROUTE_SOURCE_KEY={$CARD_PAYMENT_ROUTE_SOURCE_KEY}"

const val BOOKING_CARD_PAYMENT_SUCCESS_KEY = "booking_card_payment_success"

enum class CardPaymentSource {
    CHECKOUT,
    BOOKINGS,
}

fun NavController.navigateToCardPaymentScreen(
    orderId: Long,
    source: CardPaymentSource = CardPaymentSource.CHECKOUT,
) {
    navigateOnce(
        CARD_PAYMENT_ROUTE
            .replace("{$CARD_PAYMENT_ROUTE_ORDER_ID_KEY}", orderId.toString())
            .replace("{$CARD_PAYMENT_ROUTE_SOURCE_KEY}", source.name)
    )
}
