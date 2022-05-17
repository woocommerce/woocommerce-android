package com.woocommerce.android.ui.products.variations

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdateAttrPickerViewModel.RegularPriceState
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Responsible for calculating view state - [ProductVariation] attribute subtitles
 * which are represented by [RegularPriceState] class.
 */
class VariationsBulkUpdateAttrPickerViewModel(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val args: VariationsBulkUpdateAttrPickerDialogArgs by savedState.navArgs()

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: LiveData<ViewState> = _viewState.asLiveData()

    init {
        val regularPriceState = calculateRegularPriceState(args.variationsToUpdate)
        _viewState.value = _viewState.value.copy(regularPriceState = regularPriceState)
    }

    private fun calculateRegularPriceState(variations: Array<ProductVariation>): RegularPriceState {
        val prices = variations.map { it.regularPrice }
        return when {
            prices.isEmpty() -> RegularPriceState.None
            prices.distinct().size == 1 ->
                RegularPriceState.Value(variations.map { it.priceWithCurrency }.first() ?: "")
            else -> RegularPriceState.Mixed
        }
    }

    data class ViewState(
        val regularPriceState: RegularPriceState = RegularPriceState.None
    )

    sealed class RegularPriceState {
        object None : RegularPriceState()
        object Mixed : RegularPriceState()
        data class Value(val price: String) : RegularPriceState()
    }
}
