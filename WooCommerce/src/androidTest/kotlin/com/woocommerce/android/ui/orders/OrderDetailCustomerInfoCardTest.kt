package com.woocommerce.android.ui.orders

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.util.AddressUtils
import com.woocommerce.android.util.PhoneUtils
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderDetailCustomerInfoCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    @Before
    override fun setup() {
        super.setup()
        Intents.init()

        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        intending(not(isInternal())).respondWith(ActivityResult(Activity.RESULT_OK, null))

        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orderListFragment)).perform(click())
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun verifyOrderDetailCardViewPopulatedSuccessfully() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta",
                billingCountry = "USA"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order customer info card label matches this title:
        // R.string.customer_information
        onView(withId(R.id.customerInfo_label)).check(matches(
                withText(appContext.getString(R.string.customer_information))
        ))

        // check if order customer info card shipping label matches this title:
        // R.string.customer_information
        onView(withId(R.id.customerInfo_shippingLabel)).check(matches(
                withText(appContext.getString(R.string.orderdetail_shipping_details))
        ))

        // check if order customer info card shipping label matches this title:
        // R.string.customer_information
        onView(withId(R.id.customerInfo_billingLabel)).check(matches(
                withText(appContext.getString(R.string.orderdetail_billing_details))
        ))

        // verify that the customer shipping details is displayed
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(isDisplayed()))

        // verify that the billing view is condensed and load more button is visible
        onView(withId(R.id.customerInfo_morePanel)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_viewMore)).check(matches(isNotChecked()))
        onView(withId(R.id.customerInfo_viewMore)).check(matches(
                withText(appContext.getString(R.string.orderdetail_show_billing))
        ))
    }

    @Test
    fun verifyOrderDetailCardViewShowBillingViewPopulatedSuccessfully() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingAddress1 = "Ramada Plaza, 450 Capitol",
                billingCountry = "USA"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())

        // verify that the billing view is expanded and load more button is visible
        onView(withId(R.id.customerInfo_morePanel)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_viewMore)).check(matches(isChecked()))
        onView(withId(R.id.customerInfo_viewMore)).check(matches(
                withText(appContext.getString(R.string.orderdetail_hide_billing))
        ))

        // verify that billing details is displayed
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(isDisplayed()))

        // verify that the customer email is displayed
        onView(withId(R.id.customerInfo_emailAddr)).perform(WCMatchers.scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.customerInfo_emailBtn)).perform(WCMatchers.scrollTo()).check(matches(isDisplayed()))

        // since the customer info phone is empty, the view should not be displayed
        onView(withId(R.id.customerInfo_phone)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_callOrMessageBtn)).check(matches(withEffectiveVisibility(GONE)))

        // click on Hide Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())

        // verify that the billing view is condensed and load more button is visible
        onView(withId(R.id.customerInfo_morePanel)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_viewMore)).check(matches(isNotChecked()))
        onView(withId(R.id.customerInfo_viewMore)).check(matches(
                withText(appContext.getString(R.string.orderdetail_show_billing))
        ))
    }

    @Test
    fun verifyOrderDetailCardViewBillingMatchesShippingPopulatedSuccessfully() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta",
                billingCountry = "USA"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order customer info card shipping details matches this format:
        // Anitaa Murthy
        // 60, Fast lane, Chicago,
        // USA
        val billingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.billingFirstName, mockWCOrderModel.billingLastName
        )
        val billingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.billingCountry)

        // Assumes that the name, address and country info is available
        val billingAddrFull = "$billingName\n$billingAddr\n$billingCountry"
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(billingAddrFull)))

        // Note: the shipping address should match the billing address
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(withText(billingAddrFull)))
    }

    @Test
    fun verifyOrderDetailCardViewWithSeparateBillingPopulatedSuccessfully() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta",
                billingCountry = "USA",
                shippingFirstName = "Anitaa",
                shippingLastName = "Murthy",
                shippingAddress1 = "1234, Abs avenue",
                shippingCountry = "USA"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order customer info card shipping details matches this format:
        // Anitaa Murthy
        // 60, Fast lane, Chicago,
        // USA
        val shippingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.shippingFirstName, mockWCOrderModel.shippingLastName
        )
        val shippingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getShippingAddress())
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.shippingCountry)

        // Assumes that the shipping name, address and country info is available
        // & is different from the billing address
        val shippingAddrFull = "$shippingName\n$shippingAddr\n$shippingCountry"
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(shippingAddrFull)))

        // Assumes that the billing name, address and country info is available
        // & is different from the shipping address
        val billingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.billingFirstName, mockWCOrderModel.billingLastName
        )
        val billingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.billingCountry)
        val billingAddrFull = "$billingName\n$billingAddr\n$billingCountry"
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(withText(billingAddrFull)))
    }

    @Test
    fun verifyOrderDetailCardViewWithShippingForDifferentLocalePopulatedSuccessfully() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingAddress1 = "29, Kuppam Beach Road",
                billingCountry = "India",
                billingPostalCode = "600041"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order customer info card shipping details matches this format:
        // Anitaa Murthy
        // 29, Kuppam Beach Road,
        // India, 600041
        val billingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.billingFirstName, mockWCOrderModel.billingLastName
        )
        val billingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.billingCountry)

        // Assumes that the name, address and country info is available
        val billingAddrFull = "$billingName\n$billingAddr\n$billingCountry"
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(billingAddrFull)))
    }

    @Test
    fun verifyOrderDetailCardViewWithBillingPhonePopulatedSuccessfully() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingPhone = "9962789522"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(click())

        // since the customer info phone is NOT empty, the view should be displayed
        onView(withId(R.id.customerInfo_phone)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_callOrMessageBtn)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_phone)).check(matches(
                withText(PhoneUtils.formatPhone(mockWCOrderModel.billingPhone))
        ))
    }

    @Test
    fun verifyOrderDetailCardViewWithBillingPhoneForDifferentLocalePopulatedSuccessfully() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingPhone = "07911123456"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(click())

        // since the customer info phone is NOT empty, the view should be displayed
        onView(withId(R.id.customerInfo_phone)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_callOrMessageBtn)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_phone)).check(matches(
                withText(PhoneUtils.formatPhone(mockWCOrderModel.billingPhone))
        ))
    }

    /**
     * For the next 3 tests written below, we need to open a popup and click on one
     * or two of the options in the popup. (Either simulate a dial or sms intent)
     * PopupMenu doesn't have an API to check whether it is showing.
     * So we use a custom matcher to check the visibility of the drop down list view instead.
     * But when running the entire test suite, these test cases were failing because when moving from
     * [DashboardFragment] -> [OrderListFragment] -> [OrderDetailFragment], a snackbar is displayed
     * (Error fetching data - because the Dashboard stats are not mocked). This Snackbar removes
     * the focus from the Popup menu and Espresso was unable to find the Popup menu and open it
     * and remained in a waiting state. To solve this, created a [MockedDashboardModule]
     * which mocks the [DashboardPresenter] and prevent the snackbar from being displayed
     */
    @Test
    fun verifyDisplayPopupForCorrectPhoneNumber() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingPhone = "9962789422"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(click())

        // click on the call or message button
        onView(withId(R.id.customerInfo_callOrMessageBtn)).perform(WCMatchers.scrollTo(), click())

        // verify that the popup menu is displayed & the first item in the list is call
        onView(withText(appContext.getString(R.string.orderdetail_call_customer)))
                .inRoot(isPlatformPopup()).check(matches(isDisplayed()))

        // verify that the popup menu is displayed & the second item in the list is message
        onView(withText(appContext.getString(R.string.orderdetail_message_customer)))
                .inRoot(isPlatformPopup()).check(matches(isDisplayed()))

        // verify that there are only 2 items in the popup list
        onView(isAssignableFrom(ListView::class.java))
                .check(matches(WCMatchers.withItemCount(2)))
    }

    @Test
    fun verifyDisplayPopupAndCallForCorrectPhoneNumber() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingPhone = "9962789422"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(click())

        // click on the call or message button
        onView(withId(R.id.customerInfo_callOrMessageBtn)).perform(WCMatchers.scrollTo(), click())

        // click on the item in the popup menu with text "Call"
        onView(withText(appContext.getString(R.string.orderdetail_call_customer)))
                .inRoot(isPlatformPopup()).perform(click())

        // check if phone intent is opened for the given phone number
        intended(allOf(hasAction(Intent.ACTION_DIAL), hasData(
                Uri.parse("tel:${mockWCOrderModel.billingPhone}")
        )))
    }

    @Test
    fun verifyDisplayPopupAndMessageForCorrectPhoneNumber() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                billingPhone = "9962789422"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(click())

        // click on the call or message button. Since the view might not be visible, scrollTo()
        // is used to this ensures that the view is displayed before proceeding to the click() action
        onView(withId(R.id.customerInfo_callOrMessageBtn)).perform(WCMatchers.scrollTo(), click())

        // click on the item in the popup menu with text "Message"
        onView(withText(appContext.getString(R.string.orderdetail_message_customer)))
                .inRoot(isPlatformPopup()).perform(click())

        // check if sms intent is opened for the given phone number
        intended(allOf(hasAction(Intent.ACTION_SENDTO), hasData(
                Uri.parse("smsto:${mockWCOrderModel.billingPhone}")
        )))
    }

    @Test
    fun verifyEmailCustomerWithCorrectEmail() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(click())

        // click on the Email icon
        onView(withId(R.id.customerInfo_emailBtn)).perform(WCMatchers.scrollTo(), click())

        // check if email intent is opened for the given email address
        intended(allOf(hasAction(Intent.ACTION_SENDTO), hasData(
                Uri.parse("mailto:${mockWCOrderModel.billingEmail}")
        )))
    }
}
