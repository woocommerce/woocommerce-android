package com.woocommerce.android.e2e.screens.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.NestedScrollViewExtension
import com.woocommerce.android.e2e.helpers.util.OrderData
import com.woocommerce.android.e2e.helpers.util.Screen
import org.hamcrest.Matchers

class SingleOrderScreen : Screen {
    companion object {
        const val AMOUNT_PRODUCTS_TOTAL = R.id.paymentInfo_productsTotal
        const val AMOUNT_FEE = R.id.paymentInfo_Fees
        const val AMOUNT_SHIPPING = R.id.paymentInfo_shippingTotal
        const val AMOUNT_TAXES = R.id.paymentInfo_taxesTotal
        const val AMOUNT_TOTAL = R.id.paymentInfo_total
        const val COLLECT_PAYMENT_BUTTON = R.id.paymentInfo_collectCardPresentPaymentButton
        const val CUSTOMER_NOTE = R.id.customerInfo_customerNote
        const val ORDER_STATUS_CUSTOMER = R.id.orderStatus_header
        const val ORDER_STATUS_TAG = R.id.orderStatus_orderTags
        const val TOOLBAR = R.id.toolbar
    }

    constructor() : super(TOOLBAR)

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

    private fun assertOrderStatus(orderStatus: String): SingleOrderScreen {
        assertIdAndTextDisplayed(ORDER_STATUS_TAG, orderStatus)
        return this
    }

    private fun assertCustomerName(customerName: String): SingleOrderScreen {
        assertIdAndTextDisplayed(ORDER_STATUS_CUSTOMER, customerName)
        return this
    }

    private fun assertOrderId(orderId: Int): SingleOrderScreen {
        Espresso.onView(withId(TOOLBAR))
            .check(ViewAssertions.matches(hasDescendant(withText("Order #$orderId"))))
            .check(ViewAssertions.matches(isDisplayed()))
        return this
    }

    private fun assertCustomerNote(customerNote: String): SingleOrderScreen {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(withId(CUSTOMER_NOTE)),
                Matchers.allOf(
                    withId(R.id.notEmptyLabel),
                    withText(customerNote)
                )
            )
        )
            .perform(NestedScrollViewExtension())
            .check(ViewAssertions.matches(isDisplayed()))

        return this
    }

    private fun assertPayments(order: OrderData): SingleOrderScreen {
        Espresso.onView(withId(AMOUNT_TOTAL))
            .perform(NestedScrollViewExtension())

        if (order.productsTotalRaw.isNotBlank()) {
            assertIdAndTextDisplayed(AMOUNT_PRODUCTS_TOTAL, order.productsTotalAmount)
        }

        if (order.shippingRaw.isNotBlank()) {
            assertIdAndTextDisplayed(AMOUNT_SHIPPING, order.shippingAmount)
        }

        if (order.feeRaw.isNotBlank()) {
            assertIdAndTextDisplayed(AMOUNT_FEE, order.feeAmount)
        }

        if (order.taxesRaw.isNotBlank()) {
            assertIdAndTextDisplayed(AMOUNT_TAXES, order.taxesAmount)
        }

        assertIdAndTextDisplayed(AMOUNT_TOTAL, order.total)
        return this
    }

    fun assertSingleOrderScreenWithProduct(order: OrderData): SingleOrderScreen {
        assertOrderId(order.id)

        Espresso.onView(withText(order.productName))
            .check(ViewAssertions.matches(isDisplayed()))

        assertOrderStatus(order.status)
        assertCustomerName(order.customerName)
        assertPayments(order)
        assertCustomerNote(order.customerNote)
        return this
    }

    fun tapOnCollectPayment(): PaymentSelectionScreen {
        clickOn(COLLECT_PAYMENT_BUTTON)
        return PaymentSelectionScreen()
    }
}
