package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pos.PosStoreApiRestClient
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

/**
 * Drives order creation through the POS Store API under the wc/internal/pos/v1
 * namespace, instead of the legacy REST POST /wc/v3/orders path.
 *
 * The flow is: for each distinct product/variation in the in-memory cart,
 * POST cart/add-item; then POST cart/apply-coupon for each coupon in the
 * cart; then POST cart/add-fee for each custom amount; then POST checkout to
 * materialise the cart into a pending order. The newly-created order is then
 * fetched via the existing orders REST client so the rest of the POS payment
 * flow sees an Order with fully-populated totals.
 *
 * Session continuity across the request sequence is maintained by
 * capturing the `Cart-Token` HTTP response header emitted by the first
 * call and replaying it (as a `?cart_token=` URL parameter) on all
 * subsequent calls in the same transaction. The Store API's existing
 * header-based session swap on the server picks it up so each request
 * operates on the same server-side cart.
 *
 * Coupons are applied via cart/apply-coupon and custom amounts via
 * cart/add-fee.
 *
 * Routed behind the FeatureFlag.WOO_POS_STORE_API_CHECKOUT flag from
 * WooPosTotalsRepository.createOrderFromCartItems.
 */
class PosStoreApiCheckoutUseCase @Inject constructor(
    private val restClient: PosStoreApiRestClient,
    private val selectedSite: SelectedSite,
    private val getVariationById: WooPosGetVariationById,
    private val orderStore: WCOrderStore,
    private val orderMapper: OrderMapper,
) {
    suspend operator fun invoke(
        itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData>,
    ): Result<Order> = withContext(IO) {
        val site = selectedSite.getOrNull()
            ?: return@withContext Result.failure(IllegalStateException("No selected site"))

        val products = itemClickedDataList
            .filterIsInstance<WooPosItemsViewModel.ItemClickedData.Product>()
        val coupons = itemClickedDataList
            .filterIsInstance<WooPosItemsViewModel.ItemClickedData.Coupon>()
        val customAmounts = itemClickedDataList
            .filterIsInstance<WooPosItemsViewModel.ItemClickedData.CustomAmount>()

        // The first add-item call creates the server-side cart; its response carries
        // the Cart-Token we replay on every subsequent call so we stay on that cart.
        val cartTokenHolder = CartTokenHolder()

        addProductsToCart(site, products, cartTokenHolder)
            .onFailure { return@withContext Result.failure(it) }

        applyCoupons(site, coupons, cartTokenHolder)
            .onFailure { return@withContext Result.failure(it) }

        applyCustomFees(site, customAmounts, cartTokenHolder)
            .onFailure { return@withContext Result.failure(it) }

        val checkoutPayload = restClient.checkout(site, cartToken = cartTokenHolder.value)
        if (checkoutPayload.isError) {
            return@withContext Result.failure(
                IllegalStateException("Store API checkout failed: ${checkoutPayload.error?.message}")
            )
        }
        cartTokenHolder.updateFrom(checkoutPayload)
        val orderId = checkoutPayload.result?.orderId
            ?: return@withContext Result.failure(IllegalStateException("Store API checkout returned no order id"))

        val fetched = orderStore.fetchSingleOrderSync(site, orderId)
        if (fetched.isError) {
            return@withContext Result.failure(
                IllegalStateException("Could not fetch newly created order $orderId: ${fetched.error?.message}")
            )
        }
        val orderEntity = fetched.model
            ?: return@withContext Result.failure(IllegalStateException("Order $orderId missing after fetch"))

        Result.success(orderMapper.toAppModel(orderEntity))
    }

    private suspend fun addProductsToCart(
        site: SiteModel,
        products: List<WooPosItemsViewModel.ItemClickedData.Product>,
        cartTokenHolder: CartTokenHolder,
    ): Result<Unit> {
        // Group identical scans so we issue one /cart/add-item per distinct line.
        val quantitiesById = products.groupingBy { it.id }.eachCount()

        for ((id, quantity) in quantitiesById) {
            val item = products.first { it.id == id }
            val variationAttributes = variationAttributesFor(item)

            val payload = restClient.addToCart(
                site = site,
                productId = item.id,
                quantity = quantity,
                variation = variationAttributes,
                cartToken = cartTokenHolder.value,
            )
            if (payload.isError) {
                return Result.failure(
                    IllegalStateException(
                        "Store API add-item failed for $id: ${payload.error?.message}"
                    )
                )
            }
            cartTokenHolder.updateFrom(payload)
        }
        return Result.success(Unit)
    }

    private suspend fun applyCoupons(
        site: SiteModel,
        coupons: List<WooPosItemsViewModel.ItemClickedData.Coupon>,
        cartTokenHolder: CartTokenHolder,
    ): Result<Unit> {
        for (coupon in coupons) {
            val payload = restClient.applyCoupon(
                site = site,
                code = coupon.couponCode,
                cartToken = cartTokenHolder.value,
            )
            if (payload.isError) {
                return Result.failure(
                    IllegalStateException(
                        "Store API apply-coupon failed for ${coupon.couponCode}: ${payload.error?.message}"
                    )
                )
            }
            cartTokenHolder.updateFrom(payload)
        }
        return Result.success(Unit)
    }

    private suspend fun applyCustomFees(
        site: SiteModel,
        customAmounts: List<WooPosItemsViewModel.ItemClickedData.CustomAmount>,
        cartTokenHolder: CartTokenHolder,
    ): Result<Unit> {
        for (customAmount in customAmounts) {
            val payload = restClient.addFee(
                site = site,
                name = customAmount.name,
                amount = customAmount.amount,
                taxable = customAmount.isTaxable,
                cartToken = cartTokenHolder.value,
            )
            if (payload.isError) {
                return Result.failure(
                    IllegalStateException(
                        "Store API add-fee failed for ${customAmount.name}: ${payload.error?.message}"
                    )
                )
            }
            cartTokenHolder.updateFrom(payload)
        }
        return Result.success(Unit)
    }

    private suspend fun variationAttributesFor(
        item: WooPosItemsViewModel.ItemClickedData.Product,
    ): List<PosStoreApiRestClient.VariationAttribute> {
        if (item !is WooPosItemsViewModel.ItemClickedData.Product.Variation) return emptyList()

        val variation = getVariationById(productId = item.productId, variationId = item.id)
            ?: return emptyList()

        return variation.attributes
            .filterNot { it.name.isNullOrEmpty() || it.option.isNullOrEmpty() }
            .map { PosStoreApiRestClient.VariationAttribute(it.name!!, it.option!!) }
    }

    /**
     * Transaction-scoped holder of the Cart-Token returned by the server.
     * Captures the latest non-empty Cart-Token from each response so the
     * caller can pass it along on the next call.
     */
    private class CartTokenHolder {
        var value: String? = null
            private set

        fun updateFrom(payload: WooPayload<*>) {
            payload.headers
                .firstOrNull { it.key.equals(PosStoreApiRestClient.CART_TOKEN_HEADER, ignoreCase = true) }
                ?.value
                ?.takeIf { it.isNotBlank() }
                ?.let { value = it }
        }
    }
}
