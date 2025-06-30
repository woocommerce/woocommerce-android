package org.wordpress.android.fluxc.network.rest.wpcom.wc.bundlestats

import com.google.gson.annotations.SerializedName

data class BundleStatsApiResponse(val totals: BundleStatsTotals)

data class BundleStatsTotals(
    @SerializedName("items_sold")
    val itemsSold: Int? = null,
    @SerializedName("net_revenue")
    val netRevenue: Double? = null,
)
