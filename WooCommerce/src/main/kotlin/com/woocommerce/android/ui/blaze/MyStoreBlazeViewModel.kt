package com.woocommerce.android.ui.blaze

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.util.FeatureFlag
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
                isVisible = FeatureFlag.BLAZE_ITERATION_2.isEnabled(),
                hasActiveCampaigns = false,
                product = BlazeProduct(
                    name = "Product name",
                    imgUrl = "https://proxied.site/wp-content/uploads/2023/08/product-1693384773-2061315749.png",
                )
            )
        )
    val blazeCampaignState = _blazeCampaignState.asLiveData()

    @Parcelize
    data class BlazeCampaignUi(
        val isVisible: Boolean,
        val hasActiveCampaigns: Boolean = false,
        val product: BlazeProduct
    ) : Parcelable

    @Parcelize
    data class BlazeProduct(
        val name: String,
        val imgUrl: String
    ) : Parcelable
}
