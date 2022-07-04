package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.store.WCAddonsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class AddonRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val addonsStore: WCAddonsStore,
    private val selectedSite: SelectedSite
) {
    suspend fun updateGlobalAddonsSuccessfully() =
        addonsStore.fetchAllGlobalAddonsGroups(selectedSite.get())
            .isError.not()

    suspend fun containsAddonsFrom(orderItem: Order.Item) =
        getAddonsFrom(orderItem.productId)
            ?.any { addon -> orderItem.attributesList.any { it.addonName == addon.name } }
            ?: false

    fun observeProductSpecificAddons(productRemoteID: Long): Flow<List<Addon>> =
        addonsStore.observeProductSpecificAddons(
            selectedSite.get().siteId,
            productRemoteId = productRemoteID
        )

    suspend fun hasAnyProductSpecificAddons(productRemoteID: Long): Boolean =
        observeProductSpecificAddons(productRemoteID).firstOrNull().isNullOrEmpty().not()

    suspend fun getOrderAddonsData(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = getOrder(orderID)
        ?.findOrderAttributesWith(orderItemID)
        ?.joinWithAddonsFrom(productID)

    private suspend fun getOrder(orderID: Long) =
        orderStore.getOrderByIdAndSite(orderID, selectedSite.get())

    private fun OrderEntity.findOrderAttributesWith(orderItemID: Long) =
        getLineItemList().find { it.id == orderItemID }
            ?.getAttributeList()
            ?.map { Attribute(it.key.orEmpty(), it.value.orEmpty()) }

    private suspend fun List<Attribute>.joinWithAddonsFrom(productID: Long) =
        getAddonsFrom(productID)
            ?.let { addons -> Pair(addons, this) }

    private suspend fun getAddonsFrom(productID: Long) =
        productStore.getProductByRemoteId(selectedSite.get(), productID)
            ?.let { addonsStore.observeAllAddonsForProduct(selectedSite.get().siteId, it) }
            ?.firstOrNull()
}
