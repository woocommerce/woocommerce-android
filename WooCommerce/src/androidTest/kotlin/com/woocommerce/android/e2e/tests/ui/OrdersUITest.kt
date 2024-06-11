@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.helpers.util.MocksReader
import com.woocommerce.android.e2e.helpers.util.OrderData
import com.woocommerce.android.e2e.helpers.util.iterator
import com.woocommerce.android.e2e.rules.Retry
import com.woocommerce.android.e2e.rules.RetryTestRule
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.orders.OrderListScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
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
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @get:Rule(order = 4)
    var retryTestRule = RetryTestRule()

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

    @Retry(numberOfTimes = 1)
    @Test
    @Ignore
    fun e2eCreateOrderTest() {
        val note = "Just a placeholder text"
        val ordersJSONArray = MocksReader().readOrderToArray()

        for (orderJSON in ordersJSONArray.iterator()) {
            val orderData = mapJSONToOrder(orderJSON)

            // Note: we're not entering customer name due to
            // https://github.com/woocommerce/woocommerce-android/issues/8724
            OrderListScreen()
                .createFABTap()
                .addCustomerNote(note)
                .addProductTap()
                .assertProductsSelectorScreen(composeTestRule)
                .selectProduct(composeTestRule, orderData.productName)
                .createOrder()
                .assertSingleOrderScreenWithProduct(orderData)
                .goBackToOrdersScreen()
        }
    }

    private fun mapJSONToOrder(orderJSON: JSONObject): OrderData {
        return OrderData(
            customerName = orderJSON.getJSONObject("billing").getString("first_name") + " " +
                orderJSON.getJSONObject("billing").getString("last_name"),
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
