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
            selectedSite.get(),
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
            ?.let { addonsStore.observeAllAddonsForProduct(selectedSite.get(), it) }
            ?.firstOrNull()

    suspend fun loadItemAddons(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ): List<Addon>? = getOrderAddonsData(orderID, orderItemID, productID)
        ?.let { mapAddonsFromOrderAttributes(it.first, it.second) }

    private fun mapAddonsFromOrderAttributes(
        productAddons: List<Addon>,
        orderAttributes: List<Attribute>
    ): List<Addon> = orderAttributes.mapNotNull { findMatchingAddon(it, productAddons) }

    private fun findMatchingAddon(matchingTo: Attribute, addons: List<Addon>): Addon? =
        addons.firstOrNull { it.name == matchingTo.addonName }
            ?.asAddonWithSingleSelectedOption(matchingTo)

    private fun Addon.asAddonWithSingleSelectedOption(
        attribute: Attribute
    ): Addon {
        return when (this) {
            is Addon.HasOptions -> options.find { it.label == attribute.value }
                ?.takeIf { (this is Addon.MultipleChoice) or (this is Addon.Checkbox) }
                ?.handleOptionPriceType(attribute)
                ?.let { this.asSelectableAddon(it) }
                ?: mergeOrderAttributeWithAddon(this, attribute)

            else -> this
        }
    }

    /**
     * When displaying the price of an Ordered addon with the PercentageBased price
     * we don't want to display the percentage itself, but the price applied through the percentage.
     *
     * In this method we verify if that's the scenario and replace the percentage value with the price
     * defined by the Order Attribute, if it's not the case, the Addon is returned untouched.
     */
    private fun Addon.HasOptions.Option.handleOptionPriceType(
        attribute: Attribute
    ) =
        takeIf { it.price.priceType == Addon.HasAdjustablePrice.Price.Adjusted.PriceType.PercentageBased }
            ?.copy(
                price = Addon.HasAdjustablePrice.Price.Adjusted(
                    Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee,
                    attribute.asAddonPrice
                )
            )
            ?: this

    private fun Addon.asSelectableAddon(selectedOption: Addon.HasOptions.Option): Addon? =
        when (this) {
            is Addon.Checkbox -> this.copy(options = listOf(selectedOption))
            is Addon.MultipleChoice -> this.copy(options = listOf(selectedOption))
            else -> null
        }

    /**
     * If it isn't possible to find the respective option
     * through [Order.Item.Attribute.value] matching we will
     * have to merge the [Addon] data with the Attribute in order
     * to display the Ordered addon correctly, which is exactly
     * what this method does.
     *
     * Also, in this scenario there's no way to infer the image
     * information since it's something contained inside the options only
     */
    private fun mergeOrderAttributeWithAddon(
        addon: Addon,
        attribute: Attribute
    ): Addon {
        return when (addon) {
            is Addon.Checkbox -> addon.copy(options = prepareAddonOptionBasedOnAttribute(attribute))
            is Addon.MultipleChoice -> addon.copy(
                options = prepareAddonOptionBasedOnAttribute(
                    attribute
                )
            )

            else -> addon
        }
    }

    private fun prepareAddonOptionBasedOnAttribute(attribute: Attribute) = listOf(
        Addon.HasOptions.Option(
            label = attribute.value,
            price = Addon.HasAdjustablePrice.Price.Adjusted(
                priceType = Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee,
                value = attribute.asAddonPrice
            ),
            image = ""
        )
    )
}
