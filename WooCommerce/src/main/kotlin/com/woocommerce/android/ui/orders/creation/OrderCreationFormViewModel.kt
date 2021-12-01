package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderCreationFormViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {

}
