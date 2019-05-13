package com.woocommerce.android.ui.orders

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface AddOrderShipmentTrackingContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        var orderIdentifier: OrderIdentifier?
        var isTrackingProviderFetched: Boolean
        fun takeProviderDialogView(view: AddOrderShipmentTrackingContract.DialogView)
        fun dropProviderDialogView()
        fun loadOrderDetail(orderIdentifier: OrderIdentifier, isTrackingProviderFetched: Boolean)
        fun loadShipmentTrackingProviders()
        fun loadShipmentTrackingProvidersFromDb()
        fun requestShipmentTrackingProvidersFromApi(order: WCOrderModel)
    }

    interface View : BaseView<Presenter> {
        fun isTrackingProviderFetched(): Boolean
        fun getDateShippedText(): String
        fun getProviderText(): String
    }

    interface DialogView : BaseView<Presenter> {
        fun showSkeleton(show: Boolean)
        fun showProviderListErrorSnack()
        fun showProviderList(providers: List<WCOrderShipmentProviderModel>)
    }
}
