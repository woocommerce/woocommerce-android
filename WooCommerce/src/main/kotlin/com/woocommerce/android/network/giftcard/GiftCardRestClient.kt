package com.woocommerce.android.network.giftcard

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.Response
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
            giftCardSummaryResponse.gift_cards ?: emptyList()
        }
    }

    data class GiftCardSummaryResponse (
        @SerializedName("gift_cards") val giftCards: List<GiftCardSummaryDto>? = null
    )

    data class GiftCardSummaryDto(
        val id: Long? = null,
        val code: String? = null,
        val amount: String? = null,
    )
}
