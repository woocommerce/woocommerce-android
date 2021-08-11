package com.woocommerce.android.ui.products.addons.options

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.unwrap
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderedAddonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val addonsRepository: AddonRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val regexGroupMatchSize = 3
    }

    private val orderAttributesKeyRegex = "(.*?) \\((.*?)\\)".toRegex()

    private val String.asParsedPair
        get() = orderAttributesKeyRegex
            .findAll(this)
            .first().groupValues
            .takeIf { it.size == regexGroupMatchSize }
            ?.toMutableList()
            ?.apply { removeFirst() }
            ?.let { Pair(it.first(), it.last()) }

    fun mergeDataFromOrderedAddons(order: Order, productID: Long) =
        addonsRepository.fetchOrderedAddonsData(order, productID)
            ?.unwrap { addons, attributes ->
                attributes.mapNotNull { it.key.asParsedPair }
                    .map { pair -> addons.find { it.name == pair.first } }
            }
}
