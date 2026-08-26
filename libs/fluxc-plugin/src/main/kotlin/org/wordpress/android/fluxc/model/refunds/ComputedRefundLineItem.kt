package org.wordpress.android.fluxc.model.refunds

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Per-line item for the computed refund create endpoint (`POST /wc/v3/orders/<order_id>/refunds`
 * with `compute_totals=true`).
 *
 * Same exclusive quantity/amount shape as [RefundPreviewLineItem], but the create endpoint keys
 * each line by `id` (the order line item id) instead of the preview's `line_item_id`:
 * - Quantity-based lines (products) send [lineItemId] + [quantity]; the server derives the
 *   tax-inclusive refund total from `unit_price × quantity`.
 * - Amount-based lines (fees / shipping, which have no quantity concept in POS) send
 *   [lineItemId] + [refundTotal].
 *
 * Gson omits null fields by default, so exactly one of [quantity] / [refundTotal] is sent per line.
 * The constructor is private; use [quantityBased] / [amountBased] so an invalid combination
 * (both set, or neither) can never reach the API. [ConsistentCopyVisibility] makes the generated
 * [copy] private too, so it can't bypass the factories and produce an invalid payload.
 */
@ConsistentCopyVisibility
data class ComputedRefundLineItem private constructor(
    @SerializedName("id")
    val lineItemId: Long,
    @SerializedName("quantity")
    val quantity: Int? = null,
    @SerializedName("refund_total")
    val refundTotal: BigDecimal? = null,
) {
    companion object {
        /** Quantity-based line (products): the server derives the refund total from the quantity. */
        fun quantityBased(lineItemId: Long, quantity: Int) =
            ComputedRefundLineItem(lineItemId = lineItemId, quantity = quantity)

        /** Amount-based line (fees / shipping): the client specifies the exact total to refund. */
        fun amountBased(lineItemId: Long, refundTotal: BigDecimal) =
            ComputedRefundLineItem(lineItemId = lineItemId, refundTotal = refundTotal)
    }
}
