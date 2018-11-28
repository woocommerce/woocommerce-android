package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderListContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrders(filterByStatus: String? = null, filterByKeyword: String? = null, forceRefresh: Boolean)
        fun loadMoreOrders(orderStatusFilter: String? = null)
        fun canLoadMore(): Boolean
        fun isLoading(): Boolean
        fun openOrderDetail(order: WCOrderModel)
        fun fetchAndLoadOrdersFromDb(orderStatusFilter: String? = null, isForceRefresh: Boolean)
    }

    interface View : BaseView<Presenter>, OrdersViewRouter, OrderCustomerActionListener {
        var isActive: Boolean
        var isRefreshPending: Boolean

        fun setLoadingMoreIndicator(active: Boolean)
        fun showOrders(orders: List<WCOrderModel>, filterByStatus: String? = null, isFreshData: Boolean)
        fun showNoOrdersView(show: Boolean)
        fun refreshFragmentState()
        fun showLoadOrdersError()
        fun onFilterSelected(orderStatus: String?)
        fun submitSearch(query: String?)

        fun showSkeleton(show: Boolean)
    }
}
