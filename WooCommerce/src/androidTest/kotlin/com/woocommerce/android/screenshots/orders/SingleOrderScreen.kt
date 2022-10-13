package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.NestedScrollViewExtension
import com.woocommerce.android.screenshots.util.OrderData
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf.allOf

class SingleOrderScreen : Screen {
    companion object {
        const val AMOUNT_FEE = R.id.paymentInfo_Fees
        const val AMOUNT_SHIPPING = R.id.paymentInfo_shippingTotal
        const val AMOUNT_TOTAL = R.id.paymentInfo_total
        const val COLLECT_PAYMENT_BUTTON = R.id.paymentInfo_collectCardPresentPaymentButton
        const val CUSTOMER_NOTE = R.id.customerInfo_customerNote
        const val CUSTOMER_NOTE_TEXT_FIELD = R.id.notEmptyLabel
        const val ORDER_NUMBER_LABEL = R.id.orderStatus_subtitle
        const val ORDER_STATUS_CUSTOMER = R.id.orderStatus_header
        const val ORDER_STATUS_TAG = R.id.orderStatus_orderTags
        const val TOOLBAR = R.id.toolbar
    }

    constructor() : super(ORDER_NUMBER_LABEL)

    fun goBackToOrdersScreen(): OrderListScreen {
        pressBack()
        return OrderListScreen()
    }

    private fun assertIdAndTextDisplayed(id: Int, text: String?) {
        Espresso.onView(
            Matchers.allOf(
                withId(id), withText(text)
            )
        ).check(ViewAssertions.matches(isDisplayed()))
    }

    fun assertSingleOrderScreenWithProduct(order: OrderData): SingleOrderScreen {
        Espresso.onView(withId(TOOLBAR))
            .check(ViewAssertions.matches(hasDescendant(withText("Order #${order.id}"))))
            .check(ViewAssertions.matches(isDisplayed()))

        Espresso.onView(withText(order.productName))
            .check(ViewAssertions.matches(isDisplayed()))

        assertIdAndTextDisplayed(ORDER_STATUS_TAG, order.status)
        assertIdAndTextDisplayed(ORDER_STATUS_CUSTOMER, order.customerName)

        Espresso.onView(withId(AMOUNT_TOTAL))
            .perform(NestedScrollViewExtension())
        assertIdAndTextDisplayed(AMOUNT_SHIPPING, order.shippingAmount)
        assertIdAndTextDisplayed(AMOUNT_FEE, order.feeAmount)
        assertIdAndTextDisplayed(AMOUNT_TOTAL, order.total)

        Espresso.onView(withId(CUSTOMER_NOTE))
            .perform(NestedScrollViewExtension())
        assertIdAndTextDisplayed(CUSTOMER_NOTE_TEXT_FIELD, order.customerNote)

        return this
    }

    fun tapOnCollectPayment(): PaymentSelectionScreen {
        clickOn(COLLECT_PAYMENT_BUTTON)
        return PaymentSelectionScreen()
    }
}
