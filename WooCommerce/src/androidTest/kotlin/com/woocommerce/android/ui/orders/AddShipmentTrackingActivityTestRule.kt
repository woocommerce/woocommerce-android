package com.woocommerce.android.ui.orders

import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

class AddShipmentTrackingActivityTestRule : ActivityTestRule<AddOrderShipmentTrackingActivity>(
        AddOrderShipmentTrackingActivity::class.java, false, false
) {
    /**
     * Launch directly into the [AddOrderShipmentTrackingActivity] with intent
     */
    fun launchAddShipmentActivityWithIntent(
        orderIdentifier: OrderIdentifier,
        providerName: String,
        isCustomProvider: Boolean = false
    ): AddOrderShipmentTrackingActivity {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val result = Intent(targetContext, AddOrderShipmentTrackingActivity::class.java)
        result.putExtra(AddOrderShipmentTrackingActivity.FIELD_ORDER_TRACKING_PROVIDER, providerName)
        result.putExtra(AddOrderShipmentTrackingActivity.FIELD_ORDER_IDENTIFIER, orderIdentifier)
        result.putExtra(AddOrderShipmentTrackingActivity.FIELD_IS_CUSTOM_PROVIDER, isCustomProvider)

        // Launch activity with intent
        return super.launchActivity(result)
    }

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
