package com.woocommerce.android.ui.products.addons.order

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.domain.Addon
import javax.inject.Inject

@HiltViewModel
class OrderedAddonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val addonsRepository: AddonRepository,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private val _orderedAddons = MutableLiveData<List<Addon>>()
    val orderedAddonsData: LiveData<List<Addon>> = _orderedAddons

    /**
     * Provides the currencyCode for views who requires display prices
     */
    val currencyCode =
        parameterRepository
            .getParameters(KEY_PRODUCT_PARAMETERS, savedState)
            .currencyCode
            .orEmpty()

    fun start(
        orderID: Long,
        orderItemID: Long,
        productID: Long
    ) = viewState.copy(isSkeletonShown = true).let { viewState = it }.also {
        launch(dispatchers.computation) {
            takeIf { addonsRepository.updateGlobalAddonsSuccessfully() }
                ?.let { addonsRepository.getOrderAddonsData(orderID, orderItemID, productID) }
                ?.let { mapAddonsFromOrderAttributes(it.first, it.second) }
                ?.let { dispatchResult(it) }
                ?: dispatchFailure()
        }
    }

    private fun mapAddonsFromOrderAttributes(
        productAddons: List<Addon>,
        orderAttributes: List<Order.Item.Attribute>
    ): List<Addon> = orderAttributes.mapNotNull { findMatchingAddon(it, productAddons) }

    private fun findMatchingAddon(matchingTo: Order.Item.Attribute, addons: List<Addon>): Addon? = addons.firstOrNull {
        it.name == matchingTo.addonName
    }?.asAddonWithSingleSelectedOption(matchingTo)

    private fun Addon.asAddonWithSingleSelectedOption(
        attribute: Order.Item.Attribute
    ): Addon {
        return when (this) {
            is Addon.HasOptions -> options.find { it.label == attribute.value }
                ?.takeIf { (this is Addon.MultipleChoice) or (this is Addon.Checkbox) }
                ?.let { this.asSelectableAddon(it) }
                ?: mergeOrderAttributeWithAddon(this, attribute)
            else -> this
        }
    }

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
        attribute: Order.Item.Attribute
    ): Addon {
        return when (addon) {
            is Addon.Checkbox -> addon.copy(options = prepareAddonOptionBasedOnAttribute(attribute))
            is Addon.MultipleChoice -> addon.copy(options = prepareAddonOptionBasedOnAttribute(attribute))
            else -> addon
        }
    }

    private fun prepareAddonOptionBasedOnAttribute(attribute: Order.Item.Attribute) = listOf(
        Addon.HasOptions.Option(
            label = attribute.value,
            price = Addon.HasAdjustablePrice.Price.Adjusted(
                priceType = Addon.HasAdjustablePrice.Price.Adjusted.PriceType.FlatFee,
                value = attribute.asAddonPrice
            ),
            image = ""
        )
    )

    private suspend fun dispatchResult(result: List<Addon>) {
        withContext(dispatchers.main) {
            viewState = viewState.copy(isSkeletonShown = false)
            _orderedAddons.value = result
        }
    }

    private suspend fun dispatchFailure() {
        withContext(dispatchers.main) {
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null
    ) : Parcelable
}
