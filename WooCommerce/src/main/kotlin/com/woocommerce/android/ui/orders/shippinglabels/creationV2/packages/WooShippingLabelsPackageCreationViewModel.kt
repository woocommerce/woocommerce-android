package com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooShippingLabelsPackageCreationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState)
