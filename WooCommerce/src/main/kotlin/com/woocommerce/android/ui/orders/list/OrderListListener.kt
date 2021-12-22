package com.woocommerce.android.ui.orders.list

import android.view.View
import com.woocommerce.android.model.OrderId

interface OrderListListener {
    fun openOrderDetail(
        localOrderId: Int,
        remoteOrderId: OrderId,
        orderStatus: String,
        sharedView: View? = null
    )
}
