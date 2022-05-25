package com.woocommerce.android.ui.products.categories.selector

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProductCategorySelectorViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
}
