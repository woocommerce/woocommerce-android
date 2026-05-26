package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pos.PosStoreApiRestClient
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

/**
 * Drives order creation through the POS Store API under the wc/pos/v1
 * namespace, instead of the legacy REST POST /wc/v3/orders path.
 *
 * The flow is: for each distinct product/variation in the in-memory cart,
 * POST cart/add-item; then POST checkout to materialise the cart into a
 * pending order. The newly-created order is then fetched via the existing
 * orders REST client so the rest of the POS payment flow sees an Order
 * with fully-populated totals.
 *
 * Coupons and custom fees are deliberately not supported in this spike;
 * the in-memory WooPosItemsViewModel.ItemClickedData types for those are
 * silently dropped. See DECISIONS.md alongside this file for scope notes.
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

        addProductsToCart(site, products)
            .onFailure { return@withContext Result.failure(it) }

        val checkoutPayload = restClient.checkout(site)
        if (checkoutPayload.isError) {
            return@withContext Result.failure(
                IllegalStateException("Store API checkout failed: ${checkoutPayload.error?.message}")
            )
        }
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
            )
            if (payload.isError) {
                return Result.failure(
                    IllegalStateException(
                        "Store API add-item failed for $id: ${payload.error?.message}"
                    )
                )
            }
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
}
