package org.wordpress.android.fluxc.network.rest.wpcom.wc.bundlestats

import com.google.gson.annotations.SerializedName

data class BundlesReportApiResponse(
    @SerializedName("items_sold")
    val itemsSold: Int? = null,
    @SerializedName("net_revenue")
    val netRevenue: Double? = null,
    @SerializedName("extended_info")
    val extendedInfo: ExtendedInfo
)

data class ExtendedInfo(
    val name: String? = null,
    val image: String? = null,
)
