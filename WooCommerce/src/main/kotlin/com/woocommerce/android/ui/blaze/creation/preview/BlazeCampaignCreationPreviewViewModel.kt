package ui.blaze.creation.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.CampaignPreview
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewFragmentArgs
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignPreviewUiState.CampaignDetailItem
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    blazeRepository: BlazeRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableLiveData<CampaignPreviewUiState>(CampaignPreviewUiState.Loading)
    val viewState = _viewState

    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()

    init {
        launch {
            _viewState.value = blazeRepository
                .getCampaignPreviewDetails(navArgs.productId)
                .toCampaignPreviewUiState()
        }
    }

    private fun CampaignPreview.toCampaignPreviewUiState() =
        CampaignPreviewUiState.CampaignPreviewContent(
            productId = productId,
            title = aiSuggestions.firstOrNull()?.title ?: "",
            tagLine = aiSuggestions.firstOrNull()?.tagLine ?: "",
            budget = CampaignDetailItem(
                displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_budget),
                displayValue = budget.toDisplayValue(),
            ),
            targetDetails = listOf(
                CampaignDetailItem(
                    displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_language),
                    displayValue = languages.joinToString { it.name },
                ),
                CampaignDetailItem(
                    displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_devices),
                    displayValue = locations.joinToString { it.name },
                ),
                CampaignDetailItem(
                    displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_location),
                    displayValue = devices.joinToString { it.name },
                ),
                CampaignDetailItem(
                    displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_interests),
                    displayValue = interests.joinToString { it.description },
                ),
            ),
            destinationUrl = CampaignDetailItem(
                displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_destination_url),
                displayValue = targetUrl,
                maxLinesValue = 1,
            ),
            campaignImageUrl = campaignImageUrl ?: "",
        )

    private fun BlazeRepository.Budget.toDisplayValue(): String {
        val totalBudgetWithCurrency = currencyFormatter.formatCurrency(
            totalBudget.toBigDecimal(),
            currencyCode
        )
        val duration = resourceProvider.getString(
            R.string.blaze_campaign_preview_days_duration,
            durationInDays,
            startDate.formatToMMMdd()
        )
        return "$totalBudgetWithCurrency,  $duration"
    }

    sealed interface CampaignPreviewUiState {
        object Loading : CampaignPreviewUiState
        data class CampaignPreviewContent(
            val productId: Long,
            val title: String,
            val tagLine: String,
            val campaignImageUrl: String,
            val budget: CampaignDetailItem,
            val targetDetails: List<CampaignDetailItem>,
            val destinationUrl: CampaignDetailItem,
        ) : CampaignPreviewUiState

        data class CampaignDetailItem(
            val displayTitle: String,
            val displayValue: String,
            val maxLinesValue: Int? = null,
        )
    }
}
