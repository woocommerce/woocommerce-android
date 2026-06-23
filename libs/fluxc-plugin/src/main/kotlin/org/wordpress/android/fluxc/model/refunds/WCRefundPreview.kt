package org.wordpress.android.fluxc.model.refunds

import java.math.BigDecimal

/**
 * Server-calculated refund preview returned by `POST /wc/v4/refunds/preview`.
 *
 * All monetary values are authoritative — they are computed by the same engine the create
 * endpoint uses, so [total] is guaranteed to match the `amount` of the resulting refund for the
 * same inputs.
 */
data class WCRefundPreview(
    val subtotal: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val maxRefundable: BigDecimal,
    val breakdown: Breakdown,
) {
    data class Breakdown(
        val products: Section,
        val shipping: Section,
        val fees: Section,
    )

    data class Section(
        val items: List<Item>,
        val subtotal: BigDecimal,
        val tax: BigDecimal,
        val total: BigDecimal,
    )

    data class Item(
        val id: Long,
        val name: String,
        val quantity: Int?,
        val subtotal: BigDecimal,
        val tax: BigDecimal,
        val total: BigDecimal,
        val productId: Long?,
    )
}
