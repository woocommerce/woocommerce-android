package com.woocommerce.android.ui.woopos.orders.details.refund

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
        totalTax: BigDecimal = BigDecimal.ZERO,
        taxes: List<Order.LineTaxEntry> = emptyList()
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
        taxes = taxes
    )

    private fun createOrder(items: List<Order.Item>) =
        OrderTestUtils.generateTestOrder().copy(items = items)

    @Test
    fun `given empty list, when invoke called, then returns zero`() {
        val order = createOrder(emptyList())

        val result = sut(emptyList(), order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `given single item with full quantity refund, when invoke called, then returns sum of taxes`() {
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 3f,
            totalTax = BigDecimal("6.00"),
            taxes = listOf(
                Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("3.00")),
                Order.LineTaxEntry(rateId = 2L, taxAmount = BigDecimal("3.00"))
            )
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, rowIndex = 0),
            createRefundableItem(orderItemId = 1L, rowIndex = 1),
            createRefundableItem(orderItemId = 1L, rowIndex = 2)
        )
        val order = createOrder(listOf(orderItem))

        val result = sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualTo(BigDecimal("6.00"))
    }

    @Test
    fun `given full quantity refund with totalTax different from sum of taxes, when invoke called, then returns sum of taxes`() {
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 2f,
            totalTax = BigDecimal("5.50"),
            taxes = listOf(
                Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("2.75")),
                Order.LineTaxEntry(rateId = 2L, taxAmount = BigDecimal("2.76"))
            )
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L, rowIndex = 0),
            createRefundableItem(orderItemId = 1L, rowIndex = 1)
        )
        val order = createOrder(listOf(orderItem))

        val result = sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualTo(BigDecimal("5.51"))
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

        val result = sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualTo(BigDecimal("4.00"))
    }

    @Test
    fun `given item not in order, when invoke called, then throws exception`() {
        val order = createOrder(emptyList())
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 999L)
        )

        val exception = try {
            sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = false)
            null
        } catch (e: IllegalArgumentException) {
            e
        }

        assertThat(exception).isNotNull
        assertThat(exception?.message).contains("Order item with ID 999 not found")
    }

    @Test
    fun `given item with zero quantity in order, when invoke called, then throws exception`() {
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 0f,
            totalTax = BigDecimal("10.00")
        )
        val order = createOrder(listOf(orderItem))
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L)
        )

        val exception = try {
            sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = false)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertThat(exception).isNotNull
        assertThat(exception?.message).contains("invalid quantity")
    }

    @Test
    fun `given multiple different items, when invoke called, then sums all taxes`() {
        val orderItems = listOf(
            createOrderItem(
                itemId = 1L,
                quantity = 2f,
                totalTax = BigDecimal("4.00"),
                taxes = listOf(Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("4.00")))
            ),
            createOrderItem(
                itemId = 2L,
                quantity = 1f,
                totalTax = BigDecimal("3.00"),
                taxes = listOf(Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("3.00")))
            ),
            createOrderItem(
                itemId = 3L,
                quantity = 3f,
                totalTax = BigDecimal("9.00"),
                taxes = listOf(Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("9.00")))
            )
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

        val result = sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualTo(BigDecimal("16.00"))
    }

    @Test
    fun `given partial refund with per-line rounding, when invoke called, then rounds per line`() {
        val orderItems = listOf(
            createOrderItem(itemId = 1L, quantity = 3f, totalTax = BigDecimal("10.00")),
            createOrderItem(itemId = 2L, quantity = 3f, totalTax = BigDecimal("10.00")),
            createOrderItem(itemId = 3L, quantity = 3f, totalTax = BigDecimal("10.00"))
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L),
            createRefundableItem(orderItemId = 2L),
            createRefundableItem(orderItemId = 3L)
        )
        val order = createOrder(orderItems)

        val result = sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualTo(BigDecimal("9.99"))
    }

    @Test
    fun `given partial refund with subtotal rounding, when invoke called, then rounds at subtotal level`() {
        val orderItems = listOf(
            createOrderItem(itemId = 1L, quantity = 3f, totalTax = BigDecimal("10.00")),
            createOrderItem(itemId = 2L, quantity = 3f, totalTax = BigDecimal("10.00")),
            createOrderItem(itemId = 3L, quantity = 3f, totalTax = BigDecimal("10.00"))
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 1L),
            createRefundableItem(orderItemId = 2L),
            createRefundableItem(orderItemId = 3L)
        )
        val order = createOrder(orderItems)

        val result = sut(refundableItems, order, numberOfDecimals = 2, roundAtSubtotal = true)

        assertThat(result).isEqualTo(BigDecimal("10.00"))
    }

    @Test
    fun `given lump-sum fee row, when invoke called, then tax is fee unit tax`() {
        val feeRow = WooPosRefundableItem(
            orderItemId = 99L,
            productId = 0L,
            variationId = 0L,
            name = "Tip",
            unitPrice = BigDecimal("12.50"),
            unitTax = BigDecimal("1.25"),
            formattedUnitPrice = "$12.50",
            formattedUnitTax = "$1.25",
            rowIndex = 0,
            isLumpSum = true,
        )
        val order = createOrder(emptyList())

        val result = sut(listOf(feeRow), order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualByComparingTo(BigDecimal("1.25"))
    }

    @Test
    fun `given product and lump-sum rows, when invoke called, then taxes are summed`() {
        val product = createRefundableItem(orderItemId = 1L)
        val orderItem = createOrderItem(
            itemId = 1L,
            quantity = 1f,
            totalTax = BigDecimal("2.00"),
            taxes = listOf(Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("2.00"))),
        )
        val fee = WooPosRefundableItem(
            orderItemId = 99L,
            productId = 0L,
            variationId = 0L,
            name = "Tip",
            unitPrice = BigDecimal("5.00"),
            unitTax = BigDecimal("0.50"),
            formattedUnitPrice = "$5.00",
            formattedUnitTax = "$0.50",
            rowIndex = 0,
            isLumpSum = true,
        )
        val order = createOrder(listOf(orderItem))

        val result = sut(listOf(product, fee), order, numberOfDecimals = 2, roundAtSubtotal = false)

        assertThat(result).isEqualByComparingTo(BigDecimal("2.50"))
    }
}
