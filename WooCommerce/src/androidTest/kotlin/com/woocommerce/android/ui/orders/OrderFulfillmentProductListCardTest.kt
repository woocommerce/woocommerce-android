package com.woocommerce.android.ui.orders

import android.os.Build.VERSION
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.gson.Gson
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers.withRecyclerView
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import org.junit.Assert.assertSame
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderFulfillmentProductListCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing")

    private fun redirectToOrderFulfillment() {
        // Set Order fulfillment with mock data
        activityTestRule.setOrderFulfillmentWithMockData(mockWCOrderModel)

        // click on the first order in the list and check if redirected to order detail
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // click on Order Fulfill button to redirect to Order Fulfillment
        onView(withId(R.id.productList_btnFulfill)).perform(click())
    }

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
    fun verifyProductCardListPopulatedSuccessfullyForSingleProduct() {
        // Set Order fulfillment with mock data
        val lineItems = Gson().toJson(
                listOf(mapOf(
                                "productId" to "290",
                                "variationId" to "0",
                                "name" to "Black T-shirt",
                                "quantity" to 1,
                                "subtotal" to 10
                        )))
        mockWCOrderModel.lineItems = lineItems
        redirectToOrderFulfillment()

        // check if product list is 1
        val recyclerView = activityTestRule.activity.findViewById(R.id.productList_products) as RecyclerView
        assertSame(1, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyProductCardListPopulatedSuccessfullyForMultipleProducts() {
        redirectToOrderFulfillment()

        // check if product list is 2
        val recyclerView = activityTestRule.activity.findViewById(R.id.productList_products) as RecyclerView
        assertSame(2, recyclerView.adapter?.itemCount)
    }

    @Test
    fun verifyProductCardListItemsPopulatedSuccessfully() {
        redirectToOrderFulfillment()

        // verify that the fulfill order and Details button are hidden
        onView(withId(R.id.productList_btnFulfill)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.productList_btnDetails)).check(matches(withEffectiveVisibility(GONE)))

        // check if order product card label matches this title:
        // R.string.orderdetail_product
        onView(withId(R.id.productList_lblProduct)).check(matches(
                withText(appContext.getString(R.string.orderdetail_product))
        ))

        // verify that the product total label is visible
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalPaid))
                .check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the product total tax label is visible
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_lblTax))
                .check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the tax label matches : Tax:
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_lblTax))
                .check(matches(withText(appContext.getString(R.string.orderdetail_product_lineitem_tax))))

        // verify if the first product name matches: Black T-shirt
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_name))
                .check(matches(withText(mockWCOrderModel.getLineItemList()[0].name)))

        // verify if the second product quantity matches: 2
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalPaid))
                .check(matches(withText(mockWCOrderModel.getLineItemList()[1].quantity?.toInt().toString())))

        // verify that the product total tax is visible
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalTax))
                .check(matches(withEffectiveVisibility(VISIBLE)))
    }

    /**
     * Related FluxC issue in MockedStack_WCBaseStoreTest.kt#L116
     * Some of the internals of java.util.Currency seem to be stubbed in a unit test environment,
     * giving results inconsistent with a normal running app
     */
    @Test
    fun verifyProductListItemCurrencyDisplayedCorrectlyForUSD() {
        Assume.assumeTrue(
                "Requires API 23 or higher due to localized currency values differing on older versions",
                VERSION.SDK_INT >= 23
        )

        redirectToOrderFulfillment()

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_qty_and_price, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "$${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: $13.00 ($11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "$${mockWCOrderModel.getLineItemList()[1].total}.00",
                        "$${mockWCOrderModel.getLineItemList()[1].price}.00",
                        mockWCOrderModel.getLineItemList()[1].quantity?.toInt().toString()
                ))))

        // verify that the total tax is displayed correctly
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalTax))
                .check(matches(withText("$${mockWCOrderModel.getLineItemList()[0].totalTax}.00")))
    }

    /**
     * Related FluxC issue in MockedStack_WCBaseStoreTest.kt#L116
     * Some of the internals of java.util.Currency seem to be stubbed in a unit test environment,
     * giving results inconsistent with a normal running app
     */
    @Test
    fun verifyProductListItemCurrencyDisplayedCorrectlyForEuro() {
        Assume.assumeTrue(
                "Requires API 23 or higher due to localized currency values differing on older versions",
                VERSION.SDK_INT >= 23
        )

        // inject mock data to order product list page
        mockWCOrderModel.currency = "EUR"
        redirectToOrderFulfillment()

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_qty_and_price, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "€${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: €13.00 (€11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "€${mockWCOrderModel.getLineItemList()[1].total}.00",
                        "€${mockWCOrderModel.getLineItemList()[1].price}.00",
                        mockWCOrderModel.getLineItemList()[1].quantity?.toInt().toString()
                ))))

        // verify that the total tax is displayed correctly
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalTax))
                .check(matches(withText("€${mockWCOrderModel.getLineItemList()[0].totalTax}.00")))
    }

    /**
     * Related FluxC issue in MockedStack_WCBaseStoreTest.kt#L116
     * Some of the internals of java.util.Currency seem to be stubbed in a unit test environment,
     * giving results inconsistent with a normal running app
     */
    @Test
    fun verifyProductListItemCurrencyDisplayedCorrectlyForInr() {
        Assume.assumeTrue(
                "Requires API 23 or higher due to localized currency values differing on older versions",
                VERSION.SDK_INT >= 23
        )

        // inject mock data to order product list page
        mockWCOrderModel.currency = "INR"
        redirectToOrderFulfillment()

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_qty_and_price, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "₹${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: ₹13.00 (₹11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "₹${mockWCOrderModel.getLineItemList()[1].total}.00",
                        "₹${mockWCOrderModel.getLineItemList()[1].price}.00",
                        mockWCOrderModel.getLineItemList()[1].quantity?.toInt().toString()
                ))))

        // verify that the total tax is displayed correctly
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalTax))
                .check(matches(withText("₹${mockWCOrderModel.getLineItemList()[0].totalTax}.00")))
    }

    /**
     * Related FluxC issue in MockedStack_WCBaseStoreTest.kt#L116
     * Some of the internals of java.util.Currency seem to be stubbed in a unit test environment,
     * giving results inconsistent with a normal running app
     */
    @Test
    fun verifyProductListItemCurrencyDisplayedCorrectlyForAud() {
        Assume.assumeTrue(
                "Requires API 23 or higher due to localized currency values differing on older versions",
                VERSION.SDK_INT >= 23
        )

        // inject mock data to order product list page
        mockWCOrderModel.currency = "AUD"
        redirectToOrderFulfillment()

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_qty_and_price, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "A$${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: A$13.00 (A$11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalPaid))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_qty_and_price,
                        "A\$${mockWCOrderModel.getLineItemList()[1].total}.00",
                        "A\$${mockWCOrderModel.getLineItemList()[1].price}.00",
                        mockWCOrderModel.getLineItemList()[1].quantity?.toInt().toString()
                ))))

        // verify that the total tax is displayed correctly
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_totalTax))
                .check(matches(withText("A$${mockWCOrderModel.getLineItemList()[0].totalTax}.00")))
    }

    @Test
    fun verifyProductListItemSkuIsHiddenIfNotAvailable() {
        // inject mock data to order product list page
        redirectToOrderFulfillment()

        // sku is available for the first item. Verify it is displayed correctly
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_sku))
                .check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_sku))
                .check(matches(withText(String.format(
                        appContext.getString(R.string.orderdetail_product_lineitem_sku_value),
                        mockWCOrderModel.getLineItemList()[0].sku
                    ))))

        // sku is not available for the second item. Verify it is hidden
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_sku))
                .check(matches(withEffectiveVisibility(GONE)))
    }
}
