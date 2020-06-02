package com.woocommerce.android.ui.orders

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.R
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class verifyOrderProcessingTest : TestBase() {
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

        // add mock data to order detail screen
        activityTestRule.setOrderDetailWithMockData(order = mockWCOrderModel)

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orders)).perform(click())

        //making sure all the tests start from same state.
        onView(withContentDescription("Processing")).perform(click())
    }

    @Test
    fun verifyOrderProcessing() {
        // go to Processing orders tab
       // onView(withContentDescription("Processing")).perform(click())

        // go to click on the first order with processing tag
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click Begin Fullfilment
        onView(withText("Begin fulfillment")).perform((click()))

        // click Mark Order Complete
        onView(withText("Mark Order Complete")).perform(click())

        // go back to Orders page
        onView(withContentDescription("Navigate up")).perform(click())

        // go back to Orders page
        onView(withContentDescription("Navigate up")).perform(click())

        // go to Processing orders tab
        onView(withContentDescription("All Orders")).perform(click())

    }

    @Test
    fun verifyOrderRefundFlow() {
        // go to click on the first order with processing tag
        onView(withId(R.id.ordersList))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        Thread.sleep(3000)

        // ** test errors out at clicking Issue Refund button
        onView(withText("ISSUE REFUND")).perform((click()))

        // go to Processing orders tab
        onView(withContentDescription("All Orders")).perform(click())
    }

    @Test
    fun verifySearchOrder() {
        //Navigate to All orders
        onView(withContentDescription("All Orders")).perform(click())

        // go to click on the first order with processing tag
        onView(withId(R.id.menu_search)).perform(click())

        onView((withId(R.id.orderStatusList)))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(3, click()));

        onView(withId(R.id.search_src_text)).check(matches(withHint("Orders: Completed")))
    }
}
