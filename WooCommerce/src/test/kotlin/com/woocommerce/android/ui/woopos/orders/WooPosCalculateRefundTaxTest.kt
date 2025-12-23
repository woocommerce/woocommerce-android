package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import java.math.BigDecimal
import kotlin.test.Test

class WooPosCalculateRefundTaxTest {
    private lateinit var sut: WooPosCalculateRefundTax

    @Before
    fun setup() {
        sut = WooPosCalculateRefundTax()
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

    private fun createOrderItem(
        itemId: Long,
        quantity: Float = 1f,
        totalTax: BigDecimal = BigDecimal.ZERO
    ) = Order.Item(
        itemId = itemId,
        productId = 100L,
        name = "Test Product",
        price = BigDecimal("20.00"),
        sku = "",
        quantity = quantity,
        subtotal = BigDecimal("20.00"),
        subtotalTax = BigDecimal.ZERO,
        totalTax = totalTax,
        total = BigDecimal("20.00"),
        variationId = 0L,
        attributesList = emptyList(),
        taxes = emptyList()
    )

    private fun createOrder(items: List<Order.Item>) =
        OrderTestUtils.generateTestOrder().copy(items = items)

    @Test
    fun `given empty list, when invoke called, then returns zero`() {
        val order = createOrder(emptyList())

        val result = sut(emptyList(), order)

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `given single item with full quantity refund, when invoke called, then returns exact totalTax`() {
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 3f,
            totalTax = BigDecimal("6.00")
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, rowIndex = 0),
            createRefundableItem(orderItemId = 1L, rowIndex = 1),
            createRefundableItem(orderItemId = 1L, rowIndex = 2)
        )
        val order = createOrder(listOf(orderItem))

        val result = sut(refundableItems, order)

        assertThat(result).isEqualTo(BigDecimal("6.00"))
    }

    @Test
    fun `given single item with partial quantity refund, when invoke called, then calculates proportional tax`() {
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 5f,
            totalTax = BigDecimal("10.00")
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, rowIndex = 0),
            createRefundableItem(orderItemId = 1L, rowIndex = 1)
        )
        val order = createOrder(listOf(orderItem))

        val result = sut(refundableItems, order)

        assertThat(result).isEqualTo(BigDecimal("4.00"))
    }

    @Test
    fun `given item not in order, when invoke called, then returns zero for that item`() {
        val order = createOrder(emptyList())
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 999L)
        )

        val result = sut(refundableItems, order)

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `given item with zero quantity in order, when invoke called, then returns zero`() {
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 0f,
            totalTax = BigDecimal("10.00")
        )
        val order = createOrder(listOf(orderItem))
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L)
        )

        val result = sut(refundableItems, order)

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `given multiple different items, when invoke called, then sums all taxes`() {
        val orderItems = listOf(
            createOrderItem(itemId = 1L, quantity = 2f, totalTax = BigDecimal("4.00")),
            createOrderItem(itemId = 2L, quantity = 1f, totalTax = BigDecimal("3.00")),
            createOrderItem(itemId = 3L, quantity = 3f, totalTax = BigDecimal("9.00"))
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, rowIndex = 0),
            createRefundableItem(orderItemId = 1L, rowIndex = 1),
            createRefundableItem(orderItemId = 2L),
            createRefundableItem(orderItemId = 3L, rowIndex = 0),
            createRefundableItem(orderItemId = 3L, rowIndex = 1),
            createRefundableItem(orderItemId = 3L, rowIndex = 2)
        )
        val order = createOrder(orderItems)

        val result = sut(refundableItems, order)

        assertThat(result).isEqualTo(BigDecimal("16.00"))
    }

    @Test
    fun `given partial refund with rounding, when invoke called, then rounds correctly`() {
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 3f,
            totalTax = BigDecimal("10.00")
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L)
        )
        val order = createOrder(listOf(orderItem))

        val result = sut(refundableItems, order)

        assertThat(result).isEqualTo(BigDecimal("3.33"))
    }
}
