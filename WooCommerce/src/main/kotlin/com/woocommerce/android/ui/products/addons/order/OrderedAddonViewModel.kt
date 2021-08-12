package com.woocommerce.android.ui.products.addons.order

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.unwrap
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias OrderAddonAttribute = Pair<String, String>

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

    private val String.toPair
        get() = orderAttributesKeyRegex
            .findAll(this)
            .first().groupValues
            .takeIf { it.size == addonAttributeGroupSize }
            ?.toMutableList()
            ?.apply { removeFirst() }
            ?.let { Pair(it.first(), it.last()) }

    fun init(orderID: Long, productID: Long) =
        launch(dispatchers.computation) {
            addonsRepository.fetchOrderAddonsData(orderID, productID)
                ?.unwrap(::filterAddonsOrderAttributes)
        }

    private fun filterAddonsOrderAttributes(
        addons: List<ProductAddon>,
        attributes: List<Order.Item.Attribute>
    ) = attributes.mapNotNull { it.key.toPair }
        .map { it.findMatchingAddon(addons) }

    private fun OrderAddonAttribute.findMatchingAddon(
        addons: List<ProductAddon>
    ) = unwrap { addonName, selectedOptionPrice ->
        addons.find {
            it.name == addonName
                && it.prices.contains(selectedOptionPrice)
        }
    }
}
