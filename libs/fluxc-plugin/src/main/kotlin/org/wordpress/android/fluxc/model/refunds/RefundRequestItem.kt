package org.wordpress.android.fluxc.model.refunds

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class RefundRequestItem(
    @SerializedName("id")
    val itemId: Long,
    @SerializedName("quantity")
    val quantity: Int?,
    @SerializedName("refund_total")
    val refundTotal: BigDecimal,
    @SerializedName("refund_tax")
    val refundTax: List<RefundRequestTax>
) {
    val total: BigDecimal
        get() = refundTotal + refundTax.sumOf { it.refundTotal }
}

data class RefundRequestTax(
    @SerializedName("id")
    val taxRateId: Long,
    @SerializedName("refund_total")
    val refundTotal: BigDecimal
)
