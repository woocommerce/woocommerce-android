package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class OrderCreationNavigationTarget : Event() {
    data class EditCustomerNote(val currentNote: String): OrderCreationNavigationTarget()
}
