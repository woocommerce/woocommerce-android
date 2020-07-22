package com.woocommerce.android.ui.orders

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.ui.orders.list.OrderListFragment
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderDetailCustomerInfoCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()

    private fun verifyShippingInfoDisplayedCorrectly() {
        val shippingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.shippingFirstName, mockWCOrderModel.shippingLastName
        )
        val shippingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getShippingAddress())
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.shippingCountry)
        val shippingAddrFull = "$shippingName\n$shippingAddr\n$shippingCountry"
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(shippingAddrFull)))
    }

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
        onView(withId(R.id.orders)).perform(click())
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun verifyOrderDetailCardViewPopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.shippingFirstName = "Ann"
        mockWCOrderModel.shippingLastName = "Moore"
        mockWCOrderModel.shippingAddress1 = "Nigagra Falls"
        mockWCOrderModel.shippingCountry = "USA"
        mockWCOrderModel.billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.billingLastName = "Murthy"
        mockWCOrderModel.billingFirstName = "Anitaa"
        mockWCOrderModel.billingCountry = "USA"
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

        // verify that the customer shipping details is displayed and text is correct
        onView(withId(R.id.customerInfo_shippingAddr)).perform(WCMatchers.scrollTo())
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(isDisplayed()))
        verifyShippingInfoDisplayedCorrectly()

        // verify that the billing view is condensed and load more button is visible
        onView(withId(R.id.customerInfo_morePanel)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_viewMoreButtonTitle)).check(matches(
                withText(appContext.getString(R.string.orderdetail_show_billing))
        ))
    }

    @Test
    fun verifyOrderDetailCardViewShowBillingViewPopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.shippingFirstName = "Ann"
        mockWCOrderModel.shippingLastName = "Moore"
        mockWCOrderModel.shippingAddress1 = "Nigagra Falls"
        mockWCOrderModel.shippingCountry = "USA"
        mockWCOrderModel.billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.billingLastName = "Murthy"
        mockWCOrderModel.billingFirstName = "Anitaa"
        mockWCOrderModel.billingCountry = "USA"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo())

        // verify that the customer billing details is displayed
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(isDisplayed()))

        // verify that the billing view is expanded and load more button is visible
        onView(withId(R.id.customerInfo_morePanel)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_viewMoreButtonTitle)).check(matches(
                withText(appContext.getString(R.string.orderdetail_hide_billing))
        ))

        // click on Hide Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())

        // verify that the billing view is condensed and load more button is visible
        onView(withId(R.id.customerInfo_morePanel)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_viewMoreButtonTitle)).check(matches(
                withText(appContext.getString(R.string.orderdetail_show_billing))
        ))
    }

    @Test
    fun verifyOrderDetailCardViewWithOnlyShippingPopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.shippingFirstName = "Ann"
        mockWCOrderModel.shippingLastName = "Moore"
        mockWCOrderModel.shippingAddress1 = "Nigagra Falls"
        mockWCOrderModel.shippingCountry = "USA"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the billing section is hidden since billing details not available
        onView(withId(R.id.customerInfo_viewMore)).check(matches(withEffectiveVisibility(GONE)))

        verifyShippingInfoDisplayedCorrectly()
    }

    @Test
    fun verifyOrderDetailCardViewWithNoShippingBillingInfoPopulatedSuccessfully() {
        // add mock data to order detail screen
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the billing section is hidden since billing details not available
        onView(withId(R.id.customerInfo_viewMore)).check(matches(withEffectiveVisibility(GONE)))

        // no shipping available so displays empty text
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(
                appContext.getString(R.string.orderdetail_empty_shipping_address)
        )))
    }

    @Test
    fun verifyOrderDetailCardViewWithBillingAddressPopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.billingFirstName = "Jane"
        mockWCOrderModel.billingLastName = "Masterson"
        mockWCOrderModel.billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.billingCountry = "USA"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

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

        // verify that billing phone section is not displayed
        onView(withId(R.id.customerInfo_phone)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_divider3)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_callOrMessageBtn)).check(matches(withEffectiveVisibility(GONE)))

        // verify that billing email section is not displayed
        onView(withId(R.id.customerInfo_emailAddr)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_emailBtn)).check(matches(withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyOrderDetailCardViewWithOnlyBillingPopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.billingFirstName = "Jane"
        mockWCOrderModel.billingLastName = "Masterson"
        mockWCOrderModel.billingAddress1 = "29, Kuppam Beach Road"
        mockWCOrderModel.billingCountry = "India"
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
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(withText(billingAddrFull)))

        // no shipping available so display only empty address text
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(R.string.orderdetail_empty_shipping_address)))
        onView(withId(R.id.customerInfo_shippingMethodSection)).check(matches(withEffectiveVisibility(GONE)))
    }

    // This test is disabled, see https://github.com/woocommerce/woocommerce-android/issues/2636
    @Ignore
    @Test
    fun verifyOrderDetailCardViewWithBillingPhonePopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.billingPhone = "9962789522"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo())

        // since the customer info phone is NOT empty, the view should be displayed
        onView(withId(R.id.customerInfo_phone)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_divider3)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_callOrMessageBtn)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_phone)).check(matches(
                withText(PhoneUtils.formatPhone(mockWCOrderModel.billingPhone))
        ))

        // verify that billing email section is not displayed
        onView(withId(R.id.customerInfo_emailAddr)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_emailBtn)).check(matches(withEffectiveVisibility(GONE)))

        // verify that billing address is not displayed
        onView(withId(R.id.customerInfo_billingLabel)).check(matches(withText(R.string.orderdetail_billing_details)))
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_divider2)).check(matches(withEffectiveVisibility(GONE)))

        // no shipping available so display only empty address text
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(R.string.orderdetail_empty_shipping_address)))
        onView(withId(R.id.customerInfo_shippingMethodSection)).check(matches(withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyOrderDetailCardViewWithBillingEmailPopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.billingEmail = "test@testing.com"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo())

        // verify the billing phone section is not displayed
        onView(withId(R.id.customerInfo_phone)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_divider3)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_callOrMessageBtn)).check(matches(withEffectiveVisibility(GONE)))

        // verify that billing email section is displayed
        onView(withId(R.id.customerInfo_emailAddr)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_emailBtn)).check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that billing address is not displayed
        onView(withId(R.id.customerInfo_billingLabel)).check(matches(withText(R.string.orderdetail_billing_details)))
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_divider2)).check(matches(withEffectiveVisibility(GONE)))

        // no shipping available so display only empty address text
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(R.string.orderdetail_empty_shipping_address)))
        onView(withId(R.id.customerInfo_shippingMethodSection)).check(matches(withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyOrderDetailCardViewWithBillingPhoneForDifferentLocalePopulatedSuccessfully() {
        // add mock data to order detail screen
        mockWCOrderModel.billingFirstName = "Jane"
        mockWCOrderModel.billingLastName = "Masterson"
        mockWCOrderModel.billingPhone = "07911123456"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo())

        // since the customer info phone is NOT empty, the view should be displayed
        onView(withId(R.id.customerInfo_phone)).check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.customerInfo_divider2)).check(matches(withEffectiveVisibility(VISIBLE)))
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
        mockWCOrderModel.billingFirstName = "Jane"
        mockWCOrderModel.billingLastName = "Masterson"
        mockWCOrderModel.billingPhone = "07911123456"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo())

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
        mockWCOrderModel.billingFirstName = "Jane"
        mockWCOrderModel.billingLastName = "Masterson"
        mockWCOrderModel.billingPhone = "07911123456"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo())

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
        mockWCOrderModel.shippingFirstName = "Ann"
        mockWCOrderModel.shippingLastName = "Moore"
        mockWCOrderModel.shippingAddress1 = "Nigagra Falls"
        mockWCOrderModel.shippingCountry = "USA"
        mockWCOrderModel.billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.billingLastName = "Murthy"
        mockWCOrderModel.billingFirstName = "Anitaa"
        mockWCOrderModel.billingCountry = "USA"
        mockWCOrderModel.billingPhone = "07911123456"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())

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
        mockWCOrderModel.billingFirstName = "Jane"
        mockWCOrderModel.billingLastName = "Masterson"
        mockWCOrderModel.billingEmail = "test1@testing.com"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Show Billing button
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo(), click())
        onView(withId(R.id.customerInfo_viewMore)).perform(WCMatchers.scrollTo())

        // click on the Email icon
        onView(withId(R.id.customerInfo_emailBtn)).perform(WCMatchers.scrollTo(), click())

        // check if email intent is opened for the given email address
        intended(allOf(hasAction(Intent.ACTION_SENDTO), hasData(
                Uri.parse("mailto:${mockWCOrderModel.billingEmail}")
        )))
    }

    // This test is disabled, see https://github.com/woocommerce/woocommerce-android/issues/2635
    @Ignore
    @Test
    fun verifyCustomerInfoCardShippingHiddenWhenProductVirtual() {
        // add mock data to order detail screen
        mockWCOrderModel.shippingFirstName = "Ann"
        mockWCOrderModel.shippingLastName = "Moore"
        mockWCOrderModel.shippingAddress1 = "Nigagra Falls"
        mockWCOrderModel.shippingCountry = "USA"
        mockWCOrderModel.billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.billingLastName = "Murthy"
        mockWCOrderModel.billingFirstName = "Anitaa"
        mockWCOrderModel.billingCountry = "USA"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel, isVirtualProduct = true)

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
        onView(withId(R.id.customerInfo_billingAddr)).check(matches(withText(billingAddrFull)))

        // virtual product so shipping should be hidden, even if available
        onView(withId(R.id.customerInfo_divider)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_shippingLabel)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_viewMore)).check(matches(withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyCustomerInfoCardDisplayedProductNotVirtual() {
        // add mock data to order detail screen
        // add mock data to order detail screen
        mockWCOrderModel.shippingFirstName = "Ann"
        mockWCOrderModel.shippingLastName = "Moore"
        mockWCOrderModel.shippingAddress1 = "Nigagra Falls"
        mockWCOrderModel.shippingCountry = "USA"
        mockWCOrderModel.billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.billingLastName = "Murthy"
        mockWCOrderModel.billingFirstName = "Anitaa"
        mockWCOrderModel.billingCountry = "USA"
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel, isVirtualProduct = false)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the customer info card is not hidden
        // verify that the customer shipping details is displayed and text is correct
        onView(withId(R.id.customerInfo_shippingAddr)).perform(WCMatchers.scrollTo())
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(isDisplayed()))
        verifyShippingInfoDisplayedCorrectly()

        // verify that the billing view is condensed and load more button is visible
        onView(withId(R.id.customerInfo_morePanel)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
        onView(withId(R.id.customerInfo_viewMoreButtonTitle)).check(matches(
                withText(appContext.getString(R.string.orderdetail_show_billing))
        ))
    }

    @Test
    fun verifyOrderDetailNotesCardEmptyView() {
        // add mock data to order detail screen
        val wcOrderStatusModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(wcOrderStatusModel)

        // click on the first order in the list and check if redirected to order detail
        Espresso.onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // customerNote is empty so Hide Customer Note card
        onView(withId(R.id.customerInfo_customerNoteSection)).check(
                matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun verifyOrderDetailNotesCardViewIsDisplayed() {
        // add mock data to order detail screen
        val wcOrderStatusModel = WcOrderTestUtils.generateOrderDetail(note = "This is a test note")
        activityTestRule.setOrderDetailWithMockData(wcOrderStatusModel)

        // click on the first order in the list and check if redirected to order detail
        Espresso.onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // customerNote is not empty so Show Customer Note card
        onView(withId(R.id.customerInfo_customerNoteSection)).check(matches(
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }
}
