package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderListContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrders(forceRefresh: Boolean)
        fun loadMoreOrders()
        fun canLoadMore(): Boolean
        fun isLoading(): Boolean
        fun openOrderDetail(order: WCOrderModel)
        fun fetchAndLoadOrdersFromDb(isForceRefresh: Boolean)
    }

    interface View : BaseView<Presenter>, OrdersViewRouter, OrderCustomerActionListener {
        fun setLoadingIndicator(active: Boolean)
        fun setLoadingMoreIndicator(active: Boolean)
        fun showOrders(orders: List<WCOrderModel>, isForceRefresh: Boolean)
        fun showNoOrders()
        fun refreshFragmentState()
        fun showLoadOrdersError()
    }
}
