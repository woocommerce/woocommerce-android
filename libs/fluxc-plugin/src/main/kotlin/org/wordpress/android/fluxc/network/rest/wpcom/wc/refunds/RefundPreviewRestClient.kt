package org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

/**
 * REST client for the v4 refund preview endpoint (`POST /wc/v4/refunds/preview`).
 *
 * The endpoint returns authoritative, server-calculated refund totals so the client does not need
 * to replicate backend tax/rounding logic. When the v4 route is not registered (feature flag off),
 * the request fails with HTTP 404 `rest_no_route`, surfaced as [WooErrorType.API_NOT_FOUND], which
 * callers use to fall back to the v3 + local-calculation flow.
 */
class RefundPreviewRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun previewRefund(
        site: SiteModel,
        orderId: Long,
        lineItems: List<RefundV4LineItem>,
    ): WooPayload<RefundPreviewResponse> {
        val body = mapOf(
            "order_id" to orderId,
            "line_items" to lineItems,
        )
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = WOOCOMMERCE.refunds.preview.pathV4,
            body = body,
            clazz = RefundPreviewResponse::class.java,
        )
        return response.toWooPayload()
    }

    data class RefundPreviewResponse(
        @SerializedName("breakdown") val breakdown: Breakdown?,
        @SerializedName("subtotal") val subtotal: String?,
        @SerializedName("tax") val tax: String?,
        @SerializedName("total") val total: String?,
        @SerializedName("max_refundable") val maxRefundable: String?,
    ) {
        data class Breakdown(
            @SerializedName("products") val products: Section?,
            @SerializedName("shipping") val shipping: Section?,
            @SerializedName("fees") val fees: Section?,
        )

        data class Section(
            @SerializedName("items") val items: List<Item>?,
            @SerializedName("subtotal") val subtotal: String?,
            @SerializedName("tax") val tax: String?,
            @SerializedName("total") val total: String?,
        )

        data class Item(
            @SerializedName("id") val id: Long,
            @SerializedName("name") val name: String?,
            @SerializedName("quantity") val quantity: Int?,
            @SerializedName("subtotal") val subtotal: String?,
            @SerializedName("tax") val tax: String?,
            @SerializedName("total") val total: String?,
            @SerializedName("product_id") val productId: Long?,
        )
    }
}
