package com.woocommerce.android.ui.orders.details.editing

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@HiltViewModel
class OrderEditingSharedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {

}
