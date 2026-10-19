package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestTax
import java.math.BigDecimal

class RefundsExtTest {
    @Test
    fun `given tax that does not divide evenly per unit, when refunding all units, then line tax is not exceeded`() {
        // GIVEN
        val item = givenRefundListItem(quantity = 3F, total = "9.99", taxes = listOf(TAX_RATE_ID to "0.50"))

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 3, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("9.99")
        assertThat(result.totalTax).isEqualByComparingTo("0.50")
        assertThat(result.taxes).containsExactly(TaxRefund(TAX_RATE_ID, BigDecimal("0.50")))
    }

    @Test
    fun `given tax that does not divide evenly per unit, when refunding one unit, then amounts are rounded per line`() {
        // GIVEN
        val item = givenRefundListItem(quantity = 3F, total = "9.99", taxes = listOf(TAX_RATE_ID to "0.50"))

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("3.33")
        assertThat(result.totalTax).isEqualByComparingTo("0.17")
    }

    @Test
    fun `given two units already refunded one by one, when refunding the last unit, then what is left is refunded`() {
        // GIVEN
        val item = givenRefundListItem(
            quantity = 3F,
            total = "9.99",
            taxes = listOf(TAX_RATE_ID to "0.50"),
            maxQuantity = 1F,
            refundedTotal = "6.66",
            refundedTaxes = listOf(TAX_RATE_ID to "0.34")
        )

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("3.33")
        assertThat(result.totalTax).isEqualByComparingTo("0.16")
    }

    @Test
    fun `given earlier refunds rounded the tax up, when refunding a unit, then tax already refunded is not refunded again`() {
        // GIVEN
        val item = givenRefundListItem(
            quantity = 4F,
            total = "4.00",
            taxes = listOf(TAX_RATE_ID to "0.02"),
            maxQuantity = 2F,
            refundedTotal = "2.00",
            refundedTaxes = listOf(TAX_RATE_ID to "0.02")
        )

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("1.00")
        assertThat(result.totalTax).isEqualByComparingTo("0.00")
    }

    @Test
    fun `given currency without decimals, when refunding one unit, then amounts are rounded to whole units`() {
        // GIVEN
        val item = givenRefundListItem(quantity = 3F, total = "1000", taxes = listOf(TAX_RATE_ID to "100"))

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 0)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("333")
        assertThat(result.totalTax).isEqualByComparingTo("33")
    }

    @Test
    fun `given two tax rates, when refunding all units, then each rate is refunded in full`() {
        // GIVEN
        val item = givenRefundListItem(
            quantity = 2F,
            total = "5.00",
            taxes = listOf(TAX_RATE_ID to "0.25", OTHER_TAX_RATE_ID to "0.15")
        )

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 2, decimals = 2)

        // THEN
        assertThat(result.taxes).containsExactly(
            TaxRefund(TAX_RATE_ID, BigDecimal("0.25")),
            TaxRefund(OTHER_TAX_RATE_ID, BigDecimal("0.15"))
        )
        assertThat(result.total).isEqualByComparingTo("5.40")
    }

    @Test
    fun `given a negative line, when refunding it, then its negative total is refunded`() {
        // GIVEN
        val item = givenRefundListItem(quantity = 1F, total = "-20.00", taxes = emptyList())

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("-20.00")
        assertThat(result.total).isEqualByComparingTo("-20.00")
    }

    @Test
    fun `given a negative line, when refunding one unit, then its negative share is refunded`() {
        // GIVEN
        val item = givenRefundListItem(quantity = 3F, total = "-9.99", taxes = listOf(TAX_RATE_ID to "-0.50"))

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("-3.33")
        assertThat(result.taxes).containsExactly(TaxRefund(TAX_RATE_ID, BigDecimal("-0.17")))
    }

    @Test
    fun `given two units of a negative line already refunded, when refunding the last unit, then what is left is refunded`() {
        // GIVEN
        val item = givenRefundListItem(
            quantity = 3F,
            total = "-9.99",
            taxes = listOf(TAX_RATE_ID to "-0.50"),
            maxQuantity = 1F,
            refundedTotal = "-6.66",
            refundedTaxes = listOf(TAX_RATE_ID to "-0.34")
        )

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("-3.33")
        assertThat(result.totalTax).isEqualByComparingTo("-0.16")
    }

    @Test
    fun `when refunding zero units, then nothing is refunded`() {
        // GIVEN
        val item = givenRefundListItem(quantity = 3F, total = "9.99", taxes = listOf(TAX_RATE_ID to "0.50"))
            .withRefundQuantity(newQuantity = 2, decimals = 2)

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 0, decimals = 2)

        // THEN
        assertThat(result.quantity).isZero()
        assertThat(result.total).isEqualByComparingTo("0")
        assertThat(result.taxes).isEmpty()
    }

    @Test
    fun `given rate taxes that round up together, when refunding all units, then line total is not exceeded`() {
        // GIVEN
        val item = givenRefundListItem(
            quantity = 1F,
            total = "1.00",
            taxes = listOf(TAX_RATE_ID to "0.005", OTHER_TAX_RATE_ID to "0.005")
        )

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.total).isEqualByComparingTo("1.01")
        assertThat(result.taxes).containsExactly(
            TaxRefund(TAX_RATE_ID, BigDecimal("0.01")),
            TaxRefund(OTHER_TAX_RATE_ID, BigDecimal("0.00"))
        )
    }

    @Test
    fun `given line tax without rate breakdown, when refunding one unit, then tax is kept in the item total`() {
        // GIVEN
        val orderItem = OrderTestUtils.generateTestOrderItems(quantity = 2F).first().copy(
            total = BigDecimal("10.00"),
            totalTax = BigDecimal("1.00"),
            taxes = emptyList()
        )
        val item = ProductRefundListItem(orderItem = orderItem, maxQuantity = 2F)

        // WHEN
        val result = item.withRefundQuantity(newQuantity = 1, decimals = 2)

        // THEN
        assertThat(result.subtotal).isEqualByComparingTo("5.00")
        assertThat(result.totalTax).isEqualByComparingTo("0.50")
        assertThat(result.taxes).isEmpty()
    }

    @Test
    fun `given scaled lines leave cents unallocated, when scaling to fit, then cents are also spread to taxes`() {
        // GIVEN
        val lines = listOf(
            givenRequestItem(id = 1, total = "10.00", tax = "0.50"),
            givenRequestItem(id = 2, total = "10.00", tax = "0.50"),
            givenRequestItem(id = 3, total = "10.00", tax = "0.50")
        )

        // WHEN
        val result = lines.scaledToFit(BigDecimal("31.49"), decimals = 2)

        // THEN
        assertThat(result.sumOf { it.total }).isEqualByComparingTo("31.49")
        result.zip(lines).forEach { (scaled, original) ->
            assertThat(scaled.refundTotal).isLessThanOrEqualTo(original.refundTotal)
            assertThat(scaled.refundTax.single().refundTotal)
                .isLessThanOrEqualTo(original.refundTax.single().refundTotal)
        }
    }

    @Test
    fun `given lines within the refund amount, when scaling to fit, then lines are unchanged`() {
        // GIVEN
        val lines = listOf(givenRequestItem(id = 1, total = "10.00", tax = "0.50"))

        // WHEN
        val result = lines.scaledToFit(BigDecimal("10.50"), decimals = 2)

        // THEN
        assertThat(result).isEqualTo(lines)
    }

    @Test
    fun `given lines above the refund amount, when scaling to fit, then lines add up to the amount`() {
        // GIVEN
        val lines = listOf(givenRequestItem(id = 1, total = "100.00"))

        // WHEN
        val result = lines.scaledToFit(BigDecimal("50.00"), decimals = 2)

        // THEN
        assertThat(result.single().total).isEqualByComparingTo("50.00")
        assertThat(result.single().quantity).isEqualTo(1)
    }

    @Test
    fun `given lines that do not split evenly, when scaling to fit, then no line exceeds its original amount`() {
        // GIVEN
        val lines = listOf(
            givenRequestItem(id = 1, total = "10.00", tax = "0.50"),
            givenRequestItem(id = 2, total = "10.00", tax = "0.50"),
            givenRequestItem(id = 3, total = "10.00", tax = "0.50")
        )

        // WHEN
        val result = lines.scaledToFit(BigDecimal("20.00"), decimals = 2)

        // THEN
        assertThat(result.sumOf { it.total }).isEqualByComparingTo("20.00")
        result.zip(lines).forEach { (scaled, original) ->
            assertThat(scaled.refundTotal).isLessThanOrEqualTo(original.refundTotal)
            assertThat(scaled.total).isLessThanOrEqualTo(original.total)
        }
    }

    private fun givenRefundListItem(
        quantity: Float,
        total: String,
        taxes: List<Pair<Long, String>>,
        maxQuantity: Float = quantity,
        refundedTotal: String = "0",
        refundedTaxes: List<Pair<Long, String>> = emptyList()
    ): ProductRefundListItem {
        val lineTaxes = taxes.map { (rateId, amount) -> Order.LineTaxEntry(rateId, BigDecimal(amount)) }
        val orderItem = OrderTestUtils.generateTestOrderItems(quantity = quantity).first().copy(
            total = BigDecimal(total),
            totalTax = lineTaxes.sumOf { it.taxAmount },
            taxes = lineTaxes
        )
        return ProductRefundListItem(
            orderItem = orderItem,
            maxQuantity = maxQuantity,
            refundedTotal = BigDecimal(refundedTotal),
            refundedTaxes = refundedTaxes.map { (rateId, amount) -> TaxRefund(rateId, BigDecimal(amount)) }
        )
    }

    private fun givenRequestItem(id: Long, total: String, tax: String? = null) = RefundRequestItem(
        itemId = id,
        quantity = 1,
        refundTotal = BigDecimal(total),
        refundTax = listOfNotNull(tax?.let { RefundRequestTax(TAX_RATE_ID, BigDecimal(it)) })
    )

    private companion object {
        const val TAX_RATE_ID = 1L
        const val OTHER_TAX_RATE_ID = 2L
    }
}
