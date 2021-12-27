package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class OrderCreationProductDetailsViewModel(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
}
