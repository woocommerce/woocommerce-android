package com.woocommerce.android.ui.orders

import android.os.Build.VERSION
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.ui.orders.list.OrderListItemUIType
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import org.junit.Assert.assertNotSame
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderListItemTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val orders: List<OrderListItemUIType> = WcOrderTestUtils.generateOrderListUIItems()

    @Before
    override fun setup() {
        super.setup()
        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData(orders)

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orders)).perform(ViewActions.click())
    }

    @Test
    fun verifyOrderListPopulatedSuccessfully() {
        // ensure that the recyclerView order list count = 7
        val recyclerView = activityTestRule.activity.findViewById(R.id.ordersList) as RecyclerView
        assertNotSame(0, recyclerView.adapter?.itemCount)

        // verify that pull-to-refresh is successful
        onView(withId(R.id.orderRefreshLayout)).perform(swipeDown())

        // ensure that the recyclerView order list count = 11
        assertNotSame(0, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyOrderListItemPopulatedSuccessfully() {
        // verify if the first order item order number matches: 100
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(2, R.id.orderNum))
                .check(matches(withText(getAsOrderItem(2).orderNumber)))

        // verify if the first order item order name matches the first item on the
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(2, R.id.orderName))
                .check(matches(withText(getAsOrderItem(2).orderName)))

        // verify if the first order item order total matches: $15.33
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(2, R.id.orderTotal))
                .check(matches(withText("$14.53")))
    }

    @Test
    fun verifyOrderListItemEmptyNameHandledCorrectly() {
        // verify if the first order item order name is displayed empty
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(2, R.id.orderName))
                .check(matches(withText(getAsOrderItem(2).orderName)))
    }

    @Test
    fun verifyOrderListItemLongNameHandledCorrectly() {
        // verify if the first order item order name is wrapped in the card
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(3, R.id.orderName))
                .check(matches(withText(getAsOrderItem(3).orderName)))

        // verify if the first order item order number is still displayed
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(3, R.id.orderNum))
                .check(matches(isCompletelyDisplayed()))

        // verify if the first order item order total is still displayed
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(3, R.id.orderTotal))
                .check(matches(isCompletelyDisplayed()))

        // verify if the first order item order order status is still displayed
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(3, R.id.orderTags))
                .check(matches(isCompletelyDisplayed()))
    }

    /**
     * Related FluxC issue in MockedStack_WCBaseStoreTest.kt#L116
     * Some of the internals of java.util.Currency seem to be stubbed in a unit test environment,
     * giving results inconsistent with a normal running app
     */
    @Test
    fun verifyOrderListItemTotalDisplayedCorrectlyForMultipleCurrencies() {
        Assume.assumeTrue(
                "Requires API 23 or higher due to localized currency values differing on older versions",
                VERSION.SDK_INT >= 23
        )

        // verify if the first order item order total matches: $14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(2, R.id.orderTotal))
                .check(matches(withText("$14.53")))

        // verify if the first order item order total matches: ca$14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(3, R.id.orderTotal))
                .check(matches(withText("CA$14.53")))

        // verify if the first order item order total matches: euro14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(5, R.id.orderTotal))
                .check(matches(withText("€14.53")))

        // verify if the first order item order total matches: inr14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(6, R.id.orderTotal))
                .check(matches(withText("₹14.53")))

        // scroll to the end of the recyclerview first to avoid
        //  No views in hierarchy found matching: RecyclerView with id:
        // com.woocommerce.android:id/ordersList at position: 9
        onView(withId(R.id.ordersList))
                .perform(scrollToPosition<RecyclerView.ViewHolder>(9))

        // verify if the first order item order total matches: A$14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList)
                .atPositionOnView(7, R.id.orderTotal))
                .check(matches(withText("A$14.53")))
    }

    /**
     * This test checks if the order status label name, label text color and label background color matches the
     * corresponding order status. In order to check background color of the TagView, we need to get the
     * background color of the GradientDrawable and check if it matches. The getColor() method in GradientDrawable
     * is only available on devices above API 23. So adding a check here that assumes the device testing takes place
     * in devices above API 23.
     */
    @Test
    fun verifyOrderListItemOrderStatusLabelDisplayedCorrectly() {
        Assume.assumeTrue(
                "Requires API 24 or higher due to getColor() method in GradientDrawable not available " +
                        "in devices below API 24",
                VERSION.SDK_INT >= 24
        )

        // PROCESSING: Check if order status label name, label text color, label background color
        val processingStatusPosition = 2
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(processingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(getAsOrderItem(processingStatusPosition).status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(processingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_processing_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(processingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_processing_bg)))

        // PENDING PAYMENT: Check if order status label name, label text color, label background color
        val pendingStatusPosition = 3
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(pendingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(getAsOrderItem(pendingStatusPosition).status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(pendingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_pending_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(pendingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_pending_bg)))

        // ON HOLD: Check if order status label name, label text color, label background color
        val onHoldStatusPosition = 5
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(onHoldStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(getAsOrderItem(onHoldStatusPosition).status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(onHoldStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_hold_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(onHoldStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_hold_bg)))

        // COMPLETED: Check if order status label name, label text color, label background color
        val completedStatusPosition = 6
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(completedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(getAsOrderItem(completedStatusPosition).status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(completedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_completed_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(completedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_completed_bg)))

        // scroll to the end of the recyclerview first to avoid
        //  No views in hierarchy found matching: RecyclerView with id:
        // com.woocommerce.android:id/ordersList at position: 6
        onView(withId(R.id.ordersList)).perform(scrollToPosition<RecyclerView.ViewHolder>(9))

        // CANCELLED: Check if order status label name, label text color, label background color
        val cancelledStatusPosition = 7
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(cancelledStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(getAsOrderItem(cancelledStatusPosition).status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(cancelledStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_cancelled_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(cancelledStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_cancelled_bg)))

        // REFUNDED: Check if order status label name, label text color, label background color
        val refundedStatusPosition = 8
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(refundedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(getAsOrderItem(refundedStatusPosition).status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(refundedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_refunded_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(refundedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_refunded_bg)))

        // FAILED: Check if order status label name, label text color, label background color
        val failedStatusPosition = 9
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(failedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(getAsOrderItem(failedStatusPosition).status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(failedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_failed_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(failedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_failed_bg)))
    }

    @Test
    fun verifySectionHeadersPopulatedSuccessfully() {
        // Verify TODAY
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(0, R.id.orderListHeader))
                .check(matches(withText(appContext.getString(R.string.today))))

        // Verify OLDER THAN MONTH
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(4, R.id.orderListHeader))
                .check(matches(withText(appContext.getString(R.string.date_timeframe_older_month))))
    }

    @Test
    fun verifyLoadingItemHandledCorrectly() {
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(1, R.id.orderListLoading))
                .check(matches(isDisplayed()))
    }

    private fun getAsOrderItem(pos: Int) = orders[pos] as OrderListItemUI
}
