package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Tax information for a line item, it can be part of a line item or a shipping line or a fee line.
 */
data class WCLineTaxEntry(
    @SerializedName("id")
    val rateId: Long? = null,
    @SerializedName("total")
    private val _total: String? = null,
) {
    val total: BigDecimal?
        get() = _total?.toBigDecimalOrNull()
}
