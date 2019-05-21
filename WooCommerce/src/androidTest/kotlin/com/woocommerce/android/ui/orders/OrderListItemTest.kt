package com.woocommerce.android.ui.orders

import android.os.Build.VERSION
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.swipeDown
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderListItemTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val orders: List<WCOrderModel> = WcOrderTestUtils.generateOrders()

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
        onView(ViewMatchers.withId(R.id.orders)).perform(ViewActions.click())
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
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(1, R.id.orderNum))
                .check(matches(withText(appContext.getString(R.string.orderlist_item_order_num, orders[0].number))))

        // verify if the first order item order name matches the first item on the
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(1, R.id.orderName))
                .check(matches(ViewMatchers.withText(appContext.getString(
                        R.string.orderlist_item_order_name,
                        orders[0].billingFirstName,
                        orders[0].billingLastName
                ))))

        // verify if the first order item order total matches: $15.33
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(1, R.id.orderTotal))
                .check(matches(withText("$14.53")))
    }

    @Test
    fun verifyOrderListItemEmptyNameHandledCorrectly() {
        // verify if the first order item order name is displayed empty
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(1, R.id.orderName))
                .check(matches(ViewMatchers.withText(appContext.getString(
                        R.string.orderlist_item_order_name,
                        orders[0].billingFirstName,
                        orders[0].billingLastName
                ))))
    }

    @Test
    fun verifyOrderListItemLongNameHandledCorrectly() {
        // verify if the first order item order name is wrapped in the card
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(3, R.id.orderName))
                .check(matches(ViewMatchers.withText(appContext.getString(
                        R.string.orderlist_item_order_name,
                        orders[1].billingFirstName,
                        orders[1].billingLastName
                ))))

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
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(1, R.id.orderTotal))
                .check(matches(withText("$14.53")))

        // verify if the first order item order total matches: ca$14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(3, R.id.orderTotal))
                .check(matches(withText("CA$14.53")))

        // verify if the first order item order total matches: euro14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(5, R.id.orderTotal))
                .check(matches(withText("€14.53")))

        // verify if the first order item order total matches: inr14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(7, R.id.orderTotal))
                .check(matches(withText("₹14.53")))

        // scroll to the end of the recyclerview first to avoid
        //  No views in hierarchy found matching: RecyclerView with id:
        // com.woocommerce.android:id/ordersList at position: 9
        onView(withId(R.id.ordersList))
                .perform(scrollToPosition<RecyclerView.ViewHolder>(9))

        // verify if the first order item order total matches: A$14.53
        onView(WCMatchers.withRecyclerView(R.id.ordersList)
                .atPositionOnView(9, R.id.orderTotal))
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
                "Requires API 24 or higher due to getColor() method in GradientDrawable not available devices below API 24",
                VERSION.SDK_INT >= 24
        )

        // PROCESSING: Check if order status label name, label text color, label background color
        val processingStatusPosition = 1
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(processingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(orders[0].status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(processingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_processing_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(processingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_processing_bg)))

        // PENDING PAYMENT: Check if order status label name, label text color, label background color
        val pendingStatusPosition = 3
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(pendingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(orders[1].status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(pendingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_pending_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(pendingStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_pending_bg)))

        // ON HOLD: Check if order status label name, label text color, label background color
        val onHoldStatusPosition = 5
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(onHoldStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(orders[2].status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(onHoldStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_hold_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(onHoldStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_hold_bg)))

        // COMPLETED: Check if order status label name, label text color, label background color
        val completedStatusPosition = 7
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(completedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(orders[3].status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(completedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_completed_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(completedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_completed_bg)))

        // scroll to the end of the recyclerview first to avoid
        //  No views in hierarchy found matching: RecyclerView with id:
        // com.woocommerce.android:id/ordersList at position: 9
        onView(withId(R.id.ordersList)).perform(scrollToPosition<RecyclerView.ViewHolder>(11))

        // CANCELLED: Check if order status label name, label text color, label background color
        val cancelledStatusPosition = 9
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(cancelledStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(orders[4].status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(cancelledStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_cancelled_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(cancelledStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_cancelled_bg)))

        // REFUNDED: Check if order status label name, label text color, label background color
        val refundedStatusPosition = 10
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(refundedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(orders[5].status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(refundedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_refunded_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(refundedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_refunded_bg)))

        // FAILED: Check if order status label name, label text color, label background color
        val failedStatusPosition = 11
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(failedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagText(orders[6].status)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(failedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagTextColor(appContext, R.color.orderStatus_failed_text)))
        onView(WCMatchers.withRecyclerView(R.id.ordersList).atPositionOnView(failedStatusPosition, R.id.orderTags))
                .check(matches(WCMatchers.withTagBackgroundColor(appContext, R.color.orderStatus_failed_bg)))
    }

    @Test
    fun verifyOrderListSectionTitleDisplayedCorrectly() {
        val recyclerView = activityTestRule.activity.findViewById(R.id.ordersList) as RecyclerView
        val orderListAdapter = recyclerView.adapter as OrderListAdapter

        // verify that there are 5 sections in the list:
        assertEquals(5, orderListAdapter.getSectionTotal())

        // verify the title of each of the section corresponds to
        // GROUP_TODAY, GROUP_YESTERDAY, GROUP_OLDER_TWO_DAYS, GROUP_OLDER_WEEK, GROUP_OLDER_MONTH
        assertTrue(orderListAdapter.isSectionAvailable(TimeGroup.GROUP_TODAY.name))
        assertTrue(orderListAdapter.isSectionAvailable(TimeGroup.GROUP_YESTERDAY.name))
        assertTrue(orderListAdapter.isSectionAvailable(TimeGroup.GROUP_OLDER_TWO_DAYS.name))
        assertTrue(orderListAdapter.isSectionAvailable(TimeGroup.GROUP_OLDER_WEEK.name))
        assertTrue(orderListAdapter.isSectionAvailable(TimeGroup.GROUP_OLDER_MONTH.name))
    }

    @Test
    fun verifyOrderListSectionItemCountDisplayedCorrectly() {
        val recyclerView = activityTestRule.activity.findViewById(R.id.ordersList) as RecyclerView
        val orderListAdapter = recyclerView.adapter as OrderListAdapter

        // verify the there is 1 item in the `GROUP_TODAY` section
        assertEquals(1, orderListAdapter.getSectionItemsTotal(TimeGroup.GROUP_TODAY.name))

        // verify the there is 1 item in the `GROUP_YESTERDAY` section
        assertEquals(1, orderListAdapter.getSectionItemsTotal(TimeGroup.GROUP_YESTERDAY.name))

        // verify the there is 1 item in the `GROUP_OLDER_TWO_DAYS` section
        assertEquals(1, orderListAdapter.getSectionItemsTotal(TimeGroup.GROUP_OLDER_TWO_DAYS.name))

        // verify the there is 1 item in the `GROUP_OLDER_WEEK` section
        assertEquals(1, orderListAdapter.getSectionItemsTotal(TimeGroup.GROUP_OLDER_WEEK.name))

        // verify the there is 1 item in the `GROUP_OLDER_MONTH` section
        assertEquals(3, orderListAdapter.getSectionItemsTotal(TimeGroup.GROUP_OLDER_MONTH.name))
    }
}
