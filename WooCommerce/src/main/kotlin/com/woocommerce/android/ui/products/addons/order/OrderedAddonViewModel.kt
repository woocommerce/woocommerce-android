package com.woocommerce.android.ui.products.addons.order

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.unwrap
import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.model.ProductAddonOption
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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

    private val orderAttributesKeyRegex = "(.*?) \\((.*?)\\)".toRegex()

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

    fun start(
        orderID: Long,
        productID: Long
    ) = launch(dispatchers.computation) {
        addonsRepository.fetchOrderAddonsData(orderID, productID)
            ?.unwrap(::filterAddonsOrderAttributes)
    }

    private fun filterAddonsOrderAttributes(
        productAddons: List<ProductAddon>,
        orderAttributes: List<Attribute>
    ) = orderAttributes.mapNotNull { it.findMatchingAddon(productAddons) }

    private fun Attribute.findMatchingAddon(
        addons: List<ProductAddon>
    ) = addons.find { it.name == key.asAddonName }
        ?.asAddonWithSelectedOption(this)

    private fun ProductAddon.asAddonWithSelectedOption(
        attribute: Attribute
    ) = options.find { it.label == attribute.value }
            ?.let { copy(rawOptions = listOf(it)) }
            ?: mergeAttributeWithAddon(this, attribute)

    private fun mergeAttributeWithAddon(
        addon: ProductAddon,
        attribute: Attribute
    ) = addon.copy(
        rawOptions = listOf(
            ProductAddonOption(
                priceType = addon.priceType,
                label = attribute.value,
                price = attribute.key.asAddonPrice,
                image = addon.options.first().image
            )
        )
    )
}
