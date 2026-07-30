package org.wordpress.android.fluxc.model.refunds

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Locks the JSON wire format of the refund line item payload models: the preview endpoint keys
 * lines by `line_item_id`, the computed create endpoint keys them by `id`, and each line carries
 * exactly one of `quantity` / `refund_total`.
 */
class RefundLineItemSerializationTest {
    private val gson = Gson()

    @Test
    fun `given a quantity-based preview line, when serialized, then only line_item_id and quantity are sent`() {
        val json = gson.toJson(RefundPreviewLineItem.quantityBased(lineItemId = 7L, quantity = 2))

        assertThat(json).isEqualTo("""{"line_item_id":7,"quantity":2}""")
    }

    @Test
    fun `given an amount-based preview line, when serialized, then only line_item_id and refund_total are sent`() {
        val json = gson.toJson(
            RefundPreviewLineItem.amountBased(lineItemId = 7L, refundTotal = "5.50".toBigDecimal())
        )

        assertThat(json).isEqualTo("""{"line_item_id":7,"refund_total":5.50}""")
    }

    @Test
    fun `given a quantity-based computed line, when serialized, then only id and quantity are sent`() {
        val json = gson.toJson(ComputedRefundLineItem.quantityBased(lineItemId = 7L, quantity = 2))

        assertThat(json).isEqualTo("""{"id":7,"quantity":2}""")
    }

    @Test
    fun `given an amount-based computed line, when serialized, then only id and refund_total are sent`() {
        val json = gson.toJson(
            ComputedRefundLineItem.amountBased(lineItemId = 7L, refundTotal = "5.50".toBigDecimal())
        )

        assertThat(json).isEqualTo("""{"id":7,"refund_total":5.50}""")
    }
}
