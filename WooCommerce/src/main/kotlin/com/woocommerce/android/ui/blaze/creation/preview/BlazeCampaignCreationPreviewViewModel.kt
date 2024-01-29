package com.woocommerce.android.ui.blaze.creation.preview

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.Budget
import com.woocommerce.android.ui.blaze.BlazeRepository.CampaignPreview
import com.woocommerce.android.ui.blaze.BlazeRepository.Device
import com.woocommerce.android.ui.blaze.BlazeRepository.Interest
import com.woocommerce.android.ui.blaze.BlazeRepository.Language
import com.woocommerce.android.ui.blaze.BlazeRepository.Location
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.AdDetailsUi.AdDetails
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.AdDetailsUi.Loading
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.DEVICE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LANGUAGE
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()
    private val campaign = blazeRepository.getCampaignPreviewDetails(navArgs.productId)

    private val adDetails = savedStateHandle.getStateFlow<AdDetailsUi>(viewModelScope, Loading)
    private val budget = savedStateHandle.getStateFlow(viewModelScope, getDefaultBudget())

    private val languages = blazeRepository.observeLanguages()
    private val devices = blazeRepository.observeDevices()
    private val interests = blazeRepository.observeInterests()
    private val selectedLanguages = savedStateHandle.getStateFlow<List<String>>(
        scope = viewModelScope,
        initialValue = emptyList(),
        key = "selectedLanguages"
    )
    private val selectedDevices = savedStateHandle.getStateFlow<List<String>>(
        scope = viewModelScope,
        initialValue = emptyList(),
        key = "selectedDevices"
    )

    val viewState = combine(
        adDetails,
        budget,
        languages,
        devices,
        selectedLanguages,
        selectedDevices
    ) { adDetails, budget, languages, devices, selectedLanguages, selectedDevices ->
        CampaignPreviewUiState(
            adDetails = adDetails,
            campaignDetails = campaign.toCampaignDetailsUi(
                budget,
                languages.filter { it.code in selectedLanguages },
                devices.filter { it.id in selectedDevices },
                emptyList(),
                emptyList()
            )
        )
    }.asLiveData()

    init {
        loadData()
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onEditAdClicked() {
        (adDetails.value as? AdDetails)?.let {
            triggerEvent(
                NavigateToEditAdScreen(
                    productId = navArgs.productId,
                    tagLine = it.tagLine,
                    description = it.description,
                    campaignImageUrl = it.campaignImageUrl
                )
            )
        }
    }

    fun onAdUpdated(tagline: String, description: String, campaignImageUrl: String?) {
        adDetails.update {
            AdDetails(
                productId = navArgs.productId,
                description = description,
                tagLine = tagline,
                campaignImageUrl = campaignImageUrl
            )
        }
    }

    fun onTargetSelectionUpdated(targetType: BlazeTargetType, selectedIds: List<String>) {
        launch {
            when (targetType) {
                LANGUAGE -> selectedLanguages.update { selectedIds }
                DEVICE -> selectedDevices.update { selectedIds }
                else -> Unit
            }
        }
    }

    private fun loadData() {
        launch {
            blazeRepository.fetchLanguages()
            blazeRepository.fetchDevices()
            blazeRepository.fetchInterests()

            blazeRepository.getAdSuggestions(navArgs.productId).let { suggestions ->
                adDetails.update {
                    AdDetails(
                        productId = navArgs.productId,
                        description = suggestions?.firstOrNull()?.description ?: "",
                        tagLine = suggestions?.firstOrNull()?.tagLine ?: "",
                        campaignImageUrl = campaign.campaignImageUrl
                    )
                }
            }
        }
    }

    private fun CampaignPreview.toCampaignDetailsUi(
        budget: Budget,
        languages: List<Language>,
        devices: List<Device>,
        interests: List<Interest>,
        locations: List<Location>
    ) = CampaignDetailsUi(
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
                onItemSelected = {
                    triggerEvent(NavigateToTargetSelectionScreen(LANGUAGE, languages.map { it.code }))
                },
            ),
            CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_devices),
                displayValue = devices.joinToString { it.name }
                    .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                onItemSelected = {
                    triggerEvent(NavigateToTargetSelectionScreen(DEVICE, devices.map { it.id }))
                },
            ),
            CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_location),
                displayValue = locations.joinToString { it.name }
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

    private fun Budget.toDisplayValue(): String {
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

    private fun getDefaultBudget() = Budget(
        totalBudget = BlazeRepository.DEFAULT_CAMPAIGN_TOTAL_BUDGET,
        spentBudget = 0f,
        currencyCode = BlazeRepository.BLAZE_DEFAULT_CURRENCY_CODE,
        durationInDays = BlazeRepository.DEFAULT_CAMPAIGN_DURATION,
        startDate = Date().apply { time += BlazeRepository.ONE_DAY_IN_MILLIS }, // By default start tomorrow
    )

    data class NavigateToEditAdScreen(
        val productId: Long,
        val tagLine: String,
        val description: String,
        val campaignImageUrl: String?
    ) : MultiLiveEvent.Event()

    data class CampaignPreviewUiState(
        val adDetails: AdDetailsUi,
        val campaignDetails: CampaignDetailsUi,
    )

    sealed interface AdDetailsUi : Parcelable {
        @Parcelize
        object Loading : AdDetailsUi

        @Parcelize
        data class AdDetails(
            val productId: Long,
            val description: String,
            val tagLine: String,
            val campaignImageUrl: String?,
        ) : AdDetailsUi
    }

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
    data class NavigateToTargetSelectionScreen(
        val targetType: BlazeTargetType,
        val selectedIds: List<String>
    ) : MultiLiveEvent.Event()
}
