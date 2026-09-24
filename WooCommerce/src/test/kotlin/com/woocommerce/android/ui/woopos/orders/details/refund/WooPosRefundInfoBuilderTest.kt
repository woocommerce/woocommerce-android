package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

class WooPosRefundInfoBuilderTest {
    private val formatPrice: WooPosFormatPrice = mock {
        on { invoke(any(), any()) }.thenReturn("$0.00")
    }
    private val sut = WooPosRefundInfoBuilder(mock(), formatPrice)
    private val refundInfo = RefundInfo(emptyList(), BigDecimal.ZERO)

    @Test
    fun `given order has fee lines, when totals breakdown is built, then custom amounts is formatted fees total`() {
        // GIVEN
        val order = Order.getEmptyOrder(Date(0), Date(0)).copy(
            currency = "USD",
            feesLines = listOf(Order.FeeLine.EMPTY.copy(total = BigDecimal("12.50")))
        )
        whenever(formatPrice(BigDecimal("12.50"), "USD")).thenReturn("$12.50")

        // WHEN
        val result = sut.buildTotalsBreakdown(order, refundInfo)

        // THEN
        assertThat(result.customAmounts).isEqualTo("$12.50")
    }

    @Test
    fun `given order has no fee lines, when totals breakdown is built, then custom amounts is null`() {
        // GIVEN
        val order = Order.getEmptyOrder(Date(0), Date(0)).copy(currency = "USD")

        // WHEN
        val result = sut.buildTotalsBreakdown(order, refundInfo)

        // THEN
        assertThat(result.customAmounts).isNull()
    }
}
