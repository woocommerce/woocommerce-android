package com.woocommerce.android.ui.orders.creation.fees

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderCreationAddFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
}
