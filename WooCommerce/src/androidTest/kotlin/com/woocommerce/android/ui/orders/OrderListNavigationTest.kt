package com.woocommerce.android.ui.orders

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderListNavigationTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

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
        onView(ViewMatchers.withId(R.id.orderListFragment)).perform(ViewActions.click())
    }

    @Test
    fun switchToOrderDetailViewFromOrderListView() {
        // ensure that the recyclerView order list count > 0
        val recyclerView = activityTestRule.activity.findViewById(R.id.ordersList) as RecyclerView
        assertNotSame(0, recyclerView.adapter?.itemCount)

        // add mock data to order detail screen
        activityTestRule.setOrderDetailWithMockData(WcOrderTestUtils.generateOrderDetail())

        // click on the first order in the list and check if redirected to order detail
        Espresso.onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Ensure that the toolbar title displays "Order #" + order number
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                equalToIgnoringCase(appContext.getString(R.string.orderdetail_orderstatus_ordernum, "1")))
        ))

        // Check that the "UP" navigation button is displayed in the toolbar
        onView(withContentDescription(R.string.abc_action_bar_up_description)).check(matches(isDisplayed()))

        // Clicking the "up" button in order detail screen returns the user to the orders list screen.
        // The Toolbar title changes to "Orders"
        // The "UP" button is no longer shown
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.orders)))))
        onView(withContentDescription(getString(R.string.abc_action_bar_up_description))).check(doesNotExist())
    }

    @Test
    fun switchBackToDashboardViewFromOrderDetailView() {
        // click on the first order in the list and check if redirected to order detail
        Espresso.onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, ViewActions.click()))

        // Clicking the "Dashboard" bottom bar option loads the DashboardFragment,
        // then clicking the "Orders" bottom bar option loads the OrderListFragment (so the detail view should no longer be visible)
        onView(withId(R.id.dashboardFragment)).perform(click())
        onView(withId(R.id.orderListFragment)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.orders)))))
    }

    @Test
    fun switchBackToOrderListViewFromOrderDetailView() {
        // click on the first order in the list and check if redirected to order detail
        Espresso.onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, ViewActions.click()))

        // Clicking the "Orders" bottom bar option loads the OrderListFragment and
        // Changes the Toolbar title to "Orders"
        onView(withId(R.id.orderListFragment)).perform(click())
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(appContext.getString(R.string.orders)))))
    }

    private fun getString(resId: Int): String {
        return getInstrumentation().targetContext.getString(resId)
    }
}
