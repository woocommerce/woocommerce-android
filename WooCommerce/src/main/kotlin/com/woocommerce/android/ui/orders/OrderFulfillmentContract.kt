package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderFulfillmentContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        var isUsingCachedShipmentTrackings: Boolean
        fun loadOrderDetail(orderIdentifier: OrderIdentifier, isUsingCachedShipmentTrackings: Boolean = false)
        fun loadOrderShipmentTrackings()
        fun pushShipmentTrackingProvider(provider: String, trackingNum: String, dateShipped: String)
        fun loadShipmentTrackingsFromDb()
        fun requestShipmentTrackingsFromApi(order: WCOrderModel)
        fun markOrderComplete()
    }

    interface View : BaseView<Presenter>, OrderProductActionListener, OrderShipmentTrackingActionListener {
        fun showOrderDetail(order: WCOrderModel)
        fun showOrderShipmentTrackings(trackings: List<WCOrderShipmentTrackingModel>)
        fun showAddShipmentTrackingSnack()
        fun showAddAddShipmentTrackingErrorSnack()
        fun toggleCompleteButton(isEnabled: Boolean)
        fun fulfillOrder()
    }
}
