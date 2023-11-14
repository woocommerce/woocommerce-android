package com.woocommerce.android.ui.products.variations

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GenerateVariationBottomSheetViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onGenerateAllVariationsClicked() {
        triggerEvent(GenerateAllVariations)
    }

    fun onAddNewVariationClicked() {
        triggerEvent(AddNewVariation)
    }

    object AddNewVariation : Event()
    object GenerateAllVariations : Event()
}
