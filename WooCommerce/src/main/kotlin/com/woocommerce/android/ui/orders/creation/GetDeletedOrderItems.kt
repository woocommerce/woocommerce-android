package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import javax.inject.Inject

class GetDeletedOrderItems @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
) {
    /**
     * Returns the items whose product or variation no longer exists in the store,
     * or null when that can't be confirmed for every item, e.g. because a request failed.
     */
    suspend operator fun invoke(items: List<Order.Item>): List<Order.Item>? = coroutineScope {
        val existence = items.map { item -> async { item to item.exists() } }.awaitAll()
        if (existence.any { (_, exists) -> exists == null }) {
            null
        } else {
            existence.filter { (_, exists) -> exists == false }.map { (item, _) -> item }
        }
    }

    private suspend fun Order.Item.exists(): Boolean? {
        val site = selectedSite.get()
        val error = if (isVariation) {
            productStore.fetchSingleVariation(site, productId, variationId).error
        } else {
            productStore.fetchSingleProduct(FetchSingleProductPayload(site, productId)).error
        }
        return error.toExistence()
    }

    private fun ProductError?.toExistence(): Boolean? = when {
        this == null -> true
        type == ProductErrorType.INVALID_PRODUCT_ID -> false
        else -> null
    }
}
