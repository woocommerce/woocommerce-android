package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.orders.AddOrderNoteFragment.Companion.AddOrderNoteListener
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrdersViewRouter {
    fun openOrderDetail(order: WCOrderModel, markOrderComplete: Boolean = false)
    fun openOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNotificationId: Long? = null)
    fun openOrderFulfillment(order: WCOrderModel)
    fun openOrderProductList(order: WCOrderModel)
    fun openAddOrderNote(order: WCOrderModel, listener: AddOrderNoteListener)
}
