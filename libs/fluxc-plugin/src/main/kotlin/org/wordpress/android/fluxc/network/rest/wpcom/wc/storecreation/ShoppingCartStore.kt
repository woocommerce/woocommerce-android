package org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation

import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.PaymentMethod
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.PaymentMethod.CREDIT_CARD_PAYMENT_METHOD
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.PaymentMethod.CREDIT_PAYMENT_METHOD
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.ShoppingCart
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.ShoppingCart.CartProduct
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingCartStore @Inject constructor(
    private val restClient: ShoppingCartRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun completePurchaseWithCredit(cart: ShoppingCart) =
        completePurchase(cart, CREDIT_PAYMENT_METHOD)

    suspend fun completePurchaseWithCreditCard(cart: ShoppingCart) =
        completePurchase(cart, CREDIT_CARD_PAYMENT_METHOD)

    suspend fun addProductToCart(
        siteId: Long,
        product: CartProduct
    ): WooResult<ShoppingCart> {
        return coroutineEngine.withDefaultContext(API, this, "addProductPlanToCart") {
            val response = restClient.addProductsToShoppingCart(
                siteId = siteId,
                products = listOf(product),
                isTemporary = false
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(response.result)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun completePurchase(
        cart: ShoppingCart,
        payment: PaymentMethod
    ): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(API, this, "completePurchase") {
            val response = restClient.completePurchase(cart, payment)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(Unit)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
