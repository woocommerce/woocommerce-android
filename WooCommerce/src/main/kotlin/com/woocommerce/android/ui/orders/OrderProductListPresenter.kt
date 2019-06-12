package com.woocommerce.android.ui.orders

import com.woocommerce.android.annotations.OpenClassOnDebug
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@OpenClassOnDebug
class OrderProductListPresenter @Inject constructor(
    private val orderStore: WCOrderStore
) : OrderProductListContract.Presenter {
    private var productView: OrderProductListContract.View? = null

    override fun takeView(view: OrderProductListContract.View) {
        productView = view
    }

    override fun dropView() {
        productView = null
    }

    /**
     * Loading order detail from local database.
     * Segregating methods that request data from db for better ui testing
     */
    override fun getOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel? {
        return orderStore.getOrderByIdentifier(orderIdentifier)
    }

    override fun loadOrderDetail(orderIdentifier: OrderIdentifier) {
        if (orderIdentifier.isNotEmpty()) {
            productView?.let { view ->
                val orderModel = getOrderDetailFromDb(orderIdentifier)
                orderModel?.let {
                    view.showOrderProducts(it)
                }
            }
        }
    }
}
