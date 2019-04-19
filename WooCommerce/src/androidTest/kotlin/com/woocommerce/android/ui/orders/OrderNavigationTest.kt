package com.woocommerce.android.ui.orders

import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import junit.framework.Assert.assertNotSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderNavigationTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    @Before
    override fun setup() {
        super.setup()
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // Select the orders bottom menu option
        Espresso.onView(ViewMatchers.withId(R.id.orders)).perform(ViewActions.click())
        activityTestRule.setOrderListWithMockData(fetchOrdersFromDb())
    }

    @Test
    fun onClickOrderListItemDisplaysOrderDetailView() {
        val recyclerView = activityTestRule.activity.findViewById(R.id.ordersList) as RecyclerView

        // inject sample data to the order list fragment from the presenter and ensure that the order count > 0
        assertNotSame(0, recyclerView.adapter?.itemCount)

        // click on the first order in the list and check if redirected to order detail
/*       Espresso.onView(ViewMatchers.withId(R.id.ordersList))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, ViewActions.click())) */

        // Ensure that the toolbar title displays "Orders #" + order number
        /*
        Espresso.onView(ViewMatchers.withId(id.toolbar)).check(
                ViewAssertions.matches(
                        WCMatchers.withToolbarTitle(Matchers.equalToIgnoringCase(appContext.getString(string.orders)))
                )
        )
        */

        // Check that the "UP" navigation button is displayed in the toolbar

        // Clicking the device "back" button returns the user to the orders list screen.
        // The Toolbar title changes to "Orders" and the "UP" button is no longer shown.

        // Clicking the "Dashboard" bottom bar option loads the DashboardFragment,
        // then clicking the "Orders" bottom bar option loads the OrderListFragment
        // (so the detail view should no longer be visible)

        // Clicking the "Orders" bottom bar option loads the OrderListFragment and
        // Changes the Toolbar title to "Orders"
    }

    private fun fetchOrdersFromDb() : List<WCOrderModel> {
        val result = ArrayList<WCOrderModel>()
        val om1 = WCOrderModel(1).apply {
            billingFirstName = "John"
            billingLastName = "Peters"
            currency = "USD"
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            number = "51"
            status = "processing"
            total = "14.53"
        }

        val om2 = WCOrderModel(2).apply {
            billingFirstName = "Jane"
            billingLastName = "Masterson"
            currency = "CAD"
            dateCreated = "2017-12-08T16:11:13Z"
            localSiteId = 1
            number = "63"
            status = "pending"
            total = "106.00"
        }

        val om3 = WCOrderModel(2).apply {
            billingFirstName = "Mandy"
            billingLastName = "Sykes"
            currency = "USD"
            dateCreated = "2018-02-05T16:11:13Z"
            localSiteId = 1
            number = "14"
            status = "processing"
            total = "25.73"
        }

        val om4 = WCOrderModel(2).apply {
            billingFirstName = "Jennifer"
            billingLastName = "Johnson"
            currency = "CAD"
            dateCreated = "2018-02-06T09:11:13Z"
            localSiteId = 1
            number = "15"
            status = "pending, on-hold, complete"
            total = "106.00"
        }

        val om5 = WCOrderModel(2).apply {
            billingFirstName = "Christopher"
            billingLastName = "Jones"
            currency = "USD"
            dateCreated = "2018-02-05T16:11:13Z"
            localSiteId = 1
            number = "3"
            status = "pending"
            total = "106.00"
        }

        val om6 = WCOrderModel(2).apply {
            billingFirstName = "Carissa"
            billingLastName = "King"
            currency = "USD"
            dateCreated = "2018-02-02T16:11:13Z"
            localSiteId = 1
            number = "55"
            status = "pending, Custom 1,Custom 2,Custom 3"
            total = "106.00"
        }

        result.add(om1)
        result.add(om2)
        result.add(om3)
        result.add(om4)
        result.add(om5)
        result.add(om6)

        return result
    }
}
