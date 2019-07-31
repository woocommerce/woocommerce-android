package com.woocommerce.android.ui.orders

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeRight
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
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.ui.main.getOrderDetailFragment
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderFulfillmentFragmentTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing")

    private fun redirectToOrderFulfillmentAndMarkOrderAsComplete(
        isNetworkConnected: Boolean = false,
        onOrderChanged: OnOrderChanged? = null
    ) {
        // Set Order Detail with mock data
        activityTestRule.setOrderDetailWithMockData(order = mockWCOrderModel, isNetworkConnected = isNetworkConnected)

        // Click on Orders tab in the bottom bar
        Espresso.onView(ViewMatchers.withId(R.id.orders)).perform(ViewActions.click())

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Set Order fulfillment with mock data
        activityTestRule.setOrderFulfillmentWithMockData(
                order = mockWCOrderModel,
                isNetworkConnected = isNetworkConnected,
                onOrderChanged = onOrderChanged
        )

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())

        // click on `Mark Order Complete` button
        onView(withId(R.id.orderFulfill_btnComplete)).perform(WCMatchers.scrollTo(), click())
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
    }

    @Test
    fun markOrderCompleteWhenNoNetworkAvailable() {
        redirectToOrderFulfillmentAndMarkOrderAsComplete()

        // verify that screen is not redirected to Order detail
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(equalToIgnoringCase(
                appContext.getString(R.string.orderdetail_order_fulfillment, "1")
        ))))
    }

    @Test
    fun markOrderCompleteWhenNetworkAvailableAndUndoClicked() {
        redirectToOrderFulfillmentAndMarkOrderAsComplete(true)

        // verify redirection to Order Detail page with order marked as complete
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                equalToIgnoringCase(appContext.getString(R.string.orderdetail_orderstatus_ordernum, "1")))
        ))

        // verify that fulfill button is hidden
        onView(withId(R.id.productList_btnFulfill)).check(matches(withEffectiveVisibility(GONE)))

        // verify undo snackbar is displayed
        onView(allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.order_fulfill_marked_complete))
        ).check(matches(isDisplayed()))

        // click on undo snackbar
        onView(withText(appContext.getString(R.string.undo))).perform(click())

        // verify that fulfill button is displayed
        onView(withId(R.id.productList_btnFulfill)).check(matches(withEffectiveVisibility(VISIBLE)))
    }

    @Test
    fun markOrderCompleteWhenNetworkAvailableResponseError() {
        // set mock error response when updating order status to complete
        val onOrderChangedErrorResponse = OnOrderChanged(1).apply {
            causeOfChange = UPDATE_ORDER_STATUS
            error = OrderError()
        }

        redirectToOrderFulfillmentAndMarkOrderAsComplete(true, onOrderChangedErrorResponse)

        // verify redirection to Order Detail page with order marked as complete
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                equalToIgnoringCase(appContext.getString(R.string.orderdetail_orderstatus_ordernum, "1")))
        ))

        // verify that fulfill button is hidden
        onView(withId(R.id.productList_btnFulfill)).check(matches(withEffectiveVisibility(GONE)))

        // mock network available
        val orderDetailFragment = activityTestRule.getOrderDetailFragment()
        doReturn(true).whenever(orderDetailFragment?.networkStatus)?.isConnected()

        // verify undo snackbar is displayed and swipe to dismiss it
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(withText(R.string.order_fulfill_marked_complete))).perform(swipeRight())

        Thread.sleep(1000) // Added to make it more reliable

        // check if the error snack is displayed
        onView(allOf(
                withId(com.google.android.material.R.id.snackbar_text),
                withText(R.string.order_error_update_general))
        ).check(matches(withEffectiveVisibility(VISIBLE)))
    }
}
