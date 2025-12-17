package org.wordpress.android.fluxc.model.refunds

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class RefundRequestItem(
    @SerializedName("id")
    val itemId: Long,
    @SerializedName("quantity")
    val quantity: Int?,
    @SerializedName("refund_total")
    val refundTotal: BigDecimal? = null,
    @SerializedName("refund_tax")
    val refundTax: List<RefundRequestTax>? = null
) {
    val total: BigDecimal
        get() = (refundTotal ?: BigDecimal.ZERO) + (refundTax?.sumOf { it.refundTotal } ?: BigDecimal.ZERO)
}

data class RefundRequestTax(
    @SerializedName("id")
    val taxRateId: Long,
    @SerializedName("refund_total")
    val refundTotal: BigDecimal
)
