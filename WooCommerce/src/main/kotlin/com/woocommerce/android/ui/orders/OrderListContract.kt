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
        fun fetchAndLoadOrdersFromDb(clearExisting: Boolean)
    }

    interface View : BaseView<Presenter>, OrdersViewRouter, OrderCustomerActionListener {
        var isActive: Boolean

        fun setLoadingIndicator(active: Boolean)
        fun setLoadingMoreIndicator(active: Boolean)
        fun showOrders(orders: List<WCOrderModel>, clearExisting: Boolean)
        fun showNoOrders()
        fun refreshFragmentState()
    }
}
