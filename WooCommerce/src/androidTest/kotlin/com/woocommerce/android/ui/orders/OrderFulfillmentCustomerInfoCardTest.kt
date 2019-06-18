package com.woocommerce.android.ui.orders

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.R
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.util.AddressUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderFulfillmentCustomerInfoCardTest : TestBase() {
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
        onView(withId(R.id.orders)).perform(click())
    }

    @Test
    fun verifyCustomerInfoCardPopulatedSuccessfully() {
        // Set Order fulfillment with mock data
        mockWCOrderModel.billingFirstName = "Anitaa"
        mockWCOrderModel.billingLastName = "Murthy"
        mockWCOrderModel.billingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.billingCountry = "USA"
        activityTestRule.setOrderFulfillmentWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())

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

        // verify that the customer shipping details is displayed
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(isDisplayed()))

        // check that the billing info is hidden
        onView(withId(R.id.customerInfo_billingLabel)).check(matches(withEffectiveVisibility(GONE)))

        // check if customer info card shipping details matches this format:
        val billingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.billingFirstName, mockWCOrderModel.billingLastName
        )
        val billingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.billingCountry)

        // Assumes that the shipping name, address and country info is available
        val shippingAddrFull = "$billingName\n$billingAddr\n$billingCountry"
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(shippingAddrFull)))
    }

    @Test
    fun verifyCustomerInfoCardViewWithLongNamePopulatedSuccessfully() {
        // Set Order fulfillment with mock data
        mockWCOrderModel.billingFirstName = "Itsareallylongfirstname"
        mockWCOrderModel.billingLastName = "Itsareallylonglastname"
        mockWCOrderModel.billingAddress1 = "Its a really long address to see how it is handled in UI. More to comehere"
        mockWCOrderModel.billingCountry = "India"
        activityTestRule.setOrderFulfillmentWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())

        // check if customer info card shipping details matches this format:
        val billingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.billingFirstName, mockWCOrderModel.billingLastName
        )
        val billingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.billingCountry)

        // Assumes that the shipping name, address and country info is available
        val shippingAddrFull = "$billingName\n$billingAddr\n$billingCountry"
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(shippingAddrFull)))
    }

    @Test
    fun verifyCustomerInfoCardWithNoBillingAddressPopulatedSuccessfully() {
        // Set Order fulfillment with mock data
        activityTestRule.setOrderFulfillmentWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())

        // check if customer info card shipping details matches this format:
        val shippingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.billingFirstName, mockWCOrderModel.billingLastName
        )
        val shippingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getBillingAddress())
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.billingCountry)

        // Assumes that the shipping name, address and country info is available
        var shippingAddrFull = if (!shippingName.trim().isBlank()) "$shippingName\n" else ""
        if (!shippingAddr.isBlank()) shippingAddrFull += "$shippingAddr\n"
        if (!shippingCountry.isBlank()) shippingAddrFull += shippingCountry
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(shippingAddrFull)))
    }

    @Test
    fun verifyCustomerInfoCardWithSeparateShippingAddressPopulatedSuccessfully() {
        // Set Order fulfillment with mock data
        mockWCOrderModel.shippingFirstName = "Anitaa"
        mockWCOrderModel.shippingLastName = "Murthy"
        mockWCOrderModel.shippingAddress1 = "Ramada Plaza, 450 Capitol Ave SE Atlanta"
        mockWCOrderModel.shippingCountry = "USA"
        activityTestRule.setOrderFulfillmentWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())

        // check if customer info card shipping details matches this format:
        val shippingName = appContext.getString(
                R.string.customer_full_name,
                mockWCOrderModel.shippingFirstName, mockWCOrderModel.shippingLastName
        )
        val shippingAddr = AddressUtils.getEnvelopeAddress(mockWCOrderModel.getShippingAddress())
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(mockWCOrderModel.shippingCountry)

        // Assumes that the shipping name, address and country info is available
        var shippingAddrFull = if (!shippingName.trim().isBlank()) "$shippingName\n" else ""
        if (!shippingAddr.isBlank()) shippingAddrFull += "$shippingAddr\n"
        if (!shippingCountry.isBlank()) shippingAddrFull += shippingCountry
        onView(withId(R.id.customerInfo_shippingAddr)).check(matches(withText(shippingAddrFull)))
    }

    @Test
    fun verifyCustomerInfoCardHiddenWhenProductVirtual() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel, isVirtualProduct = true)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the customer info card is hidden
        onView(withId(R.id.orderDetail_customerInfo)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyCustomerInfoCardDisplayedProductNotVirtual() {
        // add mock data to order detail screen
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel, isVirtualProduct = false)

        // click on the first order in the list and check if redirected to order detail
        onView(ViewMatchers.withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify that the customer info card is hidden
        onView(withId(R.id.orderDetail_customerInfo)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))
    }
}
