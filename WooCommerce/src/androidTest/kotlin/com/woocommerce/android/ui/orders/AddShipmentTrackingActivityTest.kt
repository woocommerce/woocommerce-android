package com.woocommerce.android.ui.orders

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.pressBack
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withClassName
import android.support.test.espresso.matcher.ViewMatchers.withContentDescription
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import android.widget.DatePicker
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.util.DateUtils
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddShipmentTrackingActivityTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing")
    private val mockShipmentTrackingList = WcOrderTestUtils.generateOrderShipmentTrackings()

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

        // setting the default provider value to empty here
        AppPrefs.setSelectedShipmentTrackingProviderName("")

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())

        // click on Add Tracking button to redirect to Add Shipment tracking
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())
    }

    @Test
    fun verifyOnBackClickOpensOrderFulfillment() {
        // Clicking the "up" button in order detail screen returns the user to the orders fulfill screen.
        // The Toolbar title changes to "Fulfill Order #1"
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())

        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(
                        appContext.getString(R.string.orderdetail_order_fulfillment, "1")
                ))))
    }

    @Test
    fun verifyDatePickerDialogDisplayedAndSelectedWhenDateFieldClicked() {
        val calendar = Calendar.getInstance()

        // verify date displayed in field is current date
        val currentDate = "${calendar.get(YEAR)}-${calendar.get(MONTH) + 1}-${calendar.get(DAY_OF_MONTH)}"
        onView(withId(R.id.addTracking_date)).check(matches(withText(
                DateUtils.getLocalizedLongDateString(appContext, currentDate)
        )))

        // click on the date shipped field
        onView(withId(R.id.addTracking_date)).perform(click())

        // verify that calendar dialog is opened
        onView(withClassName(equalTo(DatePicker::class.java.name)))
                .check(matches(isDisplayed()))

        // select a date from the dialog
        onView(withClassName(equalTo(DatePicker::class.java.name)))
                .perform(PickerActions.setDate(calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1, 2))

        // click on ok button inside dialog
        onView(withId(android.R.id.button1)).perform(click())

        // verify that the date selected is displayed correctly
        onView(withId(R.id.addTracking_date)).check(matches(withText(
                DateUtils.getLocalizedLongDateString(appContext,
                        "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-02")
        )))
    }

    @Test
    fun verifyDatePickerDialogCancelClicked() {
        val calendar = Calendar.getInstance()

        // verify date displayed in field is current date
        val currentDate = "${calendar.get(YEAR)}-${calendar.get(MONTH) + 1}-${calendar.get(DAY_OF_MONTH)}"
        onView(withId(R.id.addTracking_date)).check(matches(withText(
                DateUtils.getLocalizedLongDateString(appContext, currentDate)
        )))

        // click on the date shipped field
        onView(withId(R.id.addTracking_date)).perform(click())

        // verify that calendar dialog is opened
        onView(withClassName(equalTo(DatePicker::class.java.name)))
                .check(matches(isDisplayed()))

        // select a date from the dialog
        onView(withClassName(equalTo(DatePicker::class.java.name)))
                .perform(PickerActions.setDate(
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, 2
                ))

        // click on cancel button inside dialog
        onView(withId(android.R.id.button2)).perform(click())

        // verify that the date selected is displayed correctly
        onView(withId(R.id.addTracking_date)).check(matches(withText(
                DateUtils.getLocalizedLongDateString(appContext, currentDate)
        )))
    }

    @Test
    fun clickSelectProviderOpensProviderListDialog() {
        // check that the provider text is empty
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText("")))

        // click on select provider button
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // verify that the toolbar title matches: order_shipment_tracking_provider_toolbar_title
        onView(withId(R.id.toolbar)).check(matches(
                WCMatchers.withToolbarTitle(equalToIgnoringCase(
                        appContext.getString(R.string.order_shipment_tracking_provider_toolbar_title)
                ))))

        // clicking back button closes dialog
        pressBack()

        // verify toolbar title has changed back
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(equalToIgnoringCase(
                appContext.getString(R.string.order_shipment_tracking_toolbar_title)
        ))))

        // check that the provider text is still empty
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText("")))
    }
}
