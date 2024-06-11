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
import com.woocommerce.android.e2e.helpers.util.ProductData
import com.woocommerce.android.e2e.helpers.util.Screen
import org.hamcrest.Matchers

class SingleOrderScreen : Screen(R.id.toolbar) {
    fun goBackToOrdersScreen(): OrderListScreen {
        if (isElementDisplayed(R.id.orderDetail_container)) {
            pressBack()
        }

        return OrderListScreen()
    }

    private fun assertIdAndTextDisplayed(id: Int, text: String?) {
        Espresso.onView(
            Matchers.allOf(
                withId(id),
                withText(text)
            )
        ).check(ViewAssertions.matches(isDisplayed()))
    }

    fun assertOrderStatus(orderStatus: String): SingleOrderScreen {
        assertIdAndTextDisplayed(R.id.orderStatus_orderTags, orderStatus)
        return this
    }

    fun assertCustomerName(customerName: String): SingleOrderScreen {
        assertIdAndTextDisplayed(R.id.orderStatus_header, customerName)
        return this
    }

    fun assertOrderId(orderId: Int): SingleOrderScreen {
        Espresso.onView(withId(R.id.toolbar))
            .check(ViewAssertions.matches(hasDescendant(withText("Order #$orderId"))))
            .check(ViewAssertions.matches(isDisplayed()))
        return this
    }

    fun assertCustomerNote(customerNote: String): SingleOrderScreen {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.isDescendantOfA(withId(R.id.customerInfo_customerNote)),
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

    fun assertPayments(order: OrderData): SingleOrderScreen {
        Espresso.onView(withId(R.id.paymentInfo_total))
            .perform(NestedScrollViewExtension())

        with(order) {
            if (productsTotalRaw.isNotBlank()) {
                assertIdAndTextDisplayed(R.id.paymentInfo_productsTotal, productsTotalAmount)
            }

            if (shippingRaw.isNotBlank()) {
                assertIdAndTextDisplayed(R.id.paymentInfo_shippingTotal, shippingAmount)
            }

            if (feeRaw.isNotBlank()) {
                assertIdAndTextDisplayed(R.id.paymentInfo_Fees, feeAmount)
            }

            if (taxesRaw.isNotBlank()) {
                assertIdAndTextDisplayed(R.id.paymentInfo_taxesTotal, taxesAmount)
            }
        }

        assertIdAndTextDisplayed(R.id.paymentInfo_total, order.total)
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

    fun assertOrderHasProduct(product: ProductData): SingleOrderScreen {
        Espresso.onView(
            Matchers.allOf(
                // We start with making sure that there's a product name on screen
                Matchers.allOf(
                    withId(R.id.productInfo_name),
                    withText(product.name)
                ),
                // And that this product has a sibling with expected Price
                ViewMatchers.hasSibling(
                    Matchers.allOf(
                        withId(R.id.productInfo_total),
                        withText("$${product.priceDiscountedRaw}.00")
                    )
                ),
                // And that this product has a sibling with expected SKU
                ViewMatchers.hasSibling(
                    Matchers.allOf(
                        withId(R.id.productInfo_SKU),
                        withText("SKU: ${product.sku}")
                    )
                ),
                // And that all of them are children of Products List
                ViewMatchers.isDescendantOfA(withId(R.id.orderDetail_productList)),
            )
        )
            .check(ViewAssertions.matches(isDisplayed()))

        return this
    }

    fun tapOnCollectPayment(): PaymentSelectionScreen {
        clickOn(R.id.paymentInfo_collectCardPresentPaymentButton)
        return PaymentSelectionScreen()
    }
}
