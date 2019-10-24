package com.woocommerce.android.ui.orders.list

interface OrderListListener {
    fun openOrderDetail(remoteOrderId: Long)
    fun onFragmentScrollDown()
    fun onFragmentScrollUp()
}
