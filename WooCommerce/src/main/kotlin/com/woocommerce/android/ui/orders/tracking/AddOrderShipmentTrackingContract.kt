package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

interface AddOrderShipmentTrackingContract {
    interface Presenter : BasePresenter<View> {
        fun loadOrderDetailFromDb(orderIdentifier: OrderIdentifier): WCOrderModel?
        fun pushShipmentTrackingRecord(
            orderIdentifier: OrderIdentifier,
            wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel,
            isCustomProvider: Boolean
        ): Boolean
    }

    interface View : BaseView<BasePresenter<View>> {
        fun confirmDiscard()
        fun getDateShippedText(): String
        fun getProviderText(): String
        fun isCustomProvider(): Boolean
        fun showAddShipmentTrackingSnack()
        fun showAddAddShipmentTrackingErrorSnack()
        fun showOfflineSnack()
    }
}
