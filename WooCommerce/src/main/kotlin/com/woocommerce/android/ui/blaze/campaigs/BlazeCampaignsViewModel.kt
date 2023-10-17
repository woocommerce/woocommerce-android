package com.woocommerce.android.ui.blaze.campaigs

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel

class BlazeCampaignsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

}
