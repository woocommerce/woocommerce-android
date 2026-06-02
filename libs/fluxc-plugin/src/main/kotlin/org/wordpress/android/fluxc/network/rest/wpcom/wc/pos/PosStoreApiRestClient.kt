package org.wordpress.android.fluxc.network.rest.wpcom.wc.pos

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import java.net.URLEncoder
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
 * **Session continuity.** Each call optionally accepts a `cartToken` returned
 * by a prior call's `Cart-Token` response header. Mobile passes the token
 * back via a `?cart_token=` URL parameter (mirroring the URL-parameter
 * transport agentic commerce uses for `checkout_session_id`); the server
 * route validates it and injects it as the `HTTP_CART_TOKEN` env so the
 * Store API's existing header-based session swap picks it up. The URL-
 * parameter transport is used because `WooNetwork` does not currently
 * expose a way to add custom request headers per call.
 */
@Singleton
class PosStoreApiRestClient @Inject constructor(
    private val wooNetwork: WooNetwork
) {

    /**
     * POST /wc/pos/v1/cart/add-item.
     *
     * Adds one cart line. Returns the response together with any
     * `Cart-Token` header the server emitted, so the caller can replay
     * it on subsequent calls in the same transaction.
     */
    suspend fun addToCart(
        site: SiteModel,
        productId: Long,
        quantity: Int,
        variation: List<VariationAttribute> = emptyList(),
        cartToken: String? = null,
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
            path = appendCartToken(ADD_ITEM_PATH, cartToken),
            clazz = AddItemResponseDto::class.java,
            body = body
        )

        return when (response) {
            is Success -> WooPayload(Unit, response.headers)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * POST /wc/pos/v1/cart/apply-coupon.
     *
     * Applies a single coupon to the in-progress cart. Coupon validation
     * (usage limits, per-customer limits, product restrictions, etc.) runs
     * server-side; an invalid code comes back as an error payload. Returns
     * the response together with any refreshed `Cart-Token` header so the
     * caller can replay it on subsequent calls in the same transaction.
     */
    suspend fun applyCoupon(
        site: SiteModel,
        code: String,
        cartToken: String? = null,
    ): WooPayload<Unit> {
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = appendCartToken(APPLY_COUPON_PATH, cartToken),
            clazz = ApplyCouponResponseDto::class.java,
            body = mapOf("code" to code)
        )

        return when (response) {
            is Success -> WooPayload(Unit, response.headers)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * POST /wc/pos/v1/checkout.
     *
     * Finalises the in-progress cart into an order. The body is empty:
     * no payment_method, no billing_address, no shipping_address. The POS
     * route on the server opts out of all three Store API guards that
     * would otherwise reject this for web checkout. The order is created
     * in `pending` status, and the existing POS payment flow takes over
     * (WooPayments terminal capture for cards, cash mark-paid endpoint).
     *
     * For product types that genuinely need address/email data (gift
     * cards delivered by email, downloadables, shipped goods sold for
     * delivery), the cashier will capture those fields and the request
     * shape will grow to carry them. Today the API accepts what's sent
     * and the order can be edited later via admin if needed.
     */
    suspend fun checkout(
        site: SiteModel,
        cartToken: String? = null,
    ): WooPayload<CheckoutResponseDto> {
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = appendCartToken(CHECKOUT_PATH, cartToken),
            clazz = CheckoutResponseDto::class.java,
            body = emptyMap()
        )

        return when (response) {
            is Success -> WooPayload(response.data, response.headers)
            is Error -> WooPayload(response.error.toWooError())
        }
    }

    private fun appendCartToken(path: String, cartToken: String?): String {
        if (cartToken.isNullOrEmpty()) return path
        val separator = if (path.contains('?')) "&" else "?"
        return path + separator + CART_TOKEN_PARAM + "=" + URLEncoder.encode(cartToken, Charsets.UTF_8.name())
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
     * Minimal projection of the apply-coupon response. As with add-item, the
     * in-memory POS cart stays the client-side source of truth, so we don't
     * consume the returned cart payload here.
     */
    @Suppress("unused")
    private class ApplyCouponResponseDto

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
        /**
         * Name of the response header the Store API emits carrying the
         * server-signed cart-token JWT. Callers should look this up
         * case-insensitively in [WooPayload.headers] and replay the value
         * on subsequent calls in the same transaction.
         */
        const val CART_TOKEN_HEADER = "Cart-Token"

        private const val ADD_ITEM_PATH = "/wc/pos/v1/cart/add-item"
        private const val APPLY_COUPON_PATH = "/wc/pos/v1/cart/apply-coupon"
        private const val CHECKOUT_PATH = "/wc/pos/v1/checkout"
        private const val CART_TOKEN_PARAM = "cart_token"
    }
}
