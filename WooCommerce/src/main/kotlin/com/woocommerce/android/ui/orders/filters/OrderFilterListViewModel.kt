package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderFilterListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val networkStatus: NetworkStatus,
) : ScopedViewModel(savedState) {
}
