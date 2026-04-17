package com.woocommerce.android.ui.woopos.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPosOrdersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel()
