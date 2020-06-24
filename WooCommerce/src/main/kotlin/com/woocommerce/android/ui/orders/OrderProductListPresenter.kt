package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

class OrderProductListPresenter @Inject constructor(
    private val orderStore: WCOrderStore,
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite
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
                orderModel?.let { order ->
                    val refunds = refundStore.getAllRefunds(selectedSite.get(), order.remoteOrderId)
                            .map { it.toAppModel() }
                    view.showOrderProducts(order, refunds = refunds)
                }
            }
        }
    }
}
