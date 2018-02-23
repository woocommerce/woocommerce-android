package com.woocommerce.android.ui.orderlist

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import com.woocommerce.android.ui.base.ParentFragmentView
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel

interface OrderListContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrders()
    }

    interface View : BaseView<Presenter>, ParentFragmentView {
        var isActive: Boolean

        fun setLoadingIndicator(active: Boolean)

        fun showOrders(orders: List<WCOrderModel>)

        fun showNoOrders()

        fun getSelectedSite(): SiteModel?
    }
}
