package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers

class SingleOrderScreen : Screen {
    companion object {
        const val ORDER_NUMBER_LABEL = R.id.orderStatus_subtitle
    }

    constructor() : super(ORDER_NUMBER_LABEL)

    fun goBackToOrdersScreen(): OrderListScreen {
        pressBack()
        return OrderListScreen()
    }

    fun assertSingleOrderScreen(): SingleOrderScreen {
        Espresso.onView(withId(R.id.toolbar))
            .check(ViewAssertions.matches(hasDescendant(withSubstring("#"))))
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withId(ORDER_NUMBER_LABEL))
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withId(R.id.paymentInfo_total))
            .check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(withId(R.id.orderDetail_customerInfo))
            .check(ViewAssertions.matches(isDisplayed()))
        return this
    }

    fun assertSingleOrderScreenWithEmptyOrder(): SingleOrderScreen {
        Espresso.onView(
            Matchers.allOf(
                withId(R.id.orderStatus_header),
                withText(R.string.orderdetail_customer_name_default)
            )
        ).check(ViewAssertions.matches(isDisplayed()))
        Espresso.onView(
            Matchers.allOf(
                withId(R.id.paymentInfo_total),
                withText("$0.00")
            )
        ).check(ViewAssertions.matches(isDisplayed()))
        return assertSingleOrderScreen()
    }

    fun assertSingleOrderScreenWithProduct(productName: String): SingleOrderScreen {
        Espresso.onView(withText(productName)).check(ViewAssertions.matches(isDisplayed()))
        return assertSingleOrderScreen()
    }
}
