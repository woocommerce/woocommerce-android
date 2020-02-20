package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface OrderFulfillmentContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        var isShipmentTrackingsFetched: Boolean
        var deletedOrderShipmentTrackingModel: WCOrderShipmentTrackingModel?
        fun loadOrderDetail(orderIdentifier: OrderIdentifier, isShipmentTrackingsFetched: Boolean = false)
        fun getOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel?
        fun loadOrderShipmentTrackings()
        fun loadShipmentTrackingsFromDb()
        fun fetchShipmentTrackingsFromApi(order: WCOrderModel)
        fun getShipmentTrackingsFromDb(order: WCOrderModel): List<WCOrderShipmentTrackingModel>
        fun markOrderComplete()
        fun deleteOrderShipmentTracking(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel)
        fun isVirtualProduct(order: WCOrderModel): Boolean
    }

    interface View : BaseView<Presenter>, OrderProductActionListener, OrderShipmentTrackingActionListener {
        fun showOrderDetail(order: WCOrderModel, refunds: List<Refund>)
        fun showOrderShipmentTrackings(trackings: List<WCOrderShipmentTrackingModel>)
        fun showAddShipmentTrackingSnack()
        fun showAddAddShipmentTrackingErrorSnack()
        fun toggleCompleteButton(isEnabled: Boolean)
        fun fulfillOrder()
        fun undoDeletedTrackingOnError(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel?)
        fun markTrackingDeletedOnSuccess()
        fun showDeleteTrackingErrorSnack()
    }
}
