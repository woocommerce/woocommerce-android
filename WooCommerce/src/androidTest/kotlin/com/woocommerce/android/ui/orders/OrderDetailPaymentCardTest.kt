package com.woocommerce.android.ui.orders

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.Visibility.GONE
import android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderDetailPaymentCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

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
    fun verifyPaymentCardViewPopulatedSuccessfully() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                orderStatus = "PROCESSING",
                paymentMethodTitle = "Credit Card (Stripe)",
                discountTotal = "4"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order payment card label matches this title: R.string.payment
        onView(withId(R.id.paymentInfo_lblTitle)).check(matches(withText(appContext.getString(R.string.payment))))

        // check if order payment card sub total label matches this title: R.string.subtotal
        onView(withId(R.id.paymentInfo_lblSubtotal)).check(matches(withText(appContext.getString(R.string.subtotal))))

        // check if order payment card shipping label matches this title: R.string.shipping
        onView(withId(R.id.paymentInfo_lblShipping)).check(matches(withText(appContext.getString(R.string.shipping))))

        // check if order payment card taxes label matches this title: R.string.taxes
        onView(withId(R.id.paymentInfo_lblTaxes)).check(matches(withText(appContext.getString(R.string.taxes))))

        // check if order payment card total label matches this title: R.string.total
        onView(withId(R.id.paymentInfo_lblTotal)).check(matches(withText(appContext.getString(R.string.total))))

        // Since discount is available, check if order payment card discount label matches this title: R.string.discount
        onView(withId(R.id.paymentInfo_lblDiscount)).check(matches(withText(appContext.getString(R.string.discount))))

        // since payment is set to Processing, and order.paymentMethodTitle is not empty,
        // the payment method message should be visible
        onView(withId(R.id.paymentInfo_paymentMsg)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))
        onView(withId(R.id.paymentInfo_paymentMsg)).check(matches(withText(appContext.getString(
                R.string.orderdetail_payment_summary_completed,
                "$44.00",
                mockWCOrderModel.paymentMethodTitle)
        )))
    }

    @Test
    fun verifyPaymentMethodTitleHiddenIfNotAvailable() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // since order.paymentMethodTitle is empty, the payment method message should not be visible
        onView(withId(R.id.paymentInfo_paymentMsg)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyPaymentCardViewPopulatedSuccessfullyForEuro() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                currency = "euro", paymentMethodTitle = "Credit Card (Stripe)"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order payment card sub total matches this text
        onView(withId(R.id.paymentInfo_subTotal)).check(matches(withText("euro22.00")))

        // check if order payment card shipping total matches this text
        onView(withId(R.id.paymentInfo_shippingTotal)).check(matches(withText("euro12.00")))

        // check if order payment card tax total matches this text
        onView(withId(R.id.paymentInfo_taxesTotal)).check(matches(withText("euro2.00")))

        // check if order payment card total matches this text
        onView(withId(R.id.paymentInfo_total)).check(matches(withText("euro44.00")))

        // check if order payment message messages this text
        onView(withId(R.id.paymentInfo_paymentMsg)).check(matches(withText(appContext.getString(
                R.string.orderdetail_payment_summary_completed,
                "euro44.00",
                mockWCOrderModel.paymentMethodTitle)
        )))
    }

    @Test
    fun verifyPaymentCardViewPopulatedSuccessfullyForUsd() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                paymentMethodTitle = "Credit Card (Stripe)"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order payment card sub total matches this text
        onView(withId(R.id.paymentInfo_subTotal)).check(matches(withText("$22.00")))

        // check if order payment card shipping total matches this text
        onView(withId(R.id.paymentInfo_shippingTotal)).check(matches(withText("$12.00")))

        // check if order payment card tax total matches this text
        onView(withId(R.id.paymentInfo_taxesTotal)).check(matches(withText("$2.00")))

        // check if order payment card total matches this text
        onView(withId(R.id.paymentInfo_total)).check(matches(withText("$44.00")))

        onView(withId(R.id.paymentInfo_paymentMsg)).check(matches(withText(appContext.getString(
                R.string.orderdetail_payment_summary_completed,
                "$44.00",
                mockWCOrderModel.paymentMethodTitle)
        )))
    }

    @Test
    fun verifyPaymentCardViewPopulatedSuccessfullyForInr() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                currency = "inr", paymentMethodTitle = "Credit Card (Stripe)"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order payment card sub total matches this text
        onView(withId(R.id.paymentInfo_subTotal)).check(matches(withText("₹22.00")))

        // check if order payment card shipping total matches this text
        onView(withId(R.id.paymentInfo_shippingTotal)).check(matches(withText("₹12.00")))

        // check if order payment card tax total matches this text
        onView(withId(R.id.paymentInfo_taxesTotal)).check(matches(withText("₹2.00")))

        // check if order payment card total matches this text
        onView(withId(R.id.paymentInfo_total)).check(matches(withText("₹44.00")))

        onView(withId(R.id.paymentInfo_paymentMsg)).check(matches(withText(appContext.getString(
                R.string.orderdetail_payment_summary_completed,
                "₹44.00",
                mockWCOrderModel.paymentMethodTitle)
        )))
    }

    @Test
    fun verifyPaymentCardViewNoDiscountHidden() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // since no discounts are applicable, the section should be hidden
        onView(withId(R.id.paymentInfo_discountSection)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyPaymentCardViewNoRefundHidden() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Since no refund is available, the section should be hidden:
        onView(withId(R.id.paymentInfo_refundSection)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))

        // check if order payment card label matches this title: R.string.payment
        onView(withId(R.id.paymentInfo_lblTitle)).check(matches(withText(appContext.getString(R.string.payment))))
    }

    @Test
    fun verifyPaymentCardViewWithRefundPopulatedSuccessfully() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                refundTotal = 4.25
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Since no refund is available, the section should be hidden:
        onView(withId(R.id.paymentInfo_refundSection)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // check if order payment card label matches this title: R.string.orderdetail_payment_refunded
        onView(withId(R.id.paymentInfo_lblTitle)).check(matches(withText(
                appContext.getString(R.string.orderdetail_payment_refunded)))
        )

        // verify that the refund total is displayed and the color is set to red
        onView(withId(R.id.paymentInfo_refundTotal)).check(matches(withText("$4.25")))
        onView(withId(R.id.paymentInfo_refundTotal)).check(matches(WCMatchers.withTextColor(R.color.wc_red)))

        // verify that the new payment total is displayed
        onView(withId(R.id.paymentInfo_newTotal)).check(matches(withText("$48.25")))
    }

    @Test
    fun verifyPaymentCardViewWithSingleDiscountPopulatedSuccessfully() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                discountTotal = "10", discountCodes = "30dayhoodiesale"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Since discount is available, check if it is displayed
        onView(withId(R.id.paymentInfo_discountSection)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // Since discount is available, check if the amount = 10
        onView(withId(R.id.paymentInfo_discountTotal)).check(matches(withText("$10.00")))

        // Since discount is available, check if the discount code = 30dayhoodiesale
        onView(withId(R.id.paymentInfo_discountItems)).check(matches(withText(
                appContext.getString(R.string.orderdetail_discount_items, mockWCOrderModel.discountCodes)
        )))
    }

    @Test
    fun verifyPaymentCardViewWithMultipleDiscountsPopulatedSuccessfully() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                discountTotal = "22.40", discountCodes = "30dayhoodiesale, 10dollaroff100"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // Since discount is available, check if it is displayed
        onView(withId(R.id.paymentInfo_discountSection)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // Since discount is available, check if the amount = 122.40
        onView(withId(R.id.paymentInfo_discountTotal)).check(matches(withText("$22.40")))

        // Since discount is available, check if the discount code = 30dayhoodiesale, 10dollaroff100
        onView(withId(R.id.paymentInfo_discountItems)).check(matches(withText(
                appContext.getString(R.string.orderdetail_discount_items, mockWCOrderModel.discountCodes)
        )))
    }
}
