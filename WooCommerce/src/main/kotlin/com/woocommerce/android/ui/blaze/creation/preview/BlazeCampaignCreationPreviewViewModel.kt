package com.woocommerce.android.ui.blaze.creation.preview

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.Location
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.DEVICE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.INTEREST
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LANGUAGE
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()
    private val campaignDetails = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        key = "campaignDetails",
        initialValue = null,
        clazz = BlazeRepository.CampaignDetails::class.java
    )

    private val adDetailsState = savedStateHandle.getStateFlow(viewModelScope, AdDetailsUiState.LOADING)

    val viewState = combine(campaignDetails.filterNotNull(), adDetailsState) { campaignDetails, adDetailsState ->
        CampaignPreviewUiState(
            adDetails = when (adDetailsState) {
                AdDetailsUiState.LOADING -> AdDetailsUi.Loading
                AdDetailsUiState.LOADED -> AdDetailsUi.AdDetails(
                    productId = navArgs.productId,
                    description = campaignDetails.description,
                    tagLine = campaignDetails.tagLine,
                    campaignImageUrl = campaignDetails.campaignImageUrl
                )
            },
            campaignDetails = campaignDetails.toCampaignDetailsUi()
        )
    }.asLiveData()

    init {
        loadData()
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onEditAdClicked() {
        campaignDetails.value?.let {
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
        campaignDetails.update {
            it?.copy(
                tagLine = tagline,
                description = description,
                campaignImageUrl = campaignImageUrl
            )
        }
    }

    fun onBudgetAndDurationUpdated(updatedBudget: BlazeRepository.Budget) {
        campaignDetails.update { it?.copy(budget = updatedBudget) }
    }

    fun onTargetSelectionUpdated(targetType: BlazeTargetType, selectedIds: List<String>) {
        launch {
            when (targetType) {
                LANGUAGE -> blazeRepository.observeLanguages().first().let { languages ->
                    val selectedLanguages = languages.filter { selectedIds.contains(it.code) }
                    campaignDetails.update {
                        it?.copy(targetingParameters = it.targetingParameters.copy(languages = selectedLanguages))
                    }
                }
                DEVICE -> blazeRepository.observeDevices().first().let { devices ->
                    val selectedDevices = devices.filter { selectedIds.contains(it.id) }
                    campaignDetails.update {
                        it?.copy(targetingParameters = it.targetingParameters.copy(devices = selectedDevices))
                    }
                }
                INTEREST -> blazeRepository.observeInterests().first().let { interests ->
                    val selectedInterests = interests.filter { selectedIds.contains(it.id) }
                    campaignDetails.update {
                        it?.copy(targetingParameters = it.targetingParameters.copy(interests = selectedInterests))
                    }
                }
                else -> Unit
            }
        }
    }

    fun onTargetLocationsUpdated(locations: List<Location>) {
        campaignDetails.update {
            it?.copy(targetingParameters = it.targetingParameters.copy(locations = locations))
        }
    }

    fun onConfirmClicked() {
        campaignDetails.value?.let {
            triggerEvent(NavigateToPaymentSummary(it.budget))
        }
    }

    private fun loadData() {
        launch {
            if (campaignDetails.value == null) {
                launch { campaignDetails.value = blazeRepository.generateDefaultCampaignDetails(navArgs.productId) }
            }

            blazeRepository.fetchLanguages()
            blazeRepository.fetchDevices()
            blazeRepository.fetchInterests()

            blazeRepository.fetchAdSuggestions(navArgs.productId).getOrNull().let { suggestions ->
                adDetailsState.value = AdDetailsUiState.LOADED
                campaignDetails.update {
                    it?.copy(
                        tagLine = suggestions?.firstOrNull()?.tagLine.orEmpty(),
                        description = suggestions?.firstOrNull()?.description.orEmpty()
                    )
                }
            }
        }
    }

    private fun BlazeRepository.CampaignDetails.toCampaignDetailsUi() = CampaignDetailsUi(
        budget = CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_budget),
            displayValue = budget.toDisplayValue(),
            onItemSelected = {
                triggerEvent(NavigateToBudgetScreen(budget))
            },
        ),
        targetDetails = listOf(
            CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_language),
                displayValue = targetingParameters.languages.joinToString { it.name }
                    .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                onItemSelected = {
                    triggerEvent(NavigateToTargetSelectionScreen(
                        targetType = LANGUAGE,
                        selectedIds = targetingParameters.languages.map { it.code }))
                },
            ),
            CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_devices),
                displayValue = targetingParameters.devices.joinToString { it.name }
                    .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                onItemSelected = {
                    triggerEvent(NavigateToTargetSelectionScreen(DEVICE, targetingParameters.devices.map { it.id }))
                },
            ),
            CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_location),
                displayValue = targetingParameters.locations.joinToString { it.name }
                    .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                onItemSelected = {
                    triggerEvent(NavigateToTargetLocationSelectionScreen(targetingParameters.locations))
                },
            ),
            CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_interests),
                displayValue = targetingParameters.interests.joinToString { it.description }
                    .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                onItemSelected = {
                    triggerEvent(NavigateToTargetSelectionScreen(INTEREST, targetingParameters.interests.map { it.id }))
                },
            ),
        ),
        destinationUrl = CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_destination_url),
            displayValue = targetUrl,
            maxLinesValue = 1,
            onItemSelected = {
                triggerEvent(NavigateToAdDestinationScreen(targetUrl, navArgs.productId))
            }
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

    data class CampaignPreviewUiState(
        val adDetails: AdDetailsUi,
        val campaignDetails: CampaignDetailsUi,
    )

    enum class AdDetailsUiState {
        LOADING,
        LOADED
    }

    sealed interface AdDetailsUi : Parcelable {
        @Parcelize
        data object Loading : AdDetailsUi

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

    data class NavigateToBudgetScreen(
        val budget: BlazeRepository.Budget
    ) : MultiLiveEvent.Event()

    data class NavigateToAdDestinationScreen(
        val targetUrl: String,
        val productId: Long
    ) : MultiLiveEvent.Event()

    data class NavigateToTargetSelectionScreen(
        val targetType: BlazeTargetType,
        val selectedIds: List<String>
    ) : MultiLiveEvent.Event()

    data class NavigateToTargetLocationSelectionScreen(
        val locations: List<Location>
    ) : MultiLiveEvent.Event()

    data class NavigateToEditAdScreen(
        val productId: Long,
        val tagLine: String,
        val description: String,
        val campaignImageUrl: String?
    ) : MultiLiveEvent.Event()

    // TODO we need to pass more details to use in the campaign creation
    data class NavigateToPaymentSummary(
        val budget: BlazeRepository.Budget
    ) : MultiLiveEvent.Event()
}
