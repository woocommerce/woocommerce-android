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
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.CAMPAIGN_MINIMUM_DAILY_SPEND
import com.woocommerce.android.ui.blaze.BlazeRepository.Companion.DEFAULT_CAMPAIGN_DURATION
import com.woocommerce.android.ui.blaze.Device
import com.woocommerce.android.ui.blaze.Interest
import com.woocommerce.android.ui.blaze.Language
import com.woocommerce.android.ui.blaze.Location
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.AdDetailsUi.AdDetails
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.AdDetailsUi.Loading
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.DEVICE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.INTEREST
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LANGUAGE
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
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
    private suspend fun getCampaign() = blazeRepository.getCampaignPreviewDetails(navArgs.productId)

    private val adDetails = savedStateHandle.getStateFlow<AdDetailsUi>(viewModelScope, Loading)
    private val budget = savedStateHandle.getStateFlow(viewModelScope, getDefaultBudget())

    private val languages = blazeRepository.observeLanguages()
    private val devices = blazeRepository.observeDevices()
    private val interests = blazeRepository.observeInterests()

    private val selectedLanguageCodes = savedStateHandle.getStateFlow<List<String>>(
        scope = viewModelScope,
        initialValue = emptyList(),
        key = "selectedLanguages"
    )

    private val selectedLanguages = combine(languages, selectedLanguageCodes) { languages, selectedCodes ->
        languages.filter { it.code in selectedCodes }
    }

    private val selectedDeviceIds = savedStateHandle.getStateFlow<List<String>>(
        scope = viewModelScope,
        initialValue = emptyList(),
        key = "selectedDevices"
    )

    private val selectedDevices = combine(devices, selectedDeviceIds) { devices, selectedIds ->
        devices.filter { it.id in selectedIds }
    }
    private val selectedInterestIds = savedStateHandle.getStateFlow<List<String>>(
        scope = viewModelScope,
        initialValue = emptyList(),
        key = "selectedInterests"
    )

    private val selectedInterests = combine(interests, selectedInterestIds) { interests, selectedIds ->
        interests.filter { it.id in selectedIds }
    }

    private val selectedLocations = savedStateHandle.getStateFlow<List<Location>>(
        scope = viewModelScope,
        initialValue = emptyList()
    )

    val viewState = combine(
        adDetails,
        budget,
        selectedLanguages,
        selectedDevices,
        selectedInterests,
        selectedLocations
    ) { ad, budget, selectedLanguages, selectedDevices, selectedInterests, selectedLocations ->
        CampaignPreviewUiState(
            adDetails = ad,
            campaignDetails = getCampaign().toCampaignDetailsUi(
                budget,
                selectedLanguages,
                selectedDevices,
                selectedInterests,
                selectedLocations
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

    fun onBudgetAndDurationUpdated(totalBudget: Float, durationInDays: Int, campaignStartDateMillis: Long) {
        budget.update {
            Budget(
                totalBudget = totalBudget,
                spentBudget = 0f,
                currencyCode = BlazeRepository.BLAZE_DEFAULT_CURRENCY_CODE,
                durationInDays = durationInDays,
                startDate = Date(campaignStartDateMillis)
            )
        }
    }

    fun onTargetSelectionUpdated(targetType: BlazeTargetType, selectedIds: List<String>) {
        launch {
            when (targetType) {
                LANGUAGE -> selectedLanguageCodes.update { selectedIds }
                DEVICE -> selectedDeviceIds.update { selectedIds }
                INTEREST -> selectedInterestIds.update { selectedIds }
                else -> Unit
            }
        }
    }

    fun onTargetLocationsUpdated(locations: List<Location>) {
        selectedLocations.update { locations }
    }

    fun onConfirmClicked() {
        triggerEvent(NavigateToPaymentSummary(budget.value))
    }

    private fun loadData() {
        launch {
            blazeRepository.fetchLanguages()
            blazeRepository.fetchDevices()
            blazeRepository.fetchInterests()

            blazeRepository.fetchAdSuggestions(navArgs.productId).getOrNull().let { suggestions ->
                adDetails.update {
                    AdDetails(
                        productId = navArgs.productId,
                        description = suggestions?.firstOrNull()?.description ?: "",
                        tagLine = suggestions?.firstOrNull()?.tagLine ?: "",
                        campaignImageUrl = getCampaign().campaignImageUrl
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
            onItemSelected = {
                triggerEvent(
                    NavigateToBudgetScreen(
                        totalBudget = budget.totalBudget,
                        durationInDays = budget.durationInDays,
                        campaignStartDateMillis = budget.startDate.time,
                        currencyCode = budget.currencyCode
                    )
                )
            },
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
                onItemSelected = {
                    triggerEvent(NavigateToTargetLocationSelectionScreen(locations))
                },
            ),
            CampaignDetailItemUi(
                displayTitle = resourceProvider.getString(string.blaze_campaign_preview_details_interests),
                displayValue = interests.joinToString { it.description }
                    .ifEmpty { resourceProvider.getString(string.blaze_campaign_preview_target_default_value) },
                onItemSelected = {
                    triggerEvent(NavigateToTargetSelectionScreen(INTEREST, interests.map { it.id }))
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
        totalBudget = DEFAULT_CAMPAIGN_DURATION * CAMPAIGN_MINIMUM_DAILY_SPEND,
        spentBudget = 0f,
        currencyCode = BlazeRepository.BLAZE_DEFAULT_CURRENCY_CODE,
        durationInDays = DEFAULT_CAMPAIGN_DURATION,
        startDate = Date().apply { time += BlazeRepository.ONE_DAY_IN_MILLIS }, // By default start tomorrow
    )

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

    data class NavigateToBudgetScreen(
        val totalBudget: Float,
        val durationInDays: Int,
        val campaignStartDateMillis: Long,
        val currencyCode: String
    ) : MultiLiveEvent.Event()data class NavigateToAdDestinationScreen(
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
