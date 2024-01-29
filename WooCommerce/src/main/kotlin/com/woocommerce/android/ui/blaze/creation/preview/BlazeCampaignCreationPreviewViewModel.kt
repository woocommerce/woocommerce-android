package com.woocommerce.android.ui.blaze.creation.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.CampaignPreview
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    blazeRepository: BlazeRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableLiveData(
        blazeRepository
            .getCampaignPreviewDetails(navArgs.productId)
            .toCampaignPreviewUiState(isLoading = true)
    )
    val viewState = _viewState

    init {
        launch {
            blazeRepository.getAdSuggestions(navArgs.productId)?.let { adSuggestions ->
                _viewState.value = _viewState.value?.copy(
                    isLoading = false,
                    adDetails = _viewState.value?.adDetails!!.copy(
                        description = adSuggestions.firstOrNull()?.description ?: "",
                        tagLine = adSuggestions.firstOrNull()?.tagLine ?: "",
                    )
                )
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun CampaignPreview.toCampaignPreviewUiState(isLoading: Boolean = false) =
        CampaignPreviewUiState(
            isLoading = isLoading,
            adDetails = AdDetailsUi(
                productId = productId,
                description = aiSuggestions.firstOrNull()?.description ?: "",
                tagLine = aiSuggestions.firstOrNull()?.tagLine ?: "",
                campaignImageUrl = campaignImageUrl ?: "",
            ),
            campaignDetails = toCampaignDetailsUi()
        )

    private fun CampaignPreview.toCampaignDetailsUi() =
        CampaignDetailsUi(
            budget = CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_budget),
                displayValue = budget.toDisplayValue(),
                onItemSelected = { triggerEvent(NavigateToBudgetScreen) },
            ),
            targetDetails = listOf(
                CampaignDetailItemUi(
                    displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_language),
                    displayValue = languages.joinToString { it.name }
                        .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                    onItemSelected = { /* TODO Add language selection */ },
                ),
                CampaignDetailItemUi(
                    displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_devices),
                    displayValue = locations.joinToString { it.name }
                        .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                    onItemSelected = { /* TODO Add devices selection */ },
                ),
                CampaignDetailItemUi(
                    displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_location),
                    displayValue = devices.joinToString { it.name }
                        .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                    onItemSelected = { /* TODO Add location selection */ },
                ),
                CampaignDetailItemUi(
                    displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_interests),
                    displayValue = interests.joinToString { it.description }
                        .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                    onItemSelected = { /* TODO Add interests selection */ },
                ),
            ),
            destinationUrl = CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_destination_url),
                displayValue = targetUrl,
                onItemSelected = { /* TODO Add destination url selection */ },
                maxLinesValue = 1,
            )
        )

    private fun BlazeRepository.Budget.toDisplayValue(): String {
        val totalBudgetWithCurrency = currencyFormatter.formatCurrency(
            totalBudget.toBigDecimal(),
            currencyCode
        )
        val duration = resourceProvider.getString(
            string.blaze_campaign_preview_days_duration,
            durationInDays,
            startDate.formatToMMMdd()
        )
        return "$totalBudgetWithCurrency,  $duration"
    }

    fun onEditAdClicked() {
        viewState.value?.let { campaignPreviewContent ->
            triggerEvent(
                NavigateToEditAdScreen(
                    productId = navArgs.productId,
                    tagLine = campaignPreviewContent.adDetails.tagLine,
                    description = campaignPreviewContent.adDetails.description,
                    campaignImageUrl = campaignPreviewContent.adDetails.campaignImageUrl
                )
            )
        }
    }

    fun onAdUpdated(tagline: String, description: String, campaignImageUrl: String?) {
        _viewState.value = viewState.value?.copy(
            adDetails = AdDetailsUi(
                productId = navArgs.productId,
                description = description,
                tagLine = tagline,
                campaignImageUrl = campaignImageUrl
            )
        )
    }

    data class NavigateToEditAdScreen(
        val productId: Long,
        val tagLine: String,
        val description: String,
        val campaignImageUrl: String?
    ) : MultiLiveEvent.Event()

    data class CampaignPreviewUiState(
        val isLoading: Boolean = false,
        val adDetails: AdDetailsUi,
        val campaignDetails: CampaignDetailsUi,
    )

    data class AdDetailsUi(
        val productId: Long,
        val description: String,
        val tagLine: String,
        val campaignImageUrl: String?,
    )

    data class CampaignDetailsUi(
        val budget: CampaignDetailItemUi,
        val targetDetails: List<CampaignDetailItemUi>,
        val destinationUrl: CampaignDetailItemUi,
    )

    data class CampaignDetailItemUi(
        val displayTitle: String,
        val displayValue: String,
        val onItemSelected: () -> Unit,
        val maxLinesValue: Int? = null,
    )

    object NavigateToBudgetScreen : MultiLiveEvent.Event()
}
