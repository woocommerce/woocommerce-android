package com.woocommerce.android.ui.orders

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.matcher.RootMatchers
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
import android.widget.ListView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.helpers.WCMatchers.withRecyclerView
import com.woocommerce.android.helpers.WcHelperUtils
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.util.DateUtils
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.allOf
import org.junit.After
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
class OrderDetailShipmentTrackingCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
    private val mockShipmentTrackingList = WcOrderTestUtils.generateOrderShipmentTrackings()

    /**
     * Helper method to update the network status for the current fragment to test
     * offline scenarios
     */
    private fun getOrderDetailFragment(): OrderDetailFragment? {
        val orderListFragment = activityTestRule.activity.supportFragmentManager
                .findFragmentByTag(OrderListFragment.TAG) as? OrderListFragment
        return orderListFragment?.childFragmentManager
                ?.findFragmentByTag(OrderDetailFragment.TAG) as? OrderDetailFragment
    }

    @Before
    override fun setup() {
        super.setup()
        Intents.init()

        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
                .respondWith(ActivityResult(Activity.RESULT_OK, null))

        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orders)).perform(click())
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun verifyOrderShipmentCardViewHiddenIfNotAvailable() {
        activityTestRule.setOrderDetailWithMockData(order = mockWCOrderModel, orderShipmentTrackings = emptyList())

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Since trackings are empty, the shipment tracking card should be hidden
        onView(withId(R.id.orderDetail_shipmentList)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyOrderShipmentCardViewPopulatedSuccessfullyIfAvailable() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Since trackings are not empty, the shipment tracking card should be visible
        onView(withId(R.id.orderDetail_shipmentList)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // check if shipment tracking list is 4
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)

        // verify that the Add tracking button is not visible
        onView(withId(R.id.shipmentTrack_btnAddTracking)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))

        // verify that the shipment tracking title is displayed correctly
        onView(withId(R.id.shipmentTrack_label)).check(matches(
                ViewMatchers.withText(appContext.getString(string.order_shipment_tracking))
        ))
    }

    @Test
    fun verifyShipmentTrackingItemDetailsPopulatedSuccessfully() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify if the first shipment tracking name matches the first item from the mock data
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_type))
                .check(matches(withText(mockShipmentTrackingList[0].trackingProvider)))

        // verify if the first shipment tracking name matches the first item from the mock data
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_number))
                .check(matches(withText(mockShipmentTrackingList[0].trackingNumber)))

        // verify if the first shipment tracking name matches the first item from the mock data
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_dateShipped))
                .check(matches(withText(appContext.getString(
                        R.string.order_shipment_tracking_shipped_date,
                        DateUtils.getLocalizedLongDateString(appContext, mockShipmentTrackingList[0].dateShipped)
                ))))

        // verify that the Delete tracking button is not visible
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnDelete))
                .check(matches(ViewMatchers.withEffectiveVisibility(GONE)))

        // verify that the Hamburger icon button is visible
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))
    }

    @Test
    fun verifyShipmentTrackingItemPopupDisplayedSuccessfully() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the Hamburger icon button for first item is clicked and popup is opened
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .perform(WCMatchers.scrollTo(), click())

        // verify that since the first item in the shipment tracking list has a tracking link, the popup item count = 2
        onView(ViewMatchers.isAssignableFrom(ListView::class.java))
                .check(matches(WCMatchers.withItemCount(2)))

        // verify that the popup menu is displayed & the first item in the list is Track shipment
        onView(withText(appContext.getString(R.string.orderdetail_track_shipment)))
                .inRoot(RootMatchers.isPlatformPopup()).check(matches(ViewMatchers.isDisplayed()))

        // verify that the popup menu is displayed & the second item in the list is Delete shipment
        onView(withText(appContext.getString(R.string.orderdetail_delete_tracking)))
                .inRoot(RootMatchers.isPlatformPopup()).check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun verifyShipmentTrackingItemPopupItemDisplayedSuccessfullyIfTrackingLinkNotAvailable() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the Hamburger icon button for first item is clicked and popup is opened
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(1, R.id.tracking_btnTrack))
                .perform(WCMatchers.scrollTo(), click())

        // verify that since the first item in the shipment tracking list has a tracking link, the popup item count = 2
        onView(ViewMatchers.isAssignableFrom(ListView::class.java))
                .check(matches(WCMatchers.withItemCount(1)))

        // verify that the popup menu is displayed & the first item in the list is Delete shipment
        onView(withText(appContext.getString(R.string.orderdetail_delete_tracking)))
                .inRoot(RootMatchers.isPlatformPopup()).check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun verifyShipmentTrackingItemPopupItemClickOpensWebView() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the Hamburger icon button for first item is clicked and popup is opened
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .perform(WCMatchers.scrollTo(), click())

        // click on the popup menu item "Track shipment"
        onView(withText(appContext.getString(R.string.orderdetail_track_shipment)))
                .inRoot(RootMatchers.isPlatformPopup()).perform(click())

        // check if webview intent is opened for the given url
        Intents.intended(
                CoreMatchers.allOf(
                        IntentMatchers.hasAction(Intent.ACTION_VIEW), IntentMatchers.hasData(
                        mockShipmentTrackingList[0].trackingLink
                )
                )
        )
    }

    @Test
    fun verifyShipmentTrackingItemClickCopiesToClipboard() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on the shipment tracking item - this should copy the text to clipboard
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_copyNumber))
                .perform(WCMatchers.scrollTo(), click())

        // tests are failing in devices below api 27 when getting clipboard text without handler
        Handler(Looper.getMainLooper()).post {
            val clipboardText = WcHelperUtils.getClipboardText(appContext)
            Assert.assertEquals(mockShipmentTrackingList[0].trackingNumber, clipboardText)
        }
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickWhenOffline() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the Hamburger icon button for first item is clicked and popup is opened
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .perform(WCMatchers.scrollTo(), click())

        // mock network not available
        val orderDetailFragment = getOrderDetailFragment()
        doReturn(false).whenever(orderDetailFragment?.networkStatus)?.isConnected()

        // click on the popup menu item "Delete shipment"
        onView(withText(appContext.getString(R.string.orderdetail_delete_tracking)))
                .inRoot(RootMatchers.isPlatformPopup()).perform(click())

        // check if the offline snack is displayed
        onView(allOf(
                withId(android.support.design.R.id.snackbar_text),
                withText(R.string.offline_error))
        ).check(matches(isDisplayed()))

        // verify that the shipment tracking list count matches the mock data count
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyShipmentTrackingItemDeleteItemClickUndoDeleteTracking() {
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the Hamburger icon button for first item is clicked and popup is opened
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .perform(WCMatchers.scrollTo(), click())

        // click on the popup menu item "Delete shipment"
        onView(withText(appContext.getString(R.string.orderdetail_delete_tracking)))
                .inRoot(RootMatchers.isPlatformPopup()).perform(click())

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
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the Hamburger icon button for first item is clicked and popup is opened
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .perform(WCMatchers.scrollTo(), click())

        // click on the popup menu item "Delete shipment"
        onView(withText(appContext.getString(R.string.orderdetail_delete_tracking)))
                .inRoot(RootMatchers.isPlatformPopup()).perform(click())

        // verify that the deleted item is removed from the shipment tracking list
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

        // mock api success response
        val orderDetailFragment = getOrderDetailFragment()
        val onOrderChangedSuccessResponse = OnOrderChanged(1).apply {
            causeOfChange = DELETE_ORDER_SHIPMENT_TRACKING
        }
        doAnswer {
            (orderDetailFragment?.presenter as? OrderDetailPresenter)?.onOrderChanged(onOrderChangedSuccessResponse)
        }. whenever(orderDetailFragment?.presenter)?.deleteOrderShipmentTracking(any())

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
        activityTestRule.setOrderDetailWithMockData(
                order = mockWCOrderModel,
                orderShipmentTrackings = mockShipmentTrackingList
        )

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the Hamburger icon button for first item is clicked and popup is opened
        onView(withRecyclerView(R.id.shipmentTrack_items).atPositionOnView(0, R.id.tracking_btnTrack))
                .perform(WCMatchers.scrollTo(), click())

        // click on the popup menu item "Delete shipment"
        onView(withText(appContext.getString(R.string.orderdetail_delete_tracking)))
                .inRoot(RootMatchers.isPlatformPopup()).perform(click())

        // verify that the deleted item is removed from the shipment tracking list
        val recyclerView = activityTestRule.activity.findViewById(R.id.shipmentTrack_items) as RecyclerView
        Assert.assertSame(mockShipmentTrackingList.size - 1, recyclerView.adapter?.itemCount)

        // mock api success response
        val orderDetailFragment = getOrderDetailFragment()
        val onOrderChangedErrorResponse = OnOrderChanged(1).apply {
            causeOfChange = DELETE_ORDER_SHIPMENT_TRACKING
            error = OrderError()
        }
        doAnswer {
            (orderDetailFragment?.presenter as? OrderDetailPresenter)?.onOrderChanged(onOrderChangedErrorResponse)
        }. whenever(orderDetailFragment?.presenter)?.deleteOrderShipmentTracking(any())

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
