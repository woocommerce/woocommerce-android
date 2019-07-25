package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper

interface OrderListContractNew {
    interface Presenter : BasePresenter<View> {
        fun generatePageWrapper(
            descriptor: WCOrderListDescriptor,
            lifecycle: Lifecycle
        ): PagedListWrapper<OrderListItemUIType>
        fun generateListDescriptor(orderStatusFilter: String?, orderSearchQuery: String? = ""): WCOrderListDescriptor

        // Order status methods
        fun getOrderStatusOptions(): Map<String, WCOrderStatusModel>
        fun refreshOrderStatusOptions()
        fun isOrderStatusOptionsRefreshing(): Boolean
    }

    interface View : BaseView<Presenter> {
        var isRefreshPending: Boolean
        var isSearching: Boolean
        var isRefreshing: Boolean

        fun showEmptyView(show: Boolean)
        fun refreshFragmentState()
        fun showOrderDetail(order: WCOrderModel)

        fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>)
    }
}
