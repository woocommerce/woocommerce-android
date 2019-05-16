package com.woocommerce.android.ui.orders

import android.support.annotation.StringRes
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface AddOrderShipmentTrackingContract {
    interface Presenter : BasePresenter<View> {
        var orderModel: WCOrderModel?
        var orderIdentifier: OrderIdentifier?
        var isShipmentTrackingProviderListFetched: Boolean
        fun takeProviderDialogView(view: AddOrderShipmentTrackingContract.DialogView)
        fun dropProviderDialogView()
        fun loadOrderDetail(orderIdentifier: OrderIdentifier)
        fun loadShipmentTrackingProviders()
        fun loadShipmentTrackingProvidersFromDb()
        fun requestShipmentTrackingProvidersFromDb(): List<WCOrderShipmentProviderModel>
        fun requestShipmentTrackingProvidersFromApi(order: WCOrderModel)
    }

    interface View : BaseView<Presenter> {
        fun getDateShippedText(): String
        fun getProviderText(): String
    }

    interface DialogView : BaseView<Presenter> {
        fun showSkeleton(show: Boolean)
        fun showProviderListErrorSnack(@StringRes stringResId: Int)
        fun showProviderList(providers: List<WCOrderShipmentProviderModel>)
    }
}
