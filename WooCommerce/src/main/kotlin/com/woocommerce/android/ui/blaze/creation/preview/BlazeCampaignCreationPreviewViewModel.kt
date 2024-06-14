package com.woocommerce.android.ui.blaze.creation.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_CONFIRM_DETAILS_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_EDIT_AD_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_FORM_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.AiSuggestionForAd
import com.woocommerce.android.ui.blaze.BlazeRepository.CampaignDetails
import com.woocommerce.android.ui.blaze.Location
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.DEVICE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.INTEREST
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LANGUAGE
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignCreationPreviewFragmentArgs by savedStateHandle.navArgs()
    private val campaignDetails = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        key = "campaignDetails",
        initialValue = null,
        clazz = CampaignDetails::class.java
    )
    private var aiSuggestions: List<AiSuggestionForAd> = emptyList()

    private val adDetailsState = savedStateHandle.getStateFlow(viewModelScope, AdDetailsUiState.LOADING)
    private val dialogState = MutableStateFlow<DialogState?>(null)

    val viewState = combine(
        campaignDetails.filterNotNull(),
        adDetailsState,
        dialogState
    ) { campaignDetails, adDetailsState, dialogState ->
        CampaignPreviewUiState(
            adDetails = when (adDetailsState) {
                AdDetailsUiState.LOADING -> AdDetailsUi.Loading
                AdDetailsUiState.LOADED -> AdDetailsUi.AdDetails(
                    productId = navArgs.productId,
                    description = campaignDetails.description,
                    tagLine = campaignDetails.tagLine,
                    campaignImageUrl = campaignDetails.campaignImage.uri,
                    isContentSuggestedByAi = isAdContentGeneratedByAi(campaignDetails)
                )
            },
            campaignDetails = campaignDetails.toCampaignDetailsUi(),
            dialogState = dialogState
        )
    }.asLiveData()

    init {
        loadData()
        analyticsTrackerWrapper.track(
            stat = BLAZE_CREATION_FORM_DISPLAYED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to navArgs.source.trackingName)
        )
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpTapped() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.BLAZE_CAMPAIGN_CREATION))
    }

    fun onEditAdClicked() {
        campaignDetails.value?.let {
            analyticsTrackerWrapper.track(stat = BLAZE_CREATION_EDIT_AD_TAPPED)
            triggerEvent(
                NavigateToEditAdScreen(
                    productId = navArgs.productId,
                    tagLine = it.tagLine,
                    description = it.description,
                    campaignImage = it.campaignImage,
                    aiSuggestions = aiSuggestions
                )
            )
        }
    }

    fun onAdUpdated(tagline: String, description: String, campaignImage: BlazeRepository.BlazeCampaignImage) {
        campaignDetails.update {
            it?.copy(
                tagLine = tagline,
                description = description,
                campaignImage = campaignImage
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

    fun onDestinationUpdated(destinationParameters: BlazeRepository.DestinationParameters) {
        campaignDetails.update { it?.copy(destinationParameters = destinationParameters) }
    }

    fun onConfirmClicked() {
        analyticsTrackerWrapper.track(
            stat = BLAZE_CREATION_CONFIRM_DETAILS_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_BLAZE_IS_AI_CONTENT to isAdContentGeneratedByAi(campaignDetails.value)
            )
        )
        campaignDetails.value?.let {
            val isImageMissing = it.campaignImage is BlazeRepository.BlazeCampaignImage.None
            val isContentMissing = it.tagLine.isEmpty() || it.description.isEmpty()
            if (isImageMissing || isContentMissing) {
                dialogState.value = DialogState(
                    message = if (isImageMissing) {
                        R.string.blaze_campaign_preview_missing_image_dialog_text
                    } else {
                        R.string.blaze_campaign_preview_missing_content_dialog_text
                    },
                    positiveButton = DialogState.DialogButton(
                        text = if (isImageMissing) {
                            R.string.blaze_campaign_preview_missing_image_dialog_positive_button
                        } else {
                            R.string.blaze_campaign_preview_missing_content_dialog_positive_button
                        },
                        onClick = {
                            dialogState.value = null
                            onEditAdClicked()
                        }
                    ),
                    negativeButton = DialogState.DialogButton(
                        text = R.string.cancel,
                        onClick = { dialogState.value = null }
                    )
                )
                return
            }

            triggerEvent(NavigateToPaymentSummary(it))
        }
    }

    private fun isAdContentGeneratedByAi(campaignDetails: CampaignDetails?): Boolean =
        aiSuggestions.any {
            it.tagLine == campaignDetails?.tagLine &&
                it.description == campaignDetails.description
        }

    private fun loadData() {
        launch {
            if (campaignDetails.value == null) {
                launch { campaignDetails.value = blazeRepository.generateDefaultCampaignDetails(navArgs.productId) }
            }

            blazeRepository.fetchLanguages()
            blazeRepository.fetchDevices()
            blazeRepository.fetchInterests()

            blazeRepository.fetchAdSuggestions(productId = navArgs.productId).getOrNull().let { suggestions ->
                aiSuggestions = suggestions.orEmpty()
                adDetailsState.value = AdDetailsUiState.LOADED
                campaignDetails.update {
                    it?.copy(
                        tagLine = suggestions?.firstOrNull()?.tagLine.orEmpty(),
                        description = suggestions?.firstOrNull()?.description.orEmpty(),
                    )
                }
            }
        }
    }

    private fun CampaignDetails.toCampaignDetailsUi() = CampaignDetailsUi(
        budget = getBudgetDetails(),
        targetDetails = listOf(
            getTargetLanguagesDetails(),
            getTargetDevicesDetails(),
            getTargetLocationsDetails(),
            getTargetInterestsDetails(),
        ),
        destinationUrl = getTargetDestinationDetails()
    )

    private fun CampaignDetails.getBudgetDetails() =
        CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_budget),
            displayValue = budget.toDisplayValue(),
            onItemSelected = {
                triggerEvent(NavigateToBudgetScreen(budget, targetingParameters))
            },
        )

    private fun CampaignDetails.getTargetDestinationDetails() =
        CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_destination_url),
            displayValue = destinationParameters.fullUrl,
            maxLinesValue = 1,
            onItemSelected = {
                triggerEvent(
                    NavigateToAdDestinationScreen(
                        productId = navArgs.productId,
                        destinationParameters = destinationParameters
                    )
                )
            }
        )

    private fun CampaignDetails.getTargetInterestsDetails() =
        CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_interests),
            displayValue = targetingParameters.interests.joinToString { it.description }
                .ifEmpty { resourceProvider.getString(R.string.blaze_campaign_preview_target_default_value) },
            onItemSelected = {
                triggerEvent(NavigateToTargetSelectionScreen(INTEREST, targetingParameters.interests.map { it.id }))
            },
        )

    private fun CampaignDetails.getTargetLocationsDetails() =
        CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_location),
            displayValue = targetingParameters.locations.joinToString { it.name }
                .ifEmpty { resourceProvider.getString(R.string.blaze_campaign_preview_target_default_value) },
            onItemSelected = {
                triggerEvent(NavigateToTargetLocationSelectionScreen(targetingParameters.locations))
            },
        )

    private fun CampaignDetails.getTargetDevicesDetails() =
        CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_devices),
            displayValue = targetingParameters.devices.joinToString { it.name }
                .ifEmpty { resourceProvider.getString(R.string.blaze_campaign_preview_target_default_value) },
            onItemSelected = {
                triggerEvent(NavigateToTargetSelectionScreen(DEVICE, targetingParameters.devices.map { it.id }))
            },
        )

    private fun CampaignDetails.getTargetLanguagesDetails() =
        CampaignDetailItemUi(
            displayTitle = resourceProvider.getString(R.string.blaze_campaign_preview_details_language),
            displayValue = targetingParameters.languages.joinToString { it.name }
                .ifEmpty { resourceProvider.getString(R.string.blaze_campaign_preview_target_default_value) },
            onItemSelected = {
                triggerEvent(
                    NavigateToTargetSelectionScreen(
                        targetType = LANGUAGE,
                        selectedIds = targetingParameters.languages.map { it.code }
                    )
                )
            },
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
        return "$totalBudgetWithCurrency, $duration"
    }

    data class CampaignPreviewUiState(
        val adDetails: AdDetailsUi,
        val campaignDetails: CampaignDetailsUi,
        val dialogState: DialogState? = null
    )

    enum class AdDetailsUiState {
        LOADING,
        LOADED
    }

    sealed interface AdDetailsUi {
        data object Loading : AdDetailsUi

        data class AdDetails(
            val productId: Long,
            val description: String,
            val tagLine: String,
            val campaignImageUrl: String?,
            val isContentSuggestedByAi: Boolean,
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
        val budget: BlazeRepository.Budget,
        val targetingParameters: BlazeRepository.TargetingParameters
    ) : MultiLiveEvent.Event()

    data class NavigateToAdDestinationScreen(
        val productId: Long,
        val destinationParameters: BlazeRepository.DestinationParameters
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
        val campaignImage: BlazeRepository.BlazeCampaignImage,
        val aiSuggestions: List<BlazeRepository.AiSuggestionForAd>
    ) : MultiLiveEvent.Event()

    data class NavigateToPaymentSummary(
        val campaignDetails: CampaignDetails
    ) : MultiLiveEvent.Event()
}
