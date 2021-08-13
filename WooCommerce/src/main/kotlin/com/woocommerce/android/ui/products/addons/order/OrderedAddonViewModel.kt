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
    val orderedAddons = _orderedAddons

    private val orderAttributesKeyRegex = "(.*?) \\((.*?)\\)".toRegex()

    fun start(
        orderID: Long,
        productID: Long
    ) = launch(dispatchers.computation) {
        addonsRepository.fetchOrderAddonsData(orderID, productID)
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
        ?.toAddonWithSingleSelectedOption(this)

    private fun ProductAddon.toAddonWithSingleSelectedOption(
        attribute: Order.Item.Attribute
    ) = options.find { it.label == attribute.value }
        ?.let { copy(rawOptions = listOf(it)) }
        ?: mergeOrderAttributeWithAddon(this, attribute)

    private fun mergeOrderAttributeWithAddon(
        addon: ProductAddon,
        attribute: Order.Item.Attribute
    ) = addon.copy(
        rawOptions = listOf(
            ProductAddonOption(
                priceType = addon.priceType,
                label = attribute.value,
                price = attribute.key.asAddonPrice,
                image = addon.options
                    .firstOrNull()?.image.orEmpty()
            )
        )
    )

    private suspend fun dispatchResult(result: List<ProductAddon>) {
        withContext(dispatchers.main) {
            orderedAddons.value = result
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
