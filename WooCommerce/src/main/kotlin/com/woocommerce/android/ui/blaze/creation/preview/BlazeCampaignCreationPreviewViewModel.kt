package com.woocommerce.android.ui.blaze.creation.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productDetailRepository: ProductDetailRepository
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableLiveData<CampaignPreviewState>(CampaignPreviewState.Loading)
    val viewState = _viewState

    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()

    init {
        launch {
            val product = productDetailRepository.getProduct(navArgs.productId)
            _viewState.value = CampaignPreviewState.CampaignPreviewContent(
                productId = product?.remoteId ?: 0,
                title = "Get the latest white shirts for a stylish look.",
                tagLine = "From $39.99",
                campaignImageUrl = "https://myer-media.com.au/wcsstore/MyerCatalogAssetStore/images/70/705/3856/100/1/797334760/797334760_1_720x928.webp",
                totalBudget = "totalBudget",
                duration = "duration",
                startDate = "startDate",
                languages = listOf("languages"),
                locations = listOf("locations"),
                devices = listOf("devices"),
                interests = listOf("interests"),
                addUrl = "addUrl",
            )
        }
    }

    sealed interface CampaignPreviewState {
        object Loading : CampaignPreviewState
        data class CampaignPreviewContent(
            val productId: Long,
            val title: String,
            val tagLine: String,
            val campaignImageUrl: String,
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
}
