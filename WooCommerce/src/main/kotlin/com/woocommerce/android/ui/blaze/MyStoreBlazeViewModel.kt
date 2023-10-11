package com.woocommerce.android.ui.blaze

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MyStoreBlazeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _blazeCampaignState =
        savedStateHandle.getStateFlow(
            scope = viewModelScope,
            initialValue = BlazeCampaignUi(
                isVisible = true,
                hasActiveCampaigns = false,
                product = null
            )
        )
    val blazeCampaignState = _blazeCampaignState.asLiveData()

    @Parcelize
    data class BlazeCampaignUi(
        val isVisible: Boolean,
        val hasActiveCampaigns: Boolean = false,
        val product: Product? = null,
    ) : Parcelable
}
