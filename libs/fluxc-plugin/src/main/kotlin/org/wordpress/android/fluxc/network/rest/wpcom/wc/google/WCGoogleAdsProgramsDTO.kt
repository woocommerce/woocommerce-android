package org.wordpress.android.fluxc.network.rest.wpcom.wc.google

import com.google.gson.annotations.SerializedName

data class WCGoogleAdsProgramsDTO(
    @SerializedName("campaigns") val campaigns: List<GoogleAdsCampaignDTO>?,
    @SerializedName("intervals") val intervals: List<GoogleAdsIntervalDTO>?,
    @SerializedName("totals") val totals: GoogleAdsTotalsDTO?,
    @SerializedName("next_page") val nextPageToken: String?
)

data class GoogleAdsCampaignDTO(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("subtotals") val subtotals: GoogleAdsTotalsDTO?
)

data class GoogleAdsIntervalDTO(
    @SerializedName("interval") val interval: String?,
    @SerializedName("subtotals") val subtotals: GoogleAdsTotalsDTO?
)

data class GoogleAdsTotalsDTO(
    @SerializedName("sales") val sales: Double?,
    @SerializedName("spend") val spend: Double?,
    @SerializedName("impressions") val impressions: Double?,
    @SerializedName("clicks") val clicks: Double?,
    @SerializedName("conversions") val conversions: Double?
)
