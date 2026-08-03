package com.woocommerce.android.ui.woopos.orders.details.refund

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class WooPosBuildRefundLineItemsTest {

    private val sut = WooPosBuildRefundLineItems()

    @Test
    fun `given product rows, when built for preview, then groups by order item id with quantity and no amount`() {
        // GIVEN — two units of the same product
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
        )

        // WHEN
        val result = sut.forPreview(items)

        // THEN
        assertThat(result).hasSize(1)
        val lineItem = result.first()
        assertThat(lineItem.lineItemId).isEqualTo(1L)
        assertThat(lineItem.quantity).isEqualTo(2)
        assertThat(lineItem.refundTotal).isNull()
    }

    @Test
    fun `given a fee row, when built for preview, then sends tax-inclusive amount and no quantity`() {
        // GIVEN
        val items = listOf(
            feeRow(orderItemId = 99L, unitPrice = BigDecimal("10.00"), unitTax = BigDecimal("1.50")),
        )

        // WHEN
        val result = sut.forPreview(items)

        // THEN
        assertThat(result).hasSize(1)
        val lineItem = result.first()
        assertThat(lineItem.lineItemId).isEqualTo(99L)
        assertThat(lineItem.quantity).isNull()
        assertThat(lineItem.refundTotal).isEqualByComparingTo(BigDecimal("11.50"))
    }

    @Test
    fun `given mixed products and fees, when built for preview, then builds both forms`() {
        // GIVEN
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            feeRow(orderItemId = 99L, unitPrice = BigDecimal("5.00"), unitTax = BigDecimal.ZERO),
        )

        // WHEN
        val result = sut.forPreview(items)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.any { it.lineItemId == 1L && it.quantity == 1 && it.refundTotal == null }).isTrue()
        assertThat(result.any { it.lineItemId == 99L && it.quantity == null }).isTrue()
    }

    @Test
    fun `given product rows, when built for computed create, then groups by order item id with quantity`() {
        // GIVEN — two units of the same product
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
        )

        // WHEN
        val result = sut.forComputedCreate(items)

        // THEN
        assertThat(result).hasSize(1)
        val lineItem = result.first()
        assertThat(lineItem.lineItemId).isEqualTo(1L)
        assertThat(lineItem.quantity).isEqualTo(2)
        assertThat(lineItem.refundTotal).isNull()
    }

    @Test
    fun `given mixed products and fees, when built for computed create, then builds both forms`() {
        // GIVEN
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            feeRow(orderItemId = 99L, unitPrice = BigDecimal("10.00"), unitTax = BigDecimal("1.50")),
        )

        // WHEN
        val result = sut.forComputedCreate(items)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.any { it.lineItemId == 1L && it.quantity == 1 && it.refundTotal == null }).isTrue()
        val feeLine = result.single { it.lineItemId == 99L }
        assertThat(feeLine.quantity).isNull()
        assertThat(feeLine.refundTotal).isEqualByComparingTo(BigDecimal("11.50"))
    }

    @Test
    fun `given a zero-total fee row, when built for preview, then the line is not sent`() {
        // GIVEN — a zeroed fee alongside a product and a nonzero fee. The server rejects a gross
        // line refund of zero with invalid_refund_total, so the zero line must be dropped.
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            feeRow(orderItemId = 98L, unitPrice = BigDecimal("0.00"), unitTax = BigDecimal.ZERO),
            feeRow(orderItemId = 99L, unitPrice = BigDecimal("10.00"), unitTax = BigDecimal("1.50")),
        )

        // WHEN
        val result = sut.forPreview(items)

        // THEN
        assertThat(result.map { it.lineItemId }).containsExactly(1L, 99L)
    }

    @Test
    fun `given a fee row whose only amount is tax, when built for preview, then the line is sent`() {
        // GIVEN — zero price but nonzero tax still has a nonzero gross amount
        val items = listOf(
            feeRow(orderItemId = 98L, unitPrice = BigDecimal.ZERO, unitTax = BigDecimal("0.10")),
        )

        // WHEN
        val result = sut.forPreview(items)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().refundTotal).isEqualByComparingTo(BigDecimal("0.10"))
    }

    @Test
    fun `given only zero-total fee rows, when built for preview, then the result is empty`() {
        // GIVEN — an empty result makes the preview fall back to the local flow instead of
        // sending a request the server would reject.
        val items = listOf(
            feeRow(orderItemId = 98L, unitPrice = BigDecimal("0.00"), unitTax = BigDecimal.ZERO),
        )

        // WHEN
        val result = sut.forPreview(items)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given a zero-total fee row, when built for computed create, then the line is not sent`() {
        // GIVEN — the create must send exactly what was previewed, so both builders filter alike
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            feeRow(orderItemId = 98L, unitPrice = BigDecimal("0.00"), unitTax = BigDecimal.ZERO),
            feeRow(orderItemId = 99L, unitPrice = BigDecimal("10.00"), unitTax = BigDecimal("1.50")),
        )

        // WHEN
        val result = sut.forComputedCreate(items)

        // THEN
        assertThat(result.map { it.lineItemId }).containsExactly(1L, 99L)
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
