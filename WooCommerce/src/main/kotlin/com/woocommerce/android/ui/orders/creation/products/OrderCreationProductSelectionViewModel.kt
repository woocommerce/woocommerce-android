package com.woocommerce.android.ui.orders.creation.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderCreationProductSelectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedState)
