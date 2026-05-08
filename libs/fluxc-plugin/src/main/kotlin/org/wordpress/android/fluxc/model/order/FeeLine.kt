package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.utils.NullJsonAdapter

/**
 * Represents a fee line.
 */
class FeeLine {
    @SerializedName("id")
    var id: Long? = null

    @SerializedName("name")
    @JsonAdapter(NullJsonAdapter::class, nullSafe = false)
    var name: String? = null

    @SerializedName("total")
    var total: String? = null

    @SerializedName("total_tax")
    var totalTax: String? = null

    @SerializedName("tax_status")
    var taxStatus: FeeLineTaxStatus? = null

    @SerializedName("taxes")
    var taxes: List<WCLineTaxEntry>? = null
}

enum class FeeLineTaxStatus(val value: String) {
    @SerializedName("taxable")
    Taxable("taxable"),
    @SerializedName("none")
    None("none")
}
