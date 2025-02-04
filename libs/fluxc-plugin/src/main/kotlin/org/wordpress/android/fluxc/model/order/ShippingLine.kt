package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.utils.NullStringJsonAdapter

data class ShippingLine(
    val id: Long? = null,
    val total: String? = null,
    @SerializedName("total_tax")
    val totalTax: String? = null,
    @SerializedName("method_id")
    @JsonAdapter(NullStringJsonAdapter::class, nullSafe = false)
    val methodId: String? = null,
    @SerializedName("method_title")
    val methodTitle: String? = null
)
