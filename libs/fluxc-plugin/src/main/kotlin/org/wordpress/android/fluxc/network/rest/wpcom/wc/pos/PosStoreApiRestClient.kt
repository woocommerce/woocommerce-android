package org.wordpress.android.fluxc.network.rest.wpcom.wc.pos

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REST client for the POS Store API namespace (`wc/pos/v1`).
 *
 * These endpoints run the WooCommerce checkout pipeline (and therefore fire
 * the checkout-time extension hooks that fulfilment of gift cards,
 * subscriptions, bookings and downloadable products relies on), with
 * POS-specific session scoping. See
 * `plugins/woocommerce/src/Internal/POS/StoreApi/` in woocommerce/woocommerce
 * for the server side.
 *
 * This is a spike client paired with the server-side architectural spike.
 * Session continuity across requests is currently unresolved (see
 * DECISIONS.md alongside the use case); tests mock this client, so unit
 * coverage is unaffected.
 */
@Singleton
class PosStoreApiRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {

    /**
     * POST /wc/pos/v1/cart/add-item.
     *
     * Adds one cart line. Currently called once per distinct product/quantity
     * pair built by the in-memory POS cart.
     */
    suspend fun addToCart(
        site: SiteModel,
        productId: Long,
        quantity: Int,
        variation: List<VariationAttribute> = emptyList(),
    ): WooPayload<Unit> {
        val body = buildMap<String, Any> {
            put("id", productId)
            put("quantity", quantity)
            if (variation.isNotEmpty()) {
                put("variation", variation.map { it.toMap() })
            }
        }

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = ADD_ITEM_PATH,
            clazz = AddItemResponseDto::class.java,
            body = body
        )

        return when (response) {
            is Success -> WooPayload(Unit)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * POST /wc/pos/v1/checkout.
     *
     * Finalises the in-progress cart into an order. No `payment_method` is
     * supplied — the Store API creates the order in `pending` status and
     * does not attempt to process payment, leaving the existing POS
     * payment flow to take over (WooPayments capture, or cash mark-paid).
     */
    suspend fun checkout(site: SiteModel): WooPayload<CheckoutResponseDto> {
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = CHECKOUT_PATH,
            clazz = CheckoutResponseDto::class.java,
            body = emptyMap()
        )

        return when (response) {
            is Success -> WooPayload(response.data)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    data class VariationAttribute(
        val attribute: String,
        val value: String,
    ) {
        fun toMap(): Map<String, String> = mapOf(
            "attribute" to attribute,
            "value" to value,
        )
    }

    /**
     * Minimal projection of the add-item response. We don't consume the cart
     * payload itself here — the in-memory POS cart remains the source of
     * truth client-side until checkout — so an empty class deserialises fine.
     */
    @Suppress("unused")
    private class AddItemResponseDto

    /**
     * Minimal projection of the checkout response. The full schema is much
     * larger; we only need the order identification fields to hand off to
     * the existing payment flow.
     */
    data class CheckoutResponseDto(
        @SerializedName("order_id") val orderId: Long,
        @SerializedName("status") val status: String?,
        @SerializedName("order_key") val orderKey: String?,
    )

    companion object {
        private const val ADD_ITEM_PATH = "/wc/pos/v1/cart/add-item"
        private const val CHECKOUT_PATH = "/wc/pos/v1/checkout"
    }
}
