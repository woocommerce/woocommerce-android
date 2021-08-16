package com.woocommerce.android.ui.products.addons.order

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.unwrap
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.model.ProductAddonOption
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OrderedAddonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val addonsRepository: AddonRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val addonAttributeGroupSize = 3
    }

    private val _orderedAddons = MutableLiveData<List<ProductAddon>>()
    val orderedAddonsData = _orderedAddons

    private val orderAttributesKeyRegex = "(.*?) \\((.*?)\\)".toRegex()

    fun start(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = launch(dispatchers.computation) {
        addonsRepository.fetchOrderAddonsData(orderID, orderItemID, productID)
            ?.unwrap(::mapAddonsFromOrderAttributes)
            ?.let { dispatchResult(it) }
    }

    private fun mapAddonsFromOrderAttributes(
        productAddons: List<ProductAddon>,
        orderAttributes: List<Order.Item.Attribute>
    ) = orderAttributes.mapNotNull { it.findMatchingAddon(productAddons) }

    private fun Order.Item.Attribute.findMatchingAddon(
        addons: List<ProductAddon>
    ) = addons.find { it.name == key.asAddonName }
        ?.asAddonWithSingleSelectedOption(this)

    private fun ProductAddon.asAddonWithSingleSelectedOption(
        attribute: Order.Item.Attribute
    ) = options.find { it.label == attribute.value }
        ?.let { this.copy(rawOptions = listOf(it)) }
        ?: mergeOrderAttributeWithAddon(this, attribute)

    /**
     * If it wasn't possible to find the respective option
     * through [Order.Item.Attribute.value] matching we will
     * have to merge the Addon data with the Attribute in order
     * to display the Ordered addon correctly.
     *
     * In this scenario there's no way to infer the image
     * information since it's something contained inside the options only
     */
    private fun mergeOrderAttributeWithAddon(
        addon: ProductAddon,
        attribute: Order.Item.Attribute
    ) = addon.copy(
        rawOptions = listOf(
            ProductAddonOption(
                priceType = addon.priceType,
                label = attribute.value,
                price = attribute.key.asAddonPrice,
                image = ""
            )
        )
    )

    private suspend fun dispatchResult(result: List<ProductAddon>) {
        withContext(dispatchers.main) {
            orderedAddonsData.value = result
        }
    }

    private val String.toAddonRegexGroup
        get() = orderAttributesKeyRegex
            .findAll(this)
            .first().groupValues
            .takeIf { it.size == addonAttributeGroupSize }
            ?.toMutableList()
            ?.apply { removeFirst() }

    private val String.asAddonName
        get() = toAddonRegexGroup
            ?.first()
            .orEmpty()

    private val String.asAddonPrice
        get() = toAddonRegexGroup
            ?.last()
            .orEmpty()
}
