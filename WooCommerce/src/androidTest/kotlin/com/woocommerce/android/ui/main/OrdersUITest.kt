@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.main

import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.helpers.InitializationRule
import com.woocommerce.android.helpers.TestBase
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.orders.OrderListScreen
import com.woocommerce.android.screenshots.util.MocksReader
import com.woocommerce.android.screenshots.util.OrderData
import com.woocommerce.android.screenshots.util.iterator
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OrdersUITest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val initRule = InitializationRule()

    @get:Rule(order = 2)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun setUp() {
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent().gotoOrdersScreen()
    }

    @Test
    fun createOrderTest() {
        val firstName = "Mira"
        val note = "Customer notes 123~"
        val status = "Processing"
        val ordersJSONArray = MocksReader().readOrderToArray()

        for (orderJSON in ordersJSONArray.iterator()) {
            val orderData = mapJSONToOrder(orderJSON)

            OrderListScreen()
                .createFABTap()
                .newOrderTap()
                .assertNewOrderScreen()
                .updateOrderStatus(status)
                .addProductTap()
                .assertOrderSelectProductScreen()
                .selectProduct(orderData.productName)
                .clickAddCustomerDetails()
                .addCustomerDetails(firstName)
                .addCustomerNotes(note)
                .addShipping()
                .addFee()
                .createOrder()
                .assertSingleOrderScreenWithProduct(orderData)
                .goBackToOrdersScreen()
        }
    }

    private fun mapJSONToOrder(orderJSON: JSONObject): OrderData {
        return OrderData(
            customer = orderJSON.getJSONObject("billing").getString("first_name"),
            customerNoteRaw = orderJSON.getString("customer_note"),
            feeRaw = orderJSON.getJSONArray("fee_lines").getJSONObject(0).getString("total"),
            id = orderJSON.getInt("id"),
            productName = orderJSON.getJSONArray("line_items").getJSONObject(0).getString("name"),
            shippingRaw = orderJSON.getString("shipping_total"),
            statusRaw = orderJSON.getString("status"),
            totalRaw = orderJSON.getString("total")
        )
    }
}
