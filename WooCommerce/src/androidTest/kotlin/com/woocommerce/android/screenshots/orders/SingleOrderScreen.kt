package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.OrderData
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

    private fun assertIdAndTextDisplayed(id: Int, text: String?) {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(id), ViewMatchers.withText(text)
            )
        ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    fun assertSingleOrderScreenWithProduct(order: OrderData): SingleOrderScreen {
        Espresso.onView(withId(R.id.toolbar))
            .check(ViewAssertions.matches(hasDescendant(withSubstring("#" + order.id))))
            .check(ViewAssertions.matches(isDisplayed()))

        Espresso.onView(withText(order.productName)).check(ViewAssertions.matches(isDisplayed()))
        this.assertIdAndTextDisplayed(R.id.orderStatus_orderTags, order.status)
        this.assertIdAndTextDisplayed(R.id.paymentInfo_total, order.total)

        return this
    }

    fun tapOnCollectPayment(): PaymentSelectionScreen {
        clickOn(R.id.paymentInfo_collectCardPresentPaymentButton)
        return PaymentSelectionScreen()
    }
}
