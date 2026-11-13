package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem
import java.math.BigDecimal

class ServerRefundLineItemsTest {
    @Test
    fun `given products, shipping and fees, when building preview lines, then products use quantity and others one`() {
        // GIVEN
        val items = listOf(givenProduct(quantity = 2), givenShipping(total = "5.00"), givenFee(total = "2.00"))

        // WHEN
        val lines = items.toRefundPreviewLineItems()

        // THEN
        assertThat(lines).containsExactly(
            RefundPreviewLineItem.quantityBased(lineItemId = PRODUCT_LINE_ID, quantity = 2),
            RefundPreviewLineItem.quantityBased(lineItemId = SHIPPING_LINE_ID, quantity = 1),
            RefundPreviewLineItem.quantityBased(lineItemId = FEE_LINE_ID, quantity = 1)
        )
    }

    @Test
    fun `given the same items, when building computed create lines, then they match the preview lines`() {
        // GIVEN
        val items = listOf(givenProduct(quantity = 2), givenShipping(total = "5.00"), givenFee(total = "2.00"))

        // WHEN
        val lines = items.toComputedRefundLineItems()

        // THEN
        assertThat(lines).containsExactly(
            ComputedRefundLineItem.quantityBased(lineItemId = PRODUCT_LINE_ID, quantity = 2),
            ComputedRefundLineItem.quantityBased(lineItemId = SHIPPING_LINE_ID, quantity = 1),
            ComputedRefundLineItem.quantityBased(lineItemId = FEE_LINE_ID, quantity = 1)
        )
    }

    @Test
    fun `given product with no quantity chosen, when building lines, then it is left out`() {
        // GIVEN
        val items = listOf(givenProduct(quantity = 0), givenFee(total = "2.00"))

        // WHEN
        val lines = items.toRefundPreviewLineItems()

        // THEN
        assertThat(lines).containsExactly(RefundPreviewLineItem.quantityBased(lineItemId = FEE_LINE_ID, quantity = 1))
    }

    @Test
    fun `given free shipping and zero fee, when building lines, then they are left out`() {
        // GIVEN
        val items = listOf(givenProduct(quantity = 1), givenShipping(total = "0.00"), givenFee(total = "0"))

        // WHEN
        val lines = items.toComputedRefundLineItems()

        // THEN
        assertThat(lines).containsExactly(
            ComputedRefundLineItem.quantityBased(lineItemId = PRODUCT_LINE_ID, quantity = 1)
        )
    }

    @Test
    fun `given a discount fee, when building lines, then it is kept`() {
        // GIVEN
        val items = listOf(givenProduct(quantity = 1), givenFee(total = "-3.00"))

        // WHEN
        val lines = items.toRefundPreviewLineItems()

        // THEN
        assertThat(lines).contains(RefundPreviewLineItem.quantityBased(lineItemId = FEE_LINE_ID, quantity = 1))
    }

    private fun givenProduct(quantity: Int) = ProductRefundListItem(
        orderItem = OrderTestUtils.generateTestOrderItems().first().copy(itemId = PRODUCT_LINE_ID),
        quantity = quantity
    )

    private fun givenShipping(total: String) = ShippingRefundListItem(
        shippingLine = Order.ShippingLine(
            itemId = SHIPPING_LINE_ID,
            methodId = "flat_rate",
            methodTitle = "Flat rate",
            totalTax = BigDecimal.ZERO,
            total = BigDecimal(total),
            taxes = emptyList()
        ),
        isSelected = true
    )

    private fun givenFee(total: String) = FeeRefundListItem(
        feeLine = Order.FeeLine.EMPTY.copy(id = FEE_LINE_ID, total = BigDecimal(total)),
        isSelected = true
    )

    private companion object {
        const val PRODUCT_LINE_ID = 21L
        const val SHIPPING_LINE_ID = 22L
        const val FEE_LINE_ID = 23L
    }
}
