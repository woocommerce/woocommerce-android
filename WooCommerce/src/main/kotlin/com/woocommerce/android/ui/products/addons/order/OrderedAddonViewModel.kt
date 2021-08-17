package com.woocommerce.android.ui.products.addons.order

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
    private val _orderedAddons = MutableLiveData<List<ProductAddon>>()
    val orderedAddonsData: LiveData<List<ProductAddon>> = _orderedAddons

    fun start(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = launch(dispatchers.computation) {
        addonsRepository.getOrderAddonsData(orderID, orderItemID, productID)
            ?.let { mapAddonsFromOrderAttributes(it.first, it.second) }
            ?.let { dispatchResult(it) }
    }

    private fun mapAddonsFromOrderAttributes(
        productAddons: List<ProductAddon>,
        orderAttributes: List<Order.Item.Attribute>
    ) = orderAttributes.mapNotNull { it.findMatchingAddon(productAddons) }

    private fun Order.Item.Attribute.findMatchingAddon(
        addons: List<ProductAddon>
    ) = addons.find { it.name == addonName }
        ?.asAddonWithSingleSelectedOption(this)

    private fun ProductAddon.asAddonWithSingleSelectedOption(
        attribute: Order.Item.Attribute
    ) = options.find { it.label == attribute.value }
        ?.let { this.copy(rawOptions = listOf(it)) }
        ?: mergeOrderAttributeWithAddon(this, attribute)

    /**
     * If it isn't possible to find the respective option
     * through [Order.Item.Attribute.value] matching we will
     * have to merge the [ProductAddon] data with the Attribute in order
     * to display the Ordered addon correctly, which is exactly
     * what this method does.
     *
     * Also, in this scenario there's no way to infer the image
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
                price = attribute.asAddonPrice,
                image = ""
            )
        )
    )

    private suspend fun dispatchResult(result: List<ProductAddon>) {
        withContext(dispatchers.main) {
            _orderedAddons.value = result
        }
    }
}
