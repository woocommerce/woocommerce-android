package com.woocommerce.android.ui.orders.list

interface OrderListListener {
    fun openOrderDetail(localOrderId: Int, remoteOrderId: Long, orderStatus: String)
}
