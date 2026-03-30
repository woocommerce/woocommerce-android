package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderattribution

import com.google.gson.annotations.SerializedName

data class ChannelSummaryResponse(
    val data: List<ChannelSummaryItemApiResponse>? = null
)

data class ChannelSummaryItemApiResponse(
    val item: String? = null,
    @SerializedName("current_period") val currentPeriod: PeriodData? = null,
    @SerializedName("previous_period") val previousPeriod: PeriodData? = null
)

data class PeriodData(
    val value: String? = null
)

/**
 * Flattened model used by the domain layer.
 * Maps from the nested [ChannelSummaryItemApiResponse] structure.
 */
data class ChannelSummaryApiResponse(
    val channelName: String? = null,
    val netRevenue: Double? = null,
    val compareNetRevenue: Double? = null,
    val ordersCount: Int? = null,
    val compareOrdersCount: Int? = null
) {
    companion object {
        fun fromApiItem(item: ChannelSummaryItemApiResponse): ChannelSummaryApiResponse {
            return ChannelSummaryApiResponse(
                channelName = item.item,
                netRevenue = item.currentPeriod?.value?.toDoubleOrNull(),
                compareNetRevenue = item.previousPeriod?.value?.toDoubleOrNull(),
                ordersCount = null,
                compareOrdersCount = null
            )
        }
    }
}
