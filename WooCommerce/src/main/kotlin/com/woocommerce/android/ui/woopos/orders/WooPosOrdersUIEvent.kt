package com.woocommerce.android.ui.woopos.orders

sealed interface WooPosOrdersUIEvent {
    data class OrderActionClicked(val action: WooPosOrdersState.OrderAction) : WooPosOrdersUIEvent
    data class ViewRefundDetailsClicked(val refundIndex: Int) : WooPosOrdersUIEvent
}
