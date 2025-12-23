package com.woocommerce.android.ui.woopos.orders

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
        val result = sut(emptyList())

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `given single item, when invoke called, then returns unit price`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"))
        )

        val result = sut(refundableItems)

        assertThat(result).isEqualTo(BigDecimal("20.00"))
    }

    @Test
    fun `given multiple items with same orderItemId, when invoke called, then returns unit price times quantity`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"), rowIndex = 0),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"), rowIndex = 1),
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("20.00"), rowIndex = 2)
        )

        val result = sut(refundableItems)

        assertThat(result).isEqualTo(BigDecimal("60.00"))
    }

    @Test
    fun `given items with different orderItemIds, when invoke called, then sums all items`() {
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.00")),
            createRefundableItem(orderItemId = 2L, unitPrice = BigDecimal("20.00")),
            createRefundableItem(orderItemId = 3L, unitPrice = BigDecimal("30.00"))
        )

        val result = sut(refundableItems)

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

        val result = sut(refundableItems)

        assertThat(result).isEqualTo(BigDecimal("85.00"))
    }
}
