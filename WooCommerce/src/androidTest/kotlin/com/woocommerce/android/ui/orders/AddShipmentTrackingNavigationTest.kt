package com.woocommerce.android.ui.orders

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.RootMatchers.isDialog
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withContentDescription
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddShipmentTrackingNavigationTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = AddShipmentTrackingActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing")

    @Test
    fun verifyErrorDisplayedOnProviderFieldWhenAddButtonClicked() {
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), "")

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
        val providerName = "Fedex"
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), providerName)

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
        val providerName = "Anitaa Test Inc"
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(
                mockWCOrderModel.getIdentifier(), providerName, true
        )

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
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(
                mockWCOrderModel.getIdentifier(), providerName, true
        )

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
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(
                mockWCOrderModel.getIdentifier(), providerName, false
        )

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
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(
                mockWCOrderModel.getIdentifier(), providerName, false
        )

        // Clicking the "up" button in order detail screen returns the user to the orders fulfill screen.
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())

        // Click on `Discard` to dismiss the screen
        onView(withText(R.string.discard)).inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        // verify that app is closed
        assertTrue(activityTestRule.activity.isFinishing)
    }

    @Test
    fun verifyDiscardDialogKeepEditingButtonClickDismissesDialog() {
        val providerName = "Fedex"
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(
                mockWCOrderModel.getIdentifier(), providerName, false
        )

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
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(
                mockWCOrderModel.getIdentifier(), providerName, false
        )

        // Clicking the "up" button in order detail screen returns the user to the orders fulfill screen.
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())

        // app should exit
        // verify that app is closed
        assertTrue(activityTestRule.activity.isFinishing)
    }
}
