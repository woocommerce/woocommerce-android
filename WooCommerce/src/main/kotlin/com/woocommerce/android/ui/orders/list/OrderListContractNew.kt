package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel

interface OrderListContractNew {
    interface Presenter : BasePresenter<View> {

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
