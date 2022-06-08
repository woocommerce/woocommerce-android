package com.woocommerce.android.ui.orders.creation.customers

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onCustomerClick(customer: WCCustomerModel) {
        // TODO
    }
}
