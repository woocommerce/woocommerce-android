package com.woocommerce.android.ui.orders

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderFulfillmentNavigationTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing")

    @Before
    override fun setup() {
        super.setup()
        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Set Order fulfillment with mock data
        activityTestRule.setOrderFulfillmentWithMockData(mockWCOrderModel)

        // Click on Orders tab in the bottom bar
        Espresso.onView(ViewMatchers.withId(R.id.orders)).perform(ViewActions.click())

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())
    }

    @Test
    fun verifyToolbarItemsDisplayedCorrectly() {
        // verify toolbar title is displayed correctly
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(equalToIgnoringCase(
                appContext.getString(R.string.orderdetail_order_fulfillment, "1")
        ))))

        // Check that the "UP" navigation button is displayed in the toolbar
        onView(withContentDescription(R.string.abc_action_bar_up_description)).check(matches(isDisplayed()))
    }

    @Test
    fun verifyClickingBackRedirectsToOrderDetailCorrectly() {
        // Clicking the "up" button in order fulfillment screen returns the user to the orders detail screen.
        // The Toolbar title changes to "Order #1"
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                equalToIgnoringCase(appContext.getString(R.string.orderdetail_orderstatus_ordernum, "1")))
        ))
    }
}
