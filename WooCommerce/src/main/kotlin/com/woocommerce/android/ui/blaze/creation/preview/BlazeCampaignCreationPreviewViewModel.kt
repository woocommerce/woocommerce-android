package com.woocommerce.android.ui.blaze.creation.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignPreviewUiState.CampaignDetailItem
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productDetailRepository: ProductDetailRepository,
    resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableLiveData<CampaignPreviewUiState>(CampaignPreviewUiState.Loading)
    val viewState = _viewState

    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()

    init {
        launch {
            @Suppress("MagicNumber")
            delay(5000)
            val product = productDetailRepository.getProduct(navArgs.productId)
            _viewState.value = CampaignPreviewUiState.CampaignPreviewContent(
                productId = product?.remoteId ?: -1,
                title = "Get the latest white t-shirts",
                tagLine = "From 45.00 USD",
                campaignImageUrl = "https://rb.gy/gmjuwb",
                budget = CampaignDetailItem(
                    displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_budget),
                    displayValue = "140 USD, 7 days from Jan 14",
                ),
                audienceDetails = listOf(
                    CampaignDetailItem(
                        displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_language),
                        displayValue = "English, Spanish",
                    ),
                    CampaignDetailItem(
                        displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_devices),
                        displayValue = "USA, Poland, Japan",
                    ),
                    CampaignDetailItem(
                        displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_location),
                        displayValue = "Samsung, Apple, Xiaomi",
                    ),
                    CampaignDetailItem(
                        displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_interests),
                        displayValue = "Fashion, Clothing, T-shirts",
                    ),
                ),
                destinationUrl = CampaignDetailItem(
                    displayTitle = "Destination URL",
                    displayValue = "https://www.myer.com.au/p/white-t-shirt-797334760-797334760",
                    maxLinesValue = 1,
                )
            )
        }
    }

    sealed interface CampaignPreviewUiState {
        object Loading : CampaignPreviewUiState
        data class CampaignPreviewContent(
            val isLoading: Boolean = false,
            val productId: Long,
            val title: String,
            val tagLine: String,
            val campaignImageUrl: String,
            val budget: CampaignDetailItem,
            val audienceDetails: List<CampaignDetailItem>,
            val destinationUrl: CampaignDetailItem,
        ) : CampaignPreviewUiState

        data class CampaignDetailItem(
            val displayTitle: String,
            val displayValue: String,
            val maxLinesValue: Int? = null,
        )
    }
}
