package com.woocommerce.android.ui.orders

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddShipmentTrackingNavigationTest : TestBase() {
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

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orderListFragment)).perform(click())

        // add mock data to order detail screen
        activityTestRule.setOrderDetailWithMockData(order = mockWCOrderModel)

        // click on a single order from the list
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
    }

    @Test
    fun verifyErrorDisplayedOnProviderFieldWhenAddButtonClicked() {
        // setting the default provider value to empty here
        AppPrefs.setSelectedShipmentTrackingProviderName("")
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(false)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify that provider text is empty
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText("")))

        // click on add menu button
        onView(withId(R.id.menu_add)).perform(click())

        // verify that error is displayed
        onView(withId(R.id.addTracking_editCarrier)).check(matches(WCMatchers.matchesError(
                appContext.getString(R.string.order_shipment_tracking_empty_provider)
        )))
    }

    @Test
    fun verifyErrorDisplayedOnTrackingNumFieldWhenAddButtonClicked() {
        // launch activity with default provider name
        val providerName = "Fedex"
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(false)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify that provider text is not empty
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText(providerName)))

        // verify that tracking number text is empty
        onView(withId(R.id.addTracking_number)).check(matches(withText("")))

        // click on add menu button
        onView(withId(R.id.menu_add)).perform(click())

        // verify that error is displayed
        onView(withId(R.id.addTracking_number)).check(matches(WCMatchers.matchesError(
                appContext.getString(R.string.order_shipment_tracking_empty_tracking_num)
        )))
    }

    @Test
    fun verifyErrorDisplayedOnTrackingNumFieldWhenAddButtonClickedForCustomProvider() {
        // launch activity with custom provider name
        val providerName = "Anitaa Test Inc"
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(true)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify that provider text = Custom provider
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText(
                appContext.getString(R.string.order_shipment_tracking_custom_provider_section_name)
        )))

        // verify that custom provider name text = providerName
        onView(withId(R.id.addTracking_custom_provider_name)).check(matches(withText(providerName)))

        // verify that tracking number text is empty
        onView(withId(R.id.addTracking_number)).check(matches(withText("")))

        // click on add menu button
        onView(withId(R.id.menu_add)).perform(click())

        // verify that error is displayed
        onView(withId(R.id.addTracking_number)).check(matches(WCMatchers.matchesError(
                appContext.getString(R.string.order_shipment_tracking_empty_tracking_num)
        )))

        // verify that error is not displayed for select provider field
        onView(withId(R.id.addTracking_editCarrier)).check(matches(WCMatchers.matchesHasNoErrorText()))

        // verify that error is not displayed for custom provider name field
        onView(withId(R.id.addTracking_custom_provider_name)).check(matches(WCMatchers.matchesHasNoErrorText()))
    }

    @Test
    fun verifyErrorDisplayedOnCustomProviderFieldWhenAddButtonClickedForCustomProvider() {
        val providerName = ""
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(true)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify that provider text = Custom provider
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText(
                appContext.getString(R.string.order_shipment_tracking_custom_provider_section_name)
        )))

        // verify that custom provider name text = providerName
        onView(withId(R.id.addTracking_custom_provider_name)).check(matches(withText(providerName)))

        // verify that the custom provider text is empty
        onView(withId(R.id.addTracking_custom_provider_name)).check(matches(withText("")))

        // click on add menu button
        onView(withId(R.id.menu_add)).perform(click())

        // verify that error is displayed
        onView(withId(R.id.addTracking_custom_provider_name)).check(matches(WCMatchers.matchesError(
                appContext.getString(R.string.order_shipment_tracking_empty_custom_provider_name)
        )))

        // verify that error is not displayed for select provider field
        onView(withId(R.id.addTracking_editCarrier)).check(matches(WCMatchers.matchesHasNoErrorText()))

        // verify that error is not displayed for select provider field
        onView(withId(R.id.addTracking_number)).check(matches(WCMatchers.matchesHasNoErrorText()))
    }

    @Test
    fun verifyToolbarItemsDisplayedCorrectly() {
        val providerName = ""
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(false)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify toolbar title is displayed correctly
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(equalToIgnoringCase(
                appContext.getString(R.string.order_shipment_tracking_toolbar_title)
        ))))

        // verify that the close buttom is displayed
        // Check that the "UP" navigation button is displayed in the toolbar
        onView(withContentDescription(R.string.abc_action_bar_up_description)).check(matches(isDisplayed()))

        // verify that aad button is displayed
        onView(withId(R.id.menu_add)).check(matches(isDisplayed()))
    }

    @Test
    fun verifyDiscardDialogDisplayedWhenTrackingDetailsEntered() {
        val providerName = "Fedex"
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(false)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // Clicking the "up" button in order detail screen returns the user to the orders fulfill screen.
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())

        // Click on `Discard` to dismiss the screen
        onView(withText(R.string.discard)).inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        // verify that order detail screen is displayed
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                Matchers.equalToIgnoringCase(
                        appContext.getString(string.orderdetail_orderstatus_ordernum, "1")
                ))))
    }

    @Test
    fun verifyDiscardDialogKeepEditingButtonClickDismissesDialog() {
        val providerName = "Fedex"
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(false)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // Clicking the "up" button in order detail screen returns the user to the orders fulfill screen.
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())

        // Click on `Discard` to dismiss the screen
        onView(withText(R.string.keep_editing)).inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        // verify that dialog is no longer displayed by checking if the toolbar title is displayed
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
    }

    @Test
    fun verifyDiscardDialogNotDisplayedWhenFieldsEmpty() {
        val providerName = ""
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(false)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // Clicking the "up" button in order detail screen returns the user to the orders fulfill screen.
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())

        // verify that order detail screen is displayed
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                Matchers.equalToIgnoringCase(
                        appContext.getString(string.orderdetail_orderstatus_ordernum, "1")
                ))))
    }
}
