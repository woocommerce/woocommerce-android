package com.woocommerce.android.ui.products.selector

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ProductSelectorSharedViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _selectedItems = MutableStateFlow(emptyList<ProductSelectorViewModel.SelectedItem>())
    val selectedItems: StateFlow<List<ProductSelectorViewModel.SelectedItem>> = _selectedItems

    fun updateSelectedItems(selectedItems: List<ProductSelectorViewModel.SelectedItem>) {
        _selectedItems.value = selectedItems
    }

    private val _isProductSelectionActive: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isProductSelectionActive: StateFlow<Boolean> = _isProductSelectionActive

    fun onProductSelectionStateChanged(isEnabled: Boolean) {
        _isProductSelectionActive.value = isEnabled
    }
}
