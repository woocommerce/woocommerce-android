package com.woocommerce.android.ui.woopos.orders.details.refund

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import java.math.BigDecimal
import kotlin.test.Test

class WooPosCalculateRefundSubtotalTest {
    private lateinit var sut: WooPosCalculateRefundSubtotal

    @Before
    fun setup() {
        sut = WooPosCalculateRefundSubtotal()
    }

    private fun createRefundableItem(
        orderItemId: Long,
        unitPrice: BigDecimal = BigDecimal("20.00"),
        rowIndex: Int = 0
    ) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = 100L,
        variationId = 0L,
        name = "Test Product",
        unitPrice = unitPrice,
        unitTax = BigDecimal("2.00"),
        formattedUnitPrice = "$$unitPrice",
        formattedUnitTax = "$2.00",
        rowIndex = rowIndex
    )

    @Test
    fun `given empty list, when invoke called, then returns zero`() {
        val result = sut(emptyList(), 2)
        assertThat(result).isEqualTo(BigDecimal("0"))
    }

    @Test
    fun `given single item, when invoke called, then returns unit price`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"))
        )

        val result = sut(refundableItems, 2)

        assertThat(result).isEqualTo(BigDecimal("20.00"))
    }

    @Test
    fun `given multiple items with same orderItemId, when invoke called, then returns unit price times quantity`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"), rowIndex = 0),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"), rowIndex = 1),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"), rowIndex = 2)
        )

        val result = sut(refundableItems, 2)

        assertThat(result).isEqualTo(BigDecimal("60.00"))
    }

    @Test
    fun `given items with different orderItemIds, when invoke called, then sums all items`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.00")),
            createRefundableItem(orderItemId = 2L, unitPrice = BigDecimal("20.00")),
            createRefundableItem(orderItemId = 3L, unitPrice = BigDecimal("30.00"))
        )

        val result = sut(refundableItems, 2)

        assertThat(result).isEqualTo(BigDecimal("60.00"))
    }

    @Test
    fun `given mixed orderItemIds with different quantities, when invoke called, then calculates correct subtotal`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.00"), rowIndex = 0),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.00"), rowIndex = 1),
            createRefundableItem(orderItemId = 2L, unitPrice = BigDecimal("20.00"), rowIndex = 0),
            createRefundableItem(orderItemId = 3L, unitPrice = BigDecimal("15.00"), rowIndex = 0),
            createRefundableItem(orderItemId = 3L, unitPrice = BigDecimal("15.00"), rowIndex = 1),
            createRefundableItem(orderItemId = 3L, unitPrice = BigDecimal("15.00"), rowIndex = 2)
        )

        val result = sut(refundableItems, 2)

        assertThat(result).isEqualTo(BigDecimal("85.00"))
    }

    @Test
    fun `given unit price with more decimals, when invoke called, then rounds per line item`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.333"), rowIndex = 0),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.333"), rowIndex = 1),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.333"), rowIndex = 2)
        )

        val result = sut(refundableItems, 2)

        assertThat(result).isEqualTo(BigDecimal("31.00"))
    }

    @Test
    fun `given unit price requiring rounding, when invoke called with 3 decimals, then rounds to 3 decimals`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.33333"), rowIndex = 0),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.33333"), rowIndex = 1)
        )

        val result = sut(refundableItems, 3)

        assertThat(result).isEqualTo(BigDecimal("20.667"))
    }

    @Test
    fun `given lump-sum fee row, when invoke called, then total is the fee unit price, not multiplied by quantity`() {
        val feeRow = WooPosRefundableItem(
            orderItemId = 99L,
            productId = 0L,
            variationId = 0L,
            name = "Tip",
            unitPrice = BigDecimal("12.50"),
            unitTax = BigDecimal.ZERO,
            formattedUnitPrice = "$12.50",
            formattedUnitTax = "$0.00",
            rowIndex = 0,
            isLumpSum = true,
        )

        val result = sut(listOf(feeRow), 2)

        assertThat(result).isEqualByComparingTo(BigDecimal("12.50"))
    }

    @Test
    fun `given product and lump-sum rows, when invoke called, then both are summed`() {
        val product = createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"))
        val fee = WooPosRefundableItem(
            orderItemId = 99L,
            productId = 0L,
            variationId = 0L,
            name = "Tip",
            unitPrice = BigDecimal("5.00"),
            unitTax = BigDecimal.ZERO,
            formattedUnitPrice = "$5.00",
            formattedUnitTax = "$0.00",
            rowIndex = 0,
            isLumpSum = true,
        )

        val result = sut(listOf(product, fee), 2)

        assertThat(result).isEqualByComparingTo(BigDecimal("25.00"))
    }
}
