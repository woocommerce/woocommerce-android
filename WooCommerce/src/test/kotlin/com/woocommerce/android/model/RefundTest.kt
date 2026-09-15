package com.woocommerce.android.model

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
}
