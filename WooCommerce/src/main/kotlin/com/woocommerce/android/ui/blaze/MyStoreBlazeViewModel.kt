package com.woocommerce.android.ui.blaze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyStoreBlazeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _blazeCampaignState =
        savedStateHandle.getStateFlow(
            scope = viewModelScope, initialValue = BlazeCampaignUi(isVisible = false)
        )
    val blazeCampaignState = _blazeCampaignState.asLiveData()

    data class BlazeCampaignUi(
        val isVisible: Boolean
    )
}
