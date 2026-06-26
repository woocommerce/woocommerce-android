package org.wordpress.android.fluxc.model.refunds

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundPreviewRestClient.RefundPreviewResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.SimplifiedRefundResponse
import java.math.BigDecimal

class RefundMapperTest {
    private val mapper = RefundMapper(Gson())

    @Test
    fun `given a v4 create response, when mapped, then line item values are stored negative`() {
        // GIVEN a combined line_items array as returned by POST /wc/v4/refunds. v4 sends positive
        // magnitudes; WCRefundItem stores them negative by contract (Refund.toAppModel negates back).
        val response = SimplifiedRefundResponse(
            refundId = 55L,
            dateCreated = null,
            amount = "22.00",
            reason = "reason",
            refundedPayment = true,
            lineItems = listOf(
                SimplifiedRefundResponse.SimplifiedLineItem(
                    id = 900L,
                    lineItemId = 12L,
                    quantity = 2,
                    refundTotal = "20.00",
                    refundTax = listOf(
                        SimplifiedRefundResponse.SimplifiedLineItemTax(taxId = 1L, refundTotal = "2.00")
                    ),
                )
            ),
        )

        // WHEN
        val model = mapper.toModel(response)

        // THEN — top-level fields plus per-line net subtotal, tax total and gross total, all negated.
        assertThat(model.id).isEqualTo(55L)
        assertThat(model.amount).isEqualByComparingTo(BigDecimal("22.00"))
        assertThat(model.automaticGatewayRefund).isTrue()
        val item = model.items.single()
        assertThat(item.itemId).isEqualTo(12L)
        assertThat(item.quantity).isEqualTo(-2)
        assertThat(item.subtotal).isEqualByComparingTo(BigDecimal("-20.00"))
        assertThat(item.totalTax).isEqualByComparingTo(BigDecimal("-2.00"))
        assertThat(item.total).isEqualByComparingTo(BigDecimal("-22.00"))
        assertThat(model.shippingLineItems).isEmpty()
        assertThat(model.feeLineItems).isEmpty()
    }

    @Test
    fun `given a real v4 create response with no tax, when mapped, then line item is negated`() {
        // GIVEN the exact payload returned by POST /wc/v4/refunds for a full single-product refund.
        val response = SimplifiedRefundResponse(
            refundId = 3266L,
            dateCreated = "2026-06-26T08:20:53",
            amount = "74.00",
            reason = "",
            refundedPayment = false,
            lineItems = listOf(
                SimplifiedRefundResponse.SimplifiedLineItem(
                    id = 1094L,
                    lineItemId = 1007L,
                    quantity = 1,
                    refundTotal = "74.00",
                    refundTax = emptyList(),
                )
            ),
        )

        // WHEN
        val model = mapper.toModel(response)

        // THEN
        assertThat(model.id).isEqualTo(3266L)
        assertThat(model.amount).isEqualByComparingTo(BigDecimal("74.00"))
        assertThat(model.automaticGatewayRefund).isFalse()
        val item = model.items.single()
        assertThat(item.itemId).isEqualTo(1007L)
        assertThat(item.quantity).isEqualTo(-1)
        assertThat(item.subtotal).isEqualByComparingTo(BigDecimal("-74.00"))
        assertThat(item.totalTax).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(item.total).isEqualByComparingTo(BigDecimal("-74.00"))
    }

    @Test
    fun `given a full preview response, when mapped, then totals and breakdown are converted`() {
        // GIVEN
        val response = fullPreviewResponse()

        // WHEN
        val model = mapper.toPreviewModel(response)

        // THEN
        assertThat(model.subtotal).isEqualByComparingTo(BigDecimal("23.00"))
        assertThat(model.tax).isEqualByComparingTo(BigDecimal("2.30"))
        assertThat(model.total).isEqualByComparingTo(BigDecimal("25.30"))
        assertThat(model.maxRefundable).isEqualByComparingTo(BigDecimal("50.00"))

        val product = model.breakdown.products.items.single()
        assertThat(product.id).isEqualTo(1L)
        assertThat(product.name).isEqualTo("Product")
        assertThat(product.quantity).isEqualTo(2)
        assertThat(product.productId).isEqualTo(10L)
        assertThat(product.total).isEqualByComparingTo(BigDecimal("22.00"))

        val shipping = model.breakdown.shipping.items.single()
        assertThat(shipping.quantity).isNull()
        assertThat(shipping.productId).isNull()
        assertThat(shipping.total).isEqualByComparingTo(BigDecimal("3.30"))

        // Empty/absent section maps to empty items and zero totals.
        assertThat(model.breakdown.fees.items).isEmpty()
        assertThat(model.breakdown.fees.total).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `given a response with null fields, when mapped, then values default to zero and empty`() {
        // GIVEN
        val response = RefundPreviewResponse(
            breakdown = null,
            subtotal = null,
            tax = null,
            total = null,
            maxRefundable = null,
        )

        // WHEN
        val model = mapper.toPreviewModel(response)

        // THEN
        assertThat(model.subtotal).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(model.tax).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(model.total).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(model.maxRefundable).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(model.breakdown.products.items).isEmpty()
        assertThat(model.breakdown.shipping.items).isEmpty()
        assertThat(model.breakdown.fees.items).isEmpty()
    }

    private fun fullPreviewResponse() = RefundPreviewResponse(
        breakdown = RefundPreviewResponse.Breakdown(
            products = RefundPreviewResponse.Section(
                items = listOf(
                    RefundPreviewResponse.Item(
                        id = 1L,
                        name = "Product",
                        quantity = 2,
                        subtotal = "20.00",
                        tax = "2.00",
                        total = "22.00",
                        productId = 10L,
                    )
                ),
                subtotal = "20.00",
                tax = "2.00",
                total = "22.00",
            ),
            shipping = RefundPreviewResponse.Section(
                items = listOf(
                    RefundPreviewResponse.Item(
                        id = 5L,
                        name = "Shipping",
                        quantity = null,
                        subtotal = "3.00",
                        tax = "0.30",
                        total = "3.30",
                        productId = null,
                    )
                ),
                subtotal = "3.00",
                tax = "0.30",
                total = "3.30",
            ),
            fees = RefundPreviewResponse.Section(items = null, subtotal = null, tax = null, total = null),
        ),
        subtotal = "23.00",
        tax = "2.30",
        total = "25.30",
        maxRefundable = "50.00",
    )
}
