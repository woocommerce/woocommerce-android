package com.woocommerce.android.ui.products

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
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.helpers.WCMatchers.scrollTo
import com.woocommerce.android.helpers.WCMatchers.withRecyclerView
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.ui.orders.WcOrderTestUtils
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.Assert.assertSame
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProductListCardTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "Completed")

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

        // inject mock data to order detail page
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // redirect to order detail page
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
    }

    @Test
    fun verifyProductCardListPopulatedSuccessfully() {
        // inject mock data to order product list page
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(scrollTo(), click())

        // Ensure that the toolbar title displays "Order #" + order number
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                equalToIgnoringCase(appContext.getString(string.orderdetail_orderstatus_ordernum,
                        mockWCOrderModel.number)
                ))))

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

        // check if product list is 2
        val recyclerView = activityTestRule.activity.findViewById(R.id.productList_products) as RecyclerView
        assertSame(2, recyclerView.adapter?.itemCount)

        // verify that the fulfill order and Details button are hidden
        onView(withId(R.id.productList_btnFulfill)).check(matches(withEffectiveVisibility(GONE)))
        onView(withId(R.id.productList_btnDetails)).check(matches(withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyProductCardListItemPopulatedSuccessfully() {
        // inject mock data to order product list page
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(scrollTo(), click())

        // verify if the first product name matches: Black T-shirt
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_name))
                .check(matches(withText(mockWCOrderModel.getLineItemList()[0].name)))

        // verify if the second product quantity matches: 2
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_qty))
                .check(matches(withText(mockWCOrderModel.getLineItemList()[1].quantity?.toInt().toString())))

        // verify that the product total label is visible
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_productTotal))
                .check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the product total tax is visible
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_totalTax))
                .check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the product total label is visible
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_lblTax))
                .check(matches(withEffectiveVisibility(VISIBLE)))

        // verify that the tax label matches : Tax:
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_lblTax))
                .check(matches(withText(appContext.getString(R.string.orderdetail_product_lineitem_tax))))
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

        // inject mock data to order product list page
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(scrollTo(), click())

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_total_single, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_single,
                        "$${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: $13.00 ($11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_multiple,
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
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(scrollTo(), click())

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_total_single, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_single,
                        "€${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: €13.00 (€11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_multiple,
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
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(scrollTo(), click())

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_total_single, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_single,
                        "₹${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: ₹13.00 (₹11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_multiple,
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
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(scrollTo(), click())

        // verify that the product total is displayed correctly for multiple quantities
        // for single quantity: getString( R.string.orderdetail_product_lineitem_total_single, orderTotal)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_single,
                        "A$${mockWCOrderModel.getLineItemList()[0].total}.00"
                ))))

        // for multiple quantities: A$13.00 (A$11.00x2)
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_productTotal))
                .check(matches(withText(appContext.getString(
                        R.string.orderdetail_product_lineitem_total_multiple,
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
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(scrollTo(), click())

        // sku is available for the first item. Verify it is displayed correctly
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_lblSku))
                .check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_sku))
                .check(matches(withEffectiveVisibility(VISIBLE)))
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_lblSku))
                .check(matches(withText(appContext.getString(R.string.orderdetail_product_lineitem_sku))))
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(0, R.id.productInfo_sku))
                .check(matches(withText(mockWCOrderModel.getLineItemList()[0].sku)))

        // sku is not available for the second item. Verify it is hidden
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_lblSku))
                .check(matches(withEffectiveVisibility(GONE)))
        onView(withRecyclerView(R.id.productList_products).atPositionOnView(1, R.id.productInfo_sku))
                .check(matches(withEffectiveVisibility(GONE)))
    }
}
