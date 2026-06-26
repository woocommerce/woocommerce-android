package com.woocommerce.android.ui.woopos.orders.details.refund

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class WooPosBuildRefundV4LineItemsTest {

    private val sut = WooPosBuildRefundV4LineItems()

    @Test
    fun `given product rows, when invoked, then groups by order item id with quantity and no amount`() {
        // GIVEN — two units of the same product
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
        )

        // WHEN
        val result = sut(items)

        // THEN
        assertThat(result).hasSize(1)
        val lineItem = result.first()
        assertThat(lineItem.lineItemId).isEqualTo(1L)
        assertThat(lineItem.quantity).isEqualTo(2)
        assertThat(lineItem.refundTotal).isNull()
    }

    @Test
    fun `given a fee row, when invoked, then sends tax-inclusive amount and no quantity`() {
        // GIVEN
        val items = listOf(
            feeRow(orderItemId = 99L, unitPrice = BigDecimal("10.00"), unitTax = BigDecimal("1.50")),
        )

        // WHEN
        val result = sut(items)

        // THEN
        assertThat(result).hasSize(1)
        val lineItem = result.first()
        assertThat(lineItem.lineItemId).isEqualTo(99L)
        assertThat(lineItem.quantity).isNull()
        assertThat(lineItem.refundTotal).isEqualByComparingTo(BigDecimal("11.50"))
    }

    @Test
    fun `given mixed products and fees, when invoked, then builds both forms`() {
        // GIVEN
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            feeRow(orderItemId = 99L, unitPrice = BigDecimal("5.00"), unitTax = BigDecimal.ZERO),
        )

        // WHEN
        val result = sut(items)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.any { it.lineItemId == 1L && it.quantity == 1 && it.refundTotal == null }).isTrue()
        assertThat(result.any { it.lineItemId == 99L && it.quantity == null }).isTrue()
    }

    private fun productRow(orderItemId: Long, rowIndex: Int) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = 10L,
        variationId = 0L,
        name = "Product",
        unitPrice = BigDecimal("20.00"),
        unitTax = BigDecimal("2.00"),
        formattedUnitPrice = "$20.00",
        formattedUnitTax = "$2.00",
        rowIndex = rowIndex,
        isLumpSum = false,
    )

    private fun feeRow(orderItemId: Long, unitPrice: BigDecimal, unitTax: BigDecimal) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = 0L,
        variationId = 0L,
        name = "Fee",
        unitPrice = unitPrice,
        unitTax = unitTax,
        formattedUnitPrice = "",
        formattedUnitTax = "",
        rowIndex = 0,
        isLumpSum = true,
    )
}
