package org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.ShoppingCart.CartProduct
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ShoppingCartRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun addProductsToShoppingCart(
        siteId: Long,
        products: List<CartProduct>,
        isTemporary: Boolean
    ): WooPayload<ShoppingCart> {
        val url = WPCOMREST.me.shopping_cart.site(siteId).urlV1_1

        val body = mapOf(
            "temporary" to isTemporary,
            "products" to products
        )

        return when (val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = body,
            clazz = ShoppingCart::class.java
        )) {
            is Success -> {
                WooPayload(response.data)
            }
            is WPComGsonRequestBuilder.Response.Error -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun completePurchase(
        cart: ShoppingCart,
        paymentMethod: PaymentMethod
    ): WooPayload<ShoppingCart> {
        val url = WPCOMREST.me.transactions.urlV1_1

        val payment = mapOf(
            "payment_method" to paymentMethod.value
        )

        val body = mapOf(
            "cart" to cart,
            "payment" to payment
        )

        return when (val response = wpComGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            params = null,
            body = body,
            clazz = ShoppingCart::class.java
        )) {
            is Success -> {
                WooPayload(response.data)
            }
            is WPComGsonRequestBuilder.Response.Error -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    data class ShoppingCart(
        @SerializedName("blog_id") val blogId: Int,
        @SerializedName("cart_key") val cartKey: Int,
        val products: List<CartProduct>?
    ) : Response {
        data class CartProduct(
            @SerializedName("product_id") val productId: Int,
            val volume: Int = 1,
            val quantity: Int? = null,
            val meta: String? = null,
            val extra: Map<String, Any> = emptyMap()
        )
    }

    enum class PaymentMethod(val value: String) {
        CREDIT_PAYMENT_METHOD("WPCOM_Billing_WPCOM"),
        CREDIT_CARD_PAYMENT_METHOD("WPCOM_Billing_MoneyPress_Stored")
    }
}
