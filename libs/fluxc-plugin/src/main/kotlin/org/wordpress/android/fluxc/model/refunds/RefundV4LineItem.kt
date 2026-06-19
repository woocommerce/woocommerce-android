package org.wordpress.android.fluxc.model.refunds

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Per-line item for the simplified v4 refund endpoints (`POST /wc/v4/refunds` and
 * `POST /wc/v4/refunds/preview`).
 *
 * The client describes *what* to refund and the server computes the monetary values:
 * - Quantity-based lines (products) send [lineItemId] + [quantity]; the server derives the
 *   tax-inclusive refund total from `unit_price × quantity`.
 * - Amount-based lines (fees / shipping, which have no quantity concept in POS) send
 *   [lineItemId] + [refundTotal].
 *
 * Gson omits null fields by default, so exactly one of [quantity] / [refundTotal] is sent per line.
 */
data class RefundV4LineItem(
    @SerializedName("line_item_id")
    val lineItemId: Long,
    @SerializedName("quantity")
    val quantity: Int? = null,
    @SerializedName("refund_total")
    val refundTotal: BigDecimal? = null,
)
