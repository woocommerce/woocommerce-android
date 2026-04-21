package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Refund
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

class WooPosGroupRefundedItemsTest {

    private val sut = WooPosGroupRefundedItems()

    private fun createRefundItem(
        orderItemId: Long,
        productId: Long = 10L,
        quantity: Int = 1,
        total: BigDecimal = BigDecimal("10.00"),
        totalTax: BigDecimal = BigDecimal.ZERO,
    ) = Refund.Item(
        productId = productId,
        quantity = quantity,
        orderItemId = orderItemId,
        name = "Refund Product",
        total = total,
        totalTax = totalTax,
        price = if (quantity > 0) total / quantity.toBigDecimal() else total,
    )

    private fun createRefund(
        id: Long = 1L,
        items: List<Refund.Item>,
    ) = Refund(
        id = id,
        dateCreated = Date(),
        amount = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.total },
        reason = null,
        automaticGatewayRefund = false,
        items = items,
        shippingLines = emptyList(),
        feeLines = emptyList(),
    )

    @Test
    fun `given single refund with single item, when invoked, then correct grouping`() {
        // GIVEN
        val refunds = listOf(
            createRefund(
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 2, total = BigDecimal("8.00"))
                )
            )
        )

        // WHEN
        val result = sut(refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().orderItemId).isEqualTo(1L)
        assertThat(result.first().quantity).isEqualTo(2)
        assertThat(result.first().total).isEqualByComparingTo(BigDecimal("8.00"))
        assertThat(result.first().productId).isEqualTo(10L)
    }

    @Test
    fun `given multiple refunds for same item, when invoked, then quantities and totals summed`() {
        // GIVEN
        val refunds = listOf(
            createRefund(
                id = 1L,
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00"))
                )
            ),
            createRefund(
                id = 2L,
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 2, total = BigDecimal("8.00"))
                )
            ),
        )

        // WHEN
        val result = sut(refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().quantity).isEqualTo(3)
        assertThat(result.first().total).isEqualByComparingTo(BigDecimal("12.00"))
    }

    @Test
    fun `given multiple refunds for same item, when grouped, then price is recomputed from total and quantity`() {
        // GIVEN
        val refunds = listOf(
            createRefund(
                id = 1L,
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00"))
                )
            ),
            createRefund(
                id = 2L,
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 2, total = BigDecimal("8.00"))
                )
            ),
        )

        // WHEN
        val result = sut(refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().quantity).isEqualTo(3)
        assertThat(result.first().total).isEqualByComparingTo(BigDecimal("12.00"))
        assertThat(result.first().price).isEqualByComparingTo(BigDecimal("4.00"))
    }

    @Test
    fun `given multiple different items, when invoked, then separate entries`() {
        // GIVEN
        val refunds = listOf(
            createRefund(
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00")),
                    createRefundItem(orderItemId = 2L, productId = 20L, quantity = 1, total = BigDecimal("6.00")),
                )
            )
        )

        // WHEN
        val result = sut(refunds)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.map { it.orderItemId }).containsExactly(1L, 2L)
        assertThat(result.map { it.productId }).containsExactly(10L, 20L)
    }

    @Test
    fun `given empty refunds, when invoked, then empty result`() {
        // WHEN
        val result = sut(emptyList())

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given refunds with no items, when invoked, then empty result`() {
        // GIVEN
        val refunds = listOf(
            createRefund(items = emptyList())
        )

        // WHEN
        val result = sut(refunds)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given multiple refunds with tax, when grouped, then totalTax is aggregated`() {
        // GIVEN
        val refunds = listOf(
            createRefund(
                id = 1L,
                items = listOf(
                    createRefundItem(
                        orderItemId = 1L,
                        productId = 10L,
                        quantity = 1,
                        total = BigDecimal("4.00"),
                        totalTax = BigDecimal("0.40"),
                    )
                )
            ),
            createRefund(
                id = 2L,
                items = listOf(
                    createRefundItem(
                        orderItemId = 1L,
                        productId = 10L,
                        quantity = 2,
                        total = BigDecimal("8.00"),
                        totalTax = BigDecimal("0.80"),
                    )
                )
            ),
        )

        // WHEN
        val result = sut(refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().totalTax).isEqualByComparingTo(BigDecimal("1.20"))
        assertThat(result.first().total).isEqualByComparingTo(BigDecimal("12.00"))
    }
}
