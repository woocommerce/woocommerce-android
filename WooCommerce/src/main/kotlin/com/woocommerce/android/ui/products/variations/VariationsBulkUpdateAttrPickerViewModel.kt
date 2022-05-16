package com.woocommerce.android.ui.products.variations

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Responsible for calculating view state - [ProductVariation] attribute subtitles
 * which are represented by [RegularPriceState] class.
 */
class VariationsBulkUpdateAttrPickerViewModel(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val args: VariationsBulkUpdateAttrPickerDialogArgs by savedState.navArgs()

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState

    init {
        val regularPriceState = calculateRegularPriceState(args.variationsToUpdate)
        _viewState.value = viewState.value.copy(regularPriceState = regularPriceState)
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
