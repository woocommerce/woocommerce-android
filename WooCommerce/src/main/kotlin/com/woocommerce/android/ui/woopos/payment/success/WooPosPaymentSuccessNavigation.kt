package com.woocommerce.android.ui.woopos.payment.success

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val PAYMENT_SUCCESS_ROUTE = "woopos-payment-success"

fun NavGraphBuilder.paymentSuccessScreen() {
    composable(PAYMENT_SUCCESS_ROUTE) {
        WooPosPaymentSuccessScreen()
    }
}
