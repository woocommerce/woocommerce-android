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
import com.google.gson.Gson
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers.withRecyclerView
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import junit.framework.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderDetailProductCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    @Before
    override fun setup() {
        super.setup()
        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orders)).perform(click())
    }

    @Test
    fun verifyProductCardViewPopulatedSuccessfullyForSingleProduct() {
        val lineItems = Gson().toJson(
                listOf(
                        mapOf(
                                "productId" to "290",
                                "variationId" to "0",
                                "name" to "Black T-shirt",
                                "quantity" to 1,
                                "subtotal" to 10
                        )
                )
        )
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(products = lineItems)
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if order product card label matches this title:
        // R.string.orderdetail_product
        onView(withId(R.id.productList_lblProduct)).check(matches(
                withText(appContext.getString(R.string.orderdetail_product))
        ))

        // check if order product card quantity label matches this title:
        // R.string.orderdetail_product_qty
        onView(withId(R.id.productList_lblQty)).check(matches(
                withText(appContext.getString(R.string.orderdetail_product_qty))
        ))

        // check if product list is 1
        val recyclerView = activityTestRule.activity.findViewById(R.id.productList_products) as RecyclerView
        assertSame(1, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyProductCardViewPopulatedSuccessfullyForMultipleProducts() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // check if product list is 2
        val recyclerView = activityTestRule.activity.findViewById(R.id.productList_products) as RecyclerView
        assertSame(2, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyProductCardDetailsButtonVisibleForOrderStatusNotMarkedAsProcessing() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                orderStatus = "Completed"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify if the details button is visible
        onView(withId(R.id.productList_btnDetails)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // verify if the fulfill button is not visible
        onView(withId(R.id.productList_btnFulfill)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyProductCardFulfillButtonVisibleForOrderStatusMarkedAsProcessing() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(
                orderStatus = "Processing"
        )
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify if the details button is not visible
        onView(withId(R.id.productList_btnFulfill)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))

        // verify if the fulfill button is visible
        onView(withId(R.id.productList_btnDetails)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))
    }

    @Test
    fun verifyProductCardViewDetailsPopulatedSuccessfully() {
        val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail()
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify if the details button is not visible
        onView(withId(R.id.productList_btnFulfill)).check(matches(ViewMatchers.withEffectiveVisibility(GONE)))

        // verify if the fulfill button is visible
        onView(withId(R.id.productList_btnDetails)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // verify if the first product name matches: Black T-shirt
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_name))
                .check(matches(withText(mockWCOrderModel.getLineItemList()[0].name)))

        // verify if the second product quantity matches: 2
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_qty))
                .check(matches(withText(mockWCOrderModel.getLineItemList()[1].quantity?.toInt().toString())))
    }
}
