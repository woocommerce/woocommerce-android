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
                productId = product?.remoteId ?: -1,
                title = "Get the latest white t-shirts",
                tagLine = "From 45.00 USD",
                campaignImageUrl = "https://rb.gy/gmjuwb",
                destinationUrl = "https://www.myer.com.au/p/white-t-shirt-797334760-797334760",
                budget = CampaignPreviewState.Budget(
                    totalBudget = "140",
                    duration = "7",
                    startDate = "2024-10-01",
                    displayBudgetDetails = "140 USD, 7 days from Jan 14"
                ),
                audience = CampaignPreviewState.Audience(
                    languages = listOf("English"),
                    locations = listOf("United States"),
                    devices = listOf("Android"),
                    interests = listOf("Fashion"),
                )
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
            val destinationUrl: String,
            val budget: Budget,
            val audience: Audience,
        ) : CampaignPreviewState

        data class Budget(
            val totalBudget: String,
            val duration: String,
            val startDate: String,
            val displayBudgetDetails: String
        )

        data class Audience(
            val languages: List<String>,
            val locations: List<String>,
            val devices: List<String>,
            val interests: List<String>,
        )
    }
}
