package com.woocommerce.android.model

import com.woocommerce.android.ui.orders.OrderTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

class RefundTest {
    @Test
    fun `given partial shipping refunds, when grouping lines, then sum refunds for each original shipping item`() {
        // GIVEN
        val shippingLine = givenShippingLine(itemId = 37, total = "-4.00", totalTax = "0.80")
        val remainingShipping = shippingLine.copy(total = BigDecimal("-6.00"), totalTax = BigDecimal("1.20"))
        val otherShipping = shippingLine.copy(itemId = 38)
        val refunds = listOf(givenRefund(shippingLine, otherShipping), givenRefund(remainingShipping))

        // WHEN
        val shippingLines = refunds.getRefundedShippingLines()

        // THEN
        assertThat(shippingLines).containsExactly(
            shippingLine.copy(total = BigDecimal("-10.00"), totalTax = BigDecimal("2.00")),
            otherShipping
        )
    }

    @Test
    fun `given shipping refunds without original item IDs, when grouping lines, then keep lines separate`() {
        // GIVEN
        val firstShipping = givenShippingLine(itemId = -1, total = "-4.00", totalTax = "0.80")
        val secondShipping = firstShipping.copy(total = BigDecimal("-6.00"), totalTax = BigDecimal("1.20"))
        val refunds = listOf(givenRefund(firstShipping), givenRefund(secondShipping))

        // WHEN
        val shippingLines = refunds.getRefundedShippingLines()

        // THEN
        assertThat(shippingLines).containsExactly(firstShipping, secondShipping)
    }

    @Test
    fun `given another line of the same product was refunded, when getting non-refunded products, then only that line is reduced`() {
        // GIVEN
        val refundedLine = givenOrderItem(itemId = 81, quantity = 1F)
        val partlyRefundedLine = givenOrderItem(itemId = 82, quantity = 3F)
        val refunds = listOf(
            givenItemsRefund(givenRefundedItem(orderItemId = 81, quantity = 1)),
            givenItemsRefund(givenRefundedItem(orderItemId = 82, quantity = 1))
        )

        // WHEN
        val products = refunds.getNonRefundedProducts(listOf(refundedLine, partlyRefundedLine))

        // THEN
        assertThat(products.map { it.itemId to it.quantity }).containsExactly(82L to 2F)
    }

    @Test
    fun `given refunded items without order line ids, when getting max refund quantities, then match by product`() {
        // GIVEN
        val line = givenOrderItem(itemId = 81, quantity = 3F)
        val refunds = listOf(givenItemsRefund(givenRefundedItem(orderItemId = -1, quantity = 2)))

        // WHEN
        val quantities = refunds.getMaxRefundQuantities(listOf(line))

        // THEN
        assertThat(quantities).containsExactlyEntriesOf(mapOf(81L to 1F))
    }

    private fun givenOrderItem(itemId: Long, quantity: Float) =
        OrderTestUtils.generateTestOrderItems(productId = PRODUCT_ID, quantity = quantity).first().copy(itemId = itemId)

    private fun givenRefundedItem(orderItemId: Long, quantity: Int) = Refund.Item(
        productId = PRODUCT_ID,
        quantity = quantity,
        orderItemId = orderItemId
    )

    private fun givenItemsRefund(vararg items: Refund.Item) = Refund(
        id = 1,
        dateCreated = Date(0),
        amount = BigDecimal.TEN,
        reason = null,
        automaticGatewayRefund = false,
        items = items.toList(),
        shippingLines = emptyList(),
        feeLines = emptyList()
    )

    private fun givenShippingLine(itemId: Long, total: String, totalTax: String) = Refund.ShippingLine(
        itemId = itemId,
        methodId = "flat_rate",
        methodTitle = "Flat rate",
        total = BigDecimal(total),
        totalTax = BigDecimal(totalTax)
    )

    private fun givenRefund(vararg shippingLines: Refund.ShippingLine) = Refund(
        id = 1,
        dateCreated = Date(0),
        amount = shippingLines.sumOf { -it.total + it.totalTax },
        reason = null,
        automaticGatewayRefund = false,
        items = emptyList(),
        shippingLines = shippingLines.toList(),
        feeLines = emptyList()
    )

    private companion object {
        const val PRODUCT_ID = 11L
    }
}
