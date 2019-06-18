package com.woocommerce.android.ui.main

import android.content.Intent
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.di.MockedSelectedSiteModule
import com.woocommerce.android.ui.orders.MockedAddOrderShipmentTrackingModule
import com.woocommerce.android.ui.orders.MockedAddOrderTrackingProviderListModule
import com.woocommerce.android.ui.orders.MockedOrderDetailModule
import com.woocommerce.android.ui.orders.MockedOrderFulfillmentModule
import com.woocommerce.android.ui.orders.MockedOrderListModule
import com.woocommerce.android.ui.orders.WcOrderTestUtils
import com.woocommerce.android.ui.products.MockedOrderProductListModule
import com.woocommerce.android.ui.products.MockedProductDetailModule
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged

class MainActivityTestRule : ActivityTestRule<MainActivity>(MainActivity::class.java, false, false) {
    /**
     * Bypass the Login flow and launch directly into the [MainActivity].
     */
    fun launchMainActivityLoggedIn(startIntent: Intent?, siteModel: SiteModel): MainActivity {
        // Configure the mocked MainPresenter to pretend the user is logged in.
        // We normally wouldn't need the MockedMainModule method, and just configure the mocked presenter directly
        // using whenever(activityTestRule.activity.presenter.userIsLoggedIn()).thenReturn(true)
        // In this case, however, userIsLoggedIn() is called in the activity's onCreate(), which means after
        // launchActivity() is too late, but the activity's presenter is null before that.
        // So, we need to configure this at the moment the injection is happening: when the presenter is initialized.
        MockedMainModule.setUserIsLoggedInResponse(true)
        // Preload the SelectedSite with a SiteModel, to satisfy the expectation that it was set during login
        // The reason for doing this here is the same as for the MockedMainModule
        MockedSelectedSiteModule.setSiteModel(siteModel)
        return super.launchActivity(startIntent)
    }

    /**
     * Setting mock data for order list screen
     */
    fun setOrderListWithMockData(
        orders: List<WCOrderModel> = WcOrderTestUtils.generateOrders(),
        orderStatusList: Map<String, WCOrderStatusModel> = WcOrderTestUtils.generateOrderStatusOptions()
    ) {
        MockedOrderListModule.setOrders(orders)
        MockedOrderListModule.setOrderStatusList(orderStatusList)
    }

    /**
     * Setting mock data for order detail screen
     */
    fun setOrderDetailWithMockData(
        order: WCOrderModel,
        orderStatus: WCOrderStatusModel = WcOrderTestUtils.generateOrderStatusDetail(),
        orderNotes: List<WCOrderNoteModel> = WcOrderTestUtils.generateSampleNotes(),
        orderShipmentTrackings: List<WCOrderShipmentTrackingModel> = WcOrderTestUtils.generateOrderShipmentTrackings(),
        isNetworkConnected: Boolean = false,
        onOrderChanged: OnOrderChanged? = null,
        isVirtualProduct: Boolean = false
    ) {
        MockedOrderDetailModule.setOrderInfo(order)
        MockedOrderDetailModule.setOrderStatus(orderStatus)
        MockedOrderDetailModule.setOrderNotes(orderNotes)
        MockedOrderDetailModule.setOrderShipmentTrackings(orderShipmentTrackings)
        MockedOrderDetailModule.setNetworkConnected(isNetworkConnected)
        MockedOrderDetailModule.setOnOrderChanged(onOrderChanged)
        MockedOrderDetailModule.setIsVirtualProduct(isVirtualProduct)
    }

    /**
     * Setting mock data for order fulfillment screen
     */
    fun setOrderFulfillmentWithMockData(
        order: WCOrderModel,
        orderShipmentTrackings: List<WCOrderShipmentTrackingModel> = WcOrderTestUtils.generateOrderShipmentTrackings(),
        isNetworkConnected: Boolean = false,
        onOrderChanged: OnOrderChanged? = null
    ) {
        setOrderDetailWithMockData(
                order = order, orderShipmentTrackings = orderShipmentTrackings, onOrderChanged = onOrderChanged
        )
        MockedOrderFulfillmentModule.setOrderInfo(order)
        MockedOrderFulfillmentModule.setOrderShipmentTrackings(orderShipmentTrackings)
        MockedOrderFulfillmentModule.setNetworkConnected(isNetworkConnected)
        MockedOrderFulfillmentModule.setOnOrderChanged(onOrderChanged)
    }

    /**
     * Setting mock data for order fulfillment screen
     */
    fun setAddShipmentTrackingWithMockData(
        order: WCOrderModel,
        onOrderChanged: OnOrderChanged,
        isNetworkConnected: Boolean = false
    ) {
        MockedAddOrderShipmentTrackingModule.setOrderInfo(order)
        MockedAddOrderShipmentTrackingModule.setOnOrderChanged(onOrderChanged)
        MockedAddOrderShipmentTrackingModule.setNetworkConnected(isNetworkConnected)
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

    /**
     * Setting mock data for order product list screen
     */
    fun setOrderProductListWithMockData(
        order: WCOrderModel
    ) {
        MockedOrderProductListModule.setOrderInfo(order)
    }

    /**
     * Setting mock data for order product detail screen
     */
    fun setOrderProductDetailWithMockData(
        product: WCProductModel
    ) {
        MockedProductDetailModule.setMockProduct(product)
    }
}
