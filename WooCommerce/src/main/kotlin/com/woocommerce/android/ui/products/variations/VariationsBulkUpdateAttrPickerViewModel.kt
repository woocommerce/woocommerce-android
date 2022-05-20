package com.woocommerce.android.ui.products.variations

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Responsible for calculating view state - [ProductVariation] attribute subtitles
 * which are represented by [VariationsAttrsGroupType] class.
 */
class VariationsBulkUpdateAttrPickerViewModel(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val args: VariationsBulkUpdateAttrPickerDialogArgs by savedState.navArgs()

    private val _viewState = MutableStateFlow(ViewState())
    val viewState: LiveData<ViewState> = _viewState.asLiveData()

    init {
        val regularPriceState = args.variationsToUpdate.regularPriceGroupType
        _viewState.value = _viewState.value.copy(regularPriceGroupType = regularPriceState)
    }

    data class ViewState(
        val regularPriceGroupType: VariationsAttrsGroupType = VariationsAttrsGroupType.None
    )
}
