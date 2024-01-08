package com.woocommerce.android.ui.products.selector

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ProductSelectorSharedViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    val selectedItemsFlow = MutableStateFlow(emptyList<ProductSelectorViewModel.SelectedItem>())

    fun updateSelectedItem(selectedItem: List<ProductSelectorViewModel.SelectedItem>) {
        selectedItemsFlow.value = selectedItem
    }
}
