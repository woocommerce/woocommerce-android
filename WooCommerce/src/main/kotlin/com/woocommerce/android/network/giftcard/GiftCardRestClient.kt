package com.woocommerce.android.network.giftcard

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCGiftCardStats
import org.wordpress.android.fluxc.model.WCGiftCardStatsInterval
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class GiftCardRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchGiftCardSummaryByOrderId(site: SiteModel, orderId: Long): WooPayload<List<GiftCardSummaryDto>> {
        val url = WOOCOMMERCE.orders.id(orderId).pathV3
        val params = mapOf("_fields" to "gift_cards")

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = GiftCardSummaryResponse::class.java,
            params = params
        ).toWooPayload { giftCardSummaryResponse ->
            giftCardSummaryResponse.giftCards ?: emptyList()
        }
    }

    suspend fun fetchGiftCardStats(
        site: SiteModel,
        startDate: String,
        endDate: String,
        interval: String = "",
    ): WooPayload<GiftCardsStatsApiResponse> {
        val url = WOOCOMMERCE.reports.giftcards.used.stats.pathV4Analytics
        val parameters = mapOf(
            "before" to endDate,
            "after" to startDate,
            "interval" to interval
        ).filter { it.value.isNotEmpty() }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = GiftCardsStatsApiResponse::class.java,
            params = parameters
        )
        return response.toWooPayload()
    }

    data class GiftCardsStatsApiResponse(
        val totals: GiftCardsStatsTotals? = null,
        val intervals: List<GiftCardsStatsInterval>? = null
    )
    data class GiftCardsStatsTotals(
        @SerializedName("giftcards_count")
        val count: Long? = null,
        @SerializedName("net_amount")
        val netAmount: Double? = null,
    )

    data class GiftCardsStatsInterval(
        val subtotals: GiftCardsStatsSubtotal? = null
    )

    data class GiftCardsStatsSubtotal(
        @SerializedName("giftcards_count")
        val count: Long? = null,
        @SerializedName("net_amount")
        val netAmount: Double? = null,
    )

    data class GiftCardSummaryResponse(@SerializedName("gift_cards") val giftCards: List<GiftCardSummaryDto>? = null)

    data class GiftCardSummaryDto(
        val id: Long? = null,
        val code: String? = null,
        val amount: String? = null,
    )
}

fun GiftCardRestClient.GiftCardsStatsApiResponse.toWCModel(): WCGiftCardStats {
    val intervals = this.intervals?.map { interval ->
        WCGiftCardStatsInterval(
            usedValue = interval.subtotals?.count ?: 0,
            netValue = interval.subtotals?.netAmount ?: 0.0,
        )
    } ?: emptyList()
    return WCGiftCardStats(
        usedValue = this.totals?.count ?: 0,
        netValue = this.totals?.netAmount ?: 0.0,
        intervals = intervals
    )
}
