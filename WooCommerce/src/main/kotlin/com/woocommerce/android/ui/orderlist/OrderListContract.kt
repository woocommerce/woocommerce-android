package com.woocommerce.android.ui.orderlist

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderListContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrders()
        fun openOrderDetail(order: WCOrderModel)
    }

    interface View : BaseView<Presenter> {
        var isActive: Boolean

        fun setLoadingIndicator(active: Boolean)
        fun showOrders(orders: List<WCOrderModel>)
        fun showNoOrders()
        fun openOrderDetail(order: WCOrderModel)

        fun getSelectedSite(): SiteModel?
    }
}
