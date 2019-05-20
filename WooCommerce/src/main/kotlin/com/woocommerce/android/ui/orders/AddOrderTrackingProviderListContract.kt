package com.woocommerce.android.ui.orders

import android.support.annotation.StringRes
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface AddOrderTrackingProviderListContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        var isShipmentTrackingProviderListFetched: Boolean
        fun loadShipmentTrackingProviders(orderIdentifier: OrderIdentifier?)
        fun loadShipmentTrackingProvidersFromDb()
        fun requestShipmentTrackingProvidersFromDb(): List<WCOrderShipmentProviderModel>
        fun requestShipmentTrackingProvidersFromApi(order: WCOrderModel)
    }

    interface View : BaseView<Presenter> {
        fun showSkeleton(show: Boolean)
        fun showProviderListErrorSnack(@StringRes stringResId: Int)
        fun showProviderList(providers: List<WCOrderShipmentProviderModel>)
    }
}
