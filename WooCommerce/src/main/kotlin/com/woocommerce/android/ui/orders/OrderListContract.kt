package com.woocommerce.android.ui.orders

import android.content.Context
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderListContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrders(context: Context?, forceRefresh: Boolean)
        fun openOrderDetail(order: WCOrderModel)
    }

    interface View : BaseView<Presenter>, OrdersViewRouter, OrderCustomerActionListener {
        var isActive: Boolean

        fun setLoadingIndicator(active: Boolean)
        fun showOrders(orders: List<WCOrderModel>, isForceRefresh: Boolean)
        fun showNoOrders()
        fun showNetworkErrorFetchOrders()
        fun refreshFragmentState()
    }
}
