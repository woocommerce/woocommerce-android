package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

/**
 * Represents a tax line
 */
class TaxLine {
    @SerializedName("id")
    val id: Long? = null

    @SerializedName("rate_id")
    val rateId: Long? = null

    @SerializedName("rate_code")
    val rateCode: String? = null

    @SerializedName("rate_percent")
    val ratePercent: Float? = null

    @SerializedName("label")
    val label: String? = null

    @SerializedName("compound")
    val compound: Boolean? = null

    @SerializedName("tax_total")
    val taxTotal: String? = null

    @SerializedName("shipping_tax_total")
    val shippingTaxTotal: String? = null
}
