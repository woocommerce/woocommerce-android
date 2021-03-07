package com.woocommerce.android.ui.orders.creation.common.navigation

import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class OrderCreationNavigationTarget : Event() {
    object AddCustomer : OrderCreationNavigationTarget()
}
