package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

class WooPosGetNonRefundedItemsTest {

    private val sut = WooPosGetNonRefundedItems()

    private fun createOrderItem(
        itemId: Long,
        productId: Long = 10L,
        name: String = "Product $itemId",
        price: BigDecimal = BigDecimal("10.00"),
        quantity: Float = 1f,
    ) = Order.Item(
        itemId = itemId,
        productId = productId,
        name = name,
        price = price,
        sku = "",
        quantity = quantity,
        subtotal = price * quantity.toBigDecimal(),
        subtotalTax = BigDecimal.ZERO,
        totalTax = BigDecimal.ZERO,
        total = price * quantity.toBigDecimal(),
        variationId = 0,
        attributesList = emptyList(),
    )

    private fun createRefundItem(
        orderItemId: Long,
        quantity: Int = 1,
        total: BigDecimal = BigDecimal("10.00"),
    ) = Refund.Item(
        productId = 10L,
        quantity = quantity,
        orderItemId = orderItemId,
        name = "Refund Product",
        total = total,
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

    private fun createOrder(items: List<Order.Item>): Order {
        return com.woocommerce.android.ui.orders.OrderTestUtils.generateTestOrder().copy(items = items)
    }

    @Test
    fun `given no refunds, when invoked, then all items returned`() {
        // GIVEN
        val items = listOf(
            createOrderItem(itemId = 1L, name = "Cup", price = BigDecimal("4.00"), quantity = 2f),
        )
        val order = createOrder(items)

        // WHEN
        val result = sut(order, emptyList())

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("Cup")
        assertThat(result.first().quantity).isEqualTo(2f)
    }

    @Test
    fun `given fully refunded item, when invoked, then item excluded`() {
        // GIVEN
        val items = listOf(
            createOrderItem(itemId = 1L, name = "Cup", price = BigDecimal("4.00"), quantity = 1f),
            createOrderItem(itemId = 2L, name = "Plate", price = BigDecimal("6.00"), quantity = 1f),
        )
        val order = createOrder(items)
        val refunds = listOf(
            createRefund(
                items = listOf(createRefundItem(orderItemId = 1L, quantity = 1, total = BigDecimal("4.00")))
            )
        )

        // WHEN
        val result = sut(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("Plate")
    }

    @Test
    fun `given partial refund, when invoked, then item has prorated total`() {
        // GIVEN
        val items = listOf(
            createOrderItem(itemId = 1L, name = "Cup", price = BigDecimal("4.00"), quantity = 3f),
        )
        val order = createOrder(items)
        val refunds = listOf(
            createRefund(
                items = listOf(createRefundItem(orderItemId = 1L, quantity = 1, total = BigDecimal("4.00")))
            )
        )

        // WHEN
        val result = sut(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().quantity).isEqualTo(2f)
        assertThat(result.first().total).isEqualByComparingTo(BigDecimal("8.00"))
    }

    @Test
    fun `given same product in different line items, when one is refunded, then only that line item affected`() {
        // GIVEN
        val items = listOf(
            createOrderItem(
                itemId = 1L,
                productId = 10L,
                name = "Cup (Red)",
                price = BigDecimal("4.00"),
                quantity = 1f
            ),
            createOrderItem(
                itemId = 2L,
                productId = 10L,
                name = "Cup (Blue)",
                price = BigDecimal("4.00"),
                quantity = 1f
            ),
        )
        val order = createOrder(items)
        val refunds = listOf(
            createRefund(
                items = listOf(createRefundItem(orderItemId = 1L, quantity = 1, total = BigDecimal("4.00")))
            )
        )

        // WHEN
        val result = sut(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("Cup (Blue)")
    }

    @Test
    fun `given zero-quantity item with no refunds, when invoked, then item is preserved`() {
        // GIVEN
        val items = listOf(
            createOrderItem(itemId = 1L, name = "Free Gift", price = BigDecimal.ZERO, quantity = 0f),
        )
        val order = createOrder(items)

        // WHEN
        val result = sut(order, emptyList())

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().name).isEqualTo("Free Gift")
        assertThat(result.first().quantity).isEqualTo(0f)
    }

    @Test
    fun `given zero-quantity item with refunds, when invoked, then item is excluded`() {
        // GIVEN
        val items = listOf(
            createOrderItem(itemId = 1L, name = "Free Gift", price = BigDecimal.ZERO, quantity = 0f),
        )
        val order = createOrder(items)
        val refunds = listOf(
            createRefund(
                items = listOf(createRefundItem(orderItemId = 1L, quantity = 1, total = BigDecimal.ZERO))
            )
        )

        // WHEN
        val result = sut(order, refunds)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given multiple refunds for same item, when invoked, then quantities are folded correctly`() {
        // GIVEN
        val items = listOf(
            createOrderItem(itemId = 1L, name = "Cup", price = BigDecimal("4.00"), quantity = 5f),
        )
        val order = createOrder(items)
        val refunds = listOf(
            createRefund(
                id = 1L,
                items = listOf(createRefundItem(orderItemId = 1L, quantity = 1, total = BigDecimal("4.00")))
            ),
            createRefund(
                id = 2L,
                items = listOf(createRefundItem(orderItemId = 1L, quantity = 2, total = BigDecimal("8.00")))
            ),
        )

        // WHEN
        val result = sut(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().quantity).isEqualTo(2f)
    }
}
