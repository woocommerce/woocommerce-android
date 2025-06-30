package org.wordpress.android.fluxc.model.shippinglabels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.shippinglabels.WCContentType.Other
import java.math.BigDecimal

data class WCShippingPackageCustoms(
    @Expose(serialize = false) val id: String,
    @SerializedName("contents_type") val contentsType: WCContentType?,
    @SerializedName("contents_explanation") val contentsExplanation: String? = null,
    @SerializedName("restriction_type") val restrictionType: WCRestrictionType?,
    @SerializedName("restriction_comments") val restrictionComments: String? = null,
    @SerializedName("non_delivery_option") val nonDeliveryOption: WCNonDeliveryOption?,
    @SerializedName("itn") val itn: String?,
    @SerializedName("items") val customsItems: List<WCCustomsItem>?
) {
    init {
        require(!(contentsType == Other && contentsExplanation.isNullOrEmpty())) {
            "you have to specify contentsExplanation when the contentsType is Other"
        }
        require(!(restrictionType == WCRestrictionType.Other && restrictionComments.isNullOrEmpty())) {
            "you have to specify restrictionComments when the restrictionType is Other"
        }
    }
}

data class WCCustomsItem(
    @SerializedName("product_id") val productId: Long,
    val description: String,
    val quantity: Int,
    val value: BigDecimal,
    val weight: Float,
    @SerializedName("hs_tariff_number") val hsTariffNumber: String?,
    @SerializedName("origin_country") val originCountry: String
)

enum class WCContentType {
    @SerializedName("merchandise")
    Merchandise,
    @SerializedName("documents")
    Documents,
    @SerializedName("gift")
    Gift,
    @SerializedName("sample")
    Sample,
    @SerializedName("other")
    Other
}

enum class WCRestrictionType {
    @SerializedName("none")
    None,
    @SerializedName("quarantine")
    Quarantine,
    @SerializedName("sanitary_phytosanitary_inspection")
    SanitaryInspection,
    @SerializedName("other")
    Other
}

enum class WCNonDeliveryOption {
    @SerializedName("abandon")
    Abandon,
    @SerializedName("return")
    Return
}
