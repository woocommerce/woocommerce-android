package com.woocommerce.android.ui.orders

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.Visibility.GONE
import android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.R
import com.woocommerce.android.R.id
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.helpers.WCMatchers.withRecyclerView
import com.woocommerce.android.helpers.WcHelperUtils
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.util.DateUtils
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
        val orderListFragment = activityTestRule.activity.supportFragmentManager
                .findFragmentByTag(OrderListFragment.TAG) as? OrderListFragment
        return orderListFragment?.childFragmentManager
                ?.findFragmentByTag(OrderFulfillmentFragment.TAG) as? OrderFulfillmentFragment
    }

    @Before
    override fun setup() {
        super.setup()
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
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())
    }

    @Test
    fun verifyOrderShipmentCardViewPopulatedSuccessfullyIfAvailable() {
        // check if shipment tracking list is 4
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)

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
        // click on the shipment tracking item - this should copy the text to clipboard
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_copyNumber))
                .perform(WCMatchers.scrollTo(), click())

        val clipboardText = WcHelperUtils.getClipboardText(appContext)
        Assert.assertEquals(mockShipmentTrackingList[0].trackingNumber, clipboardText)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickWhenOffline() {
        // mock network not available
        val orderFulfillmentFragment = getOrderFulfillmentFragment()
        doReturn(false).whenever(orderFulfillmentFragment?.networkStatus)?.isConnected()

        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(WCMatchers.scrollTo(), click())

        // check if the offline snack is displayed
        onView(allOf(withId(android.support.design.R.id.snackbar_text), withText(R.string.offline_error)))
                .check(matches(isDisplayed()))

        // verify that the shipment tracking list count matches the mock data count
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickUndoDeleteTracking() {
        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(WCMatchers.scrollTo(), click())

        // check if the shipment tracking list count has reduced by 1
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

        // check if the snackbar with undo button is displayed
        onView(allOf(
                withId(android.support.design.R.id.snackbar_text),
                withText(R.string.order_shipment_tracking_delete_snackbar_msg))
        ).check(matches(isDisplayed()))

        // click on the undo button
        onView(withText(appContext.getString(R.string.undo))).perform(click())

        // verify that the shipment tracking list count matches the mock data count
        Assert.assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickWhenResponseSuccess() {
        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(WCMatchers.scrollTo(), click())

        // verify that the deleted item is removed from the shipment tracking list
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

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
                withId(android.support.design.R.id.snackbar_text),
                withText(R.string.order_shipment_tracking_delete_success))
        ).check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the deleted item is not added back to the list
        Assert.assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickWhenResponseFailure() {
        // click on the "Delete shipment" icon
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .perform(WCMatchers.scrollTo(), click())

        // verify that the deleted item is removed from the shipment tracking list
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

        // mock api success response
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
                withId(android.support.design.R.id.snackbar_text),
                withText(R.string.order_shipment_tracking_delete_error))
        ).check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the deleted item is added back to the list
        Assert.assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }
}
