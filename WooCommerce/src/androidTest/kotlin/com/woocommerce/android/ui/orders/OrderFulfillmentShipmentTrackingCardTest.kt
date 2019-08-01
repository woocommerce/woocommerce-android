package com.woocommerce.android.ui.orders

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.R.id
import com.woocommerce.android.R.string
import com.woocommerce.android.helpers.WCHelperUtils
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.helpers.WCMatchers.withRecyclerView
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.util.DateUtils
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderFulfillmentShipmentTrackingCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing")
    private val mockShipmentTrackingList = WcOrderTestUtils.generateOrderShipmentTrackings()

    /**
     * Helper method to update the network status for the current fragment to test
     * offline scenarios
     */
    private fun getOrderFulfillmentFragment(): OrderFulfillmentFragment? {
        return activityTestRule.activity.supportFragmentManager.primaryNavigationFragment?.let { navFragment ->
            navFragment.childFragmentManager.fragments[0] as? OrderFulfillmentFragment
        }
    }

    private fun setupOrderFulfillPage(isNetworkConnected: Boolean = false) {
        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orders)).perform(click())

        // Set Order fulfillment with mock data
        activityTestRule.setOrderFulfillmentWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList,
                isNetworkConnected = isNetworkConnected
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())
    }

    @Test
    fun verifyOrderShipmentCardViewPopulatedSuccessfullyIfAvailable() {
        setupOrderFulfillPage()
        // check if shipment tracking list is 4
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)

        // verify that the Add tracking button is visible
        onView(withId(R.id.shipmentTrack_btnAddTracking)).check(
                ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(VISIBLE))
        )

        // verify that the shipment tracking title is displayed correctly
        onView(withId(R.id.shipmentTrack_label)).check(ViewAssertions.matches(
                        ViewMatchers.withText(appContext.getString(R.string.order_shipment_tracking_add_label))
                ))
    }

    @Test
    fun verifyShipmentTrackingItemDetailsPopulatedSuccessfully() {
        setupOrderFulfillPage()

        // verify if the first shipment tracking name matches the first item from the mock data
        onView(WCMatchers.withRecyclerView(id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_number))
                .check(ViewAssertions.matches(withText(mockShipmentTrackingList[0].trackingNumber)))

        // verify if the first shipment tracking name matches the first item from the mock data
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_dateShipped))
                .check(matches(withText(appContext.getString(
                        R.string.order_shipment_tracking_shipped_date,
                        DateUtils.getLocalizedLongDateString(appContext, mockShipmentTrackingList[0].dateShipped)
                ))))

        // verify that the Delete tracking button is visible
        onView(WCMatchers.withRecyclerView(id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // verify that the Hamburger icon button is not visible
        onView(WCMatchers.withRecyclerView(id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyShipmentTrackingItemClickCopiesToClipboard() {
        setupOrderFulfillPage()

        // click on the shipment tracking item - this should copy the text to clipboard
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_copyNumber))
                .perform(WCMatchers.scrollTo(), click())

        // tests are failing in devices below api 27 when getting clipboard text without handler
        Handler(Looper.getMainLooper()).post {
            val clipboardText = WCHelperUtils.getClipboardText(appContext)
            assertEquals(mockShipmentTrackingList[0].trackingNumber, clipboardText)
        }
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickWhenOffline() {
        setupOrderFulfillPage()

        // mock network not available
        val orderFulfillmentFragment = getOrderFulfillmentFragment()
        doReturn(false).whenever(orderFulfillmentFragment?.networkStatus)?.isConnected()

        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(WCMatchers.scrollTo(), click())

        // check if the offline snack is displayed
        onView(allOf(withId(com.google.android.material.R.id.snackbar_text), withText(R.string.offline_error)))
                .check(matches(isDisplayed()))

        // verify that the shipment tracking list count matches the mock data count
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickUndoDeleteTracking() {
        setupOrderFulfillPage(true)

        val orderFulfillmentFragment = getOrderFulfillmentFragment()
        doReturn(true).whenever(orderFulfillmentFragment?.networkStatus)?.isConnected()

        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(click())

        // check if the shipment tracking list count has reduced by 1
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

        // check if the snackbar with undo button is displayed
        onView(allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.order_shipment_tracking_delete_snackbar_msg))
        ).check(matches(isDisplayed()))

        // click on the undo button
        onView(withText(appContext.getString(R.string.undo))).perform(click())

        // verify that the shipment tracking list count matches the mock data count
        assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickWhenResponseSuccess() {
        setupOrderFulfillPage()

        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(WCMatchers.scrollTo(), click())

        // verify that the deleted item is removed from the shipment tracking list
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

        // mock api success response
        val orderFulfillmentFragment = getOrderFulfillmentFragment()
        val onOrderChangedSuccessResponse = OnOrderChanged(1).apply {
            causeOfChange = DELETE_ORDER_SHIPMENT_TRACKING
        }
        doAnswer {
            (orderFulfillmentFragment?.presenter as? OrderFulfillmentPresenter)
                    ?.onOrderChanged(onOrderChangedSuccessResponse)
        }. whenever(orderFulfillmentFragment?.presenter)?.deleteOrderShipmentTracking(any())

        // TODO: find an alternate solution for this
        Thread.sleep(5000)

        // check if the success snack is displayed
        onView(allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.order_shipment_tracking_delete_success))
        ).check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the deleted item is not added back to the list
        assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickWhenResponseFailure() {
        setupOrderFulfillPage()
        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(WCMatchers.scrollTo(), click())

        // verify that the deleted item is removed from the shipment tracking list
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

        // mock api error response
        val orderFulfillmentFragment = getOrderFulfillmentFragment()
        val onOrderChangedErrorResponse = OnOrderChanged(1).apply {
            causeOfChange = DELETE_ORDER_SHIPMENT_TRACKING
            error = OrderError()
        }
        doAnswer {
            (orderFulfillmentFragment?.presenter as? OrderFulfillmentPresenter)
                    ?.onOrderChanged(onOrderChangedErrorResponse)
        }. whenever(orderFulfillmentFragment?.presenter)?.deleteOrderShipmentTracking(any())

        // TODO: find an alternate solution for this
        Thread.sleep(5000)

        // check if the error snack is displayed
        onView(allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.order_shipment_tracking_delete_error))
        ).check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the deleted item is added back to the list
        assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyAddShipmentTrackingSuccessResponse() {
        setupOrderFulfillPage(true)

        // set mock response to add shipment tracking fragment
        val onOrderChangedSuccessResponse = OnOrderChanged(1).apply {
            causeOfChange = ADD_ORDER_SHIPMENT_TRACKING
        }
        activityTestRule.setAddShipmentTrackingWithMockData(mockWCOrderModel, onOrderChangedSuccessResponse, true)

        // redirect to Add shipment tracking activity
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // inject mock data to the provider list screen
        // click on select provider & select a provider from the list
        // select the first item on the second section of the list: under United States
        activityTestRule.setOrderProviderListWithMockData()
        onView(withId(R.id.addTracking_editCarrier)).perform(click())
        onView(withId(R.id.addTrackingProviderList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click()))

        // type tracking info
        val trackingNum = "12121-12121-12121-12121"
        onView(withId(R.id.addTracking_number)).perform(ViewActions.clearText(), ViewActions.typeText(trackingNum))

        // click on "Add tracking" button
        onView(withId(R.id.menu_add)).perform(click())

        // verify that add tracking snackbar is displayed
        onView(allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.order_shipment_tracking_added))
        ).check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that redirected to previous screen
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(equalToIgnoringCase(
                                appContext.getString(string.orderdetail_order_fulfillment, "1")
                        ))))
    }

    @Test
    fun verifyAddShipmentTrackingErrorResponse() {
        setupOrderFulfillPage(true)

        // set mock response to add shipment tracking fragment
        val onOrderChangedErrorResponse = OnOrderChanged(1).apply {
            causeOfChange = ADD_ORDER_SHIPMENT_TRACKING
            error = OrderError()
        }
        activityTestRule.setAddShipmentTrackingWithMockData(mockWCOrderModel, onOrderChangedErrorResponse, true)

        // redirect to Add shipment tracking activity
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // inject mock data to the provider list screen
        // click on select provider & select a provider from the list
        // select the first item on the second section of the list: under United States
        activityTestRule.setOrderProviderListWithMockData()
        onView(withId(R.id.addTracking_editCarrier)).perform(click())
        onView(withId(R.id.addTrackingProviderList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click()))

        // type tracking info
        val trackingNum = "12121-12121-12121-12121"
        onView(withId(R.id.addTracking_number)).perform(ViewActions.clearText(), ViewActions.typeText(trackingNum))

        // click on "Add tracking" button
        onView(withId(R.id.menu_add)).perform(click())

        // verify that redirected to previous screen
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(
                        appContext.getString(R.string.orderdetail_order_fulfillment, "1")
                ))))

        // verify shipment tracking count is has not changed
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }
}
