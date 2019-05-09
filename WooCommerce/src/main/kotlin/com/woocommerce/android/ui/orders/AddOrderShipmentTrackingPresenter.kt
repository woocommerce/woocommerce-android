package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class AddOrderShipmentTrackingPresenter @Inject constructor(
    private val orderStore: WCOrderStore
) : AddOrderShipmentTrackingContract.Presenter {
    private var addTrackingView: AddOrderShipmentTrackingContract.View? = null

    override fun takeView(view: AddOrderShipmentTrackingContract.View) {
        addTrackingView = view
    }

    override fun dropView() {
        addTrackingView = null
    }

    override fun getOrderByIdentifier(orderId: OrderIdentifier): WCOrderModel? {
        return orderStore.getOrderByIdentifier(orderId)
    }
}
