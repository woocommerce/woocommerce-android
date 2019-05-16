package com.woocommerce.android.ui.orders

import javax.inject.Inject

class AddOrderShipmentTrackingPresenter @Inject constructor() : AddOrderShipmentTrackingContract.Presenter {
    companion object {
        private val TAG: String = AddOrderShipmentTrackingPresenter::class.java.simpleName
    }

    private var addTrackingView: AddOrderShipmentTrackingContract.View? = null

    override fun takeView(view: AddOrderShipmentTrackingContract.View) {
        addTrackingView = view
    }

    override fun dropView() {
        addTrackingView = null
    }
}
