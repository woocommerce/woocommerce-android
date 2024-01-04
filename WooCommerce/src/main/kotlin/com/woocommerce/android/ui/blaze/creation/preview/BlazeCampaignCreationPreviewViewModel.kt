package com.woocommerce.android.ui.blaze.creation.preview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productDetailRepository: ProductDetailRepository
) : ScopedViewModel(savedStateHandle) {

    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()

    sealed interface CampaignPreviewState {
        object Loading : CampaignPreviewState
        data class CampaignPreview(
            val productDetails: ProductDetailsUiState,
            val title: String,
            val totalBudget: String,
            val duration: String,
            val startDate: String,
            val languages: List<String>,
            val locations: List<String>,
            val devices: List<String>,
            val interests: List<String>,
            val addUrl: String,
        ) : CampaignPreviewState
    }

    data class ProductDetailsUiState(
        val id: Long,
        val title: String,
        val price: String,
    )
}
