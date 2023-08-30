package com.woocommerce.android.ui.orders.creation.taxes.rates

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TaxRateSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    fun onEditTaxRatesInAdminClicked() = Unit
    fun onInfoIconClicked() = Unit
}
