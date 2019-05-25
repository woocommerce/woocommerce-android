package com.woocommerce.android.ui.orders

import android.support.test.rule.ActivityTestRule
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel

class AddShipmentTrackingActivityTestRule : ActivityTestRule<AddOrderShipmentTrackingActivity>(
        AddOrderShipmentTrackingActivity::class.java, false, false
) {
    /**
     * Setting mock data for order provider list screen
     */
    fun setOrderProviderListWithMockData(
        storeCountry: String = "US",
        order: WCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing"),
        orderProviderList: List<WCOrderShipmentProviderModel> = WcOrderTestUtils.generateShipmentTrackingProviderList()
    ) {
        MockedAddOrderTrackingProviderListModule.setOrderInfo(order)
        MockedAddOrderTrackingProviderListModule.setStoreCountry(storeCountry)
        MockedAddOrderTrackingProviderListModule.setOrderShipmentTrackingProviders(orderProviderList)
    }
}
