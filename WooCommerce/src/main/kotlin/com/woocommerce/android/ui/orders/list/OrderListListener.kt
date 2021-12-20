package com.woocommerce.android.ui.orders.list

import android.view.View

interface OrderListListener {
    fun openOrderDetail(
        localOrderId: Int,
        remoteOrderId: Long,
        orderStatus: String,
        sharedView: View? = null
    )
}
