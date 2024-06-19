package com.woocommerce.android.ui.woopos.home

sealed class WooPosHomeEvent {
    data class OrderSuccessfullyPaid(val orderId: Long) : WooPosHomeEvent()
}
