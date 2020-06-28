package com.woocommerce.android.ui.orders.list

interface OrderListListener {
    fun openOrderDetail(remoteOrderId: Long, orderStatus: String)
}
