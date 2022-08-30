@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.main

import androidx.compose.ui.test.junit4.createComposeRule
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
    val composeTestRule = createComposeRule()

    @get:Rule(order = 2)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setUp() {
        WelcomeScreen
            .logoutIfNeeded(composeTestRule)
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent().gotoOrdersScreen()
    }

    @Test
    fun createOrderTest() {
        val ordersJSONArray = MocksReader().readOrderToArray()

        for (orderJSON in ordersJSONArray.iterator()) {
            val orderData = mapJSONToOrder(orderJSON)

            OrderListScreen()
                .createFABTap()
                .newOrderTap()
                .assertNewOrderScreen()
                .addProductTap()
                .assertOrderSelectProductScreen()
                .selectProduct(orderData.productName)
                .assertNewOrderScreenWithProduct(orderData.productName)
                .createOrder()
                .assertSingleOrderScreenWithProduct(orderData)
        }
    }

    private fun mapJSONToOrder(orderJSON: JSONObject): OrderData {
        return OrderData(
            id = orderJSON.getInt("id"),
            productName = orderJSON.getJSONArray("line_items").getJSONObject(0).getString("name"),
            statusRaw = orderJSON.getString("status"),
            totalRaw = orderJSON.getString("total")
        )
    }
}
