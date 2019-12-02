package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel

interface OrderListListener {
    fun getOrderStatusOptions(): Map<String, WCOrderStatusModel>
    fun refreshOrderStatusOptions()
    fun openOrderDetail(wcOrderModel: WCOrderModel)
}
