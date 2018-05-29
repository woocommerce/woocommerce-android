package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import com.woocommerce.android.ui.base.ConnectionCheckView
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderListContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrders(forceRefresh: Boolean)
        fun openOrderDetail(order: WCOrderModel)
        fun fetchAndLoadOrdersFromDb(isForceRefresh: Boolean)
    }

    interface View : BaseView<Presenter>, OrdersViewRouter, OrderCustomerActionListener, ConnectionCheckView {
        var isActive: Boolean

        fun setLoadingIndicator(active: Boolean)
        fun showOrders(orders: List<WCOrderModel>, isForceRefresh: Boolean)
        fun showNoOrders()
        fun refreshFragmentState()
    }
}
