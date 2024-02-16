package com.woocommerce.android.ui.blaze.creation.payment

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_PAYMENT_SUBMIT_CAMPAIGN_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.PaymentMethodsData
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignPaymentSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs = BlazeCampaignPaymentSummaryFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val selectedPaymentMethodId = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "selectedPaymentMethodId"
    )
    private val paymentMethodsState = MutableStateFlow<PaymentMethodsState>(PaymentMethodsState.Loading)
    private val campaignCreationState = MutableStateFlow<CampaignCreationState?>(null)

    val viewState = combine(
        selectedPaymentMethodId,
        paymentMethodsState,
        campaignCreationState
    ) { selectedPaymentMethodId, paymentMethodState, campaignCreationState ->
        ViewState(
            budget = navArgs.campaignDetails.budget,
            paymentMethodsState = paymentMethodState,
            selectedPaymentMethodId = selectedPaymentMethodId,
            campaignCreationState = campaignCreationState
        )
    }.asLiveData()

    init {
        fetchPaymentMethodData()
    }

    fun onBackClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpClicked() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.BLAZE_CAMPAIGN_CREATION))
    }

    fun onPaymentMethodSelected(paymentMethodId: String) {
        selectedPaymentMethodId.value = paymentMethodId

        val paymentMethodState = paymentMethodsState.value
        if (paymentMethodState is PaymentMethodsState.Success &&
            !paymentMethodState.paymentMethodsData.savedPaymentMethods.any { it.id == paymentMethodId }
        ) {
            fetchPaymentMethodData()
        }
    }

    private fun fetchPaymentMethodData() {
        paymentMethodsState.value = PaymentMethodsState.Loading
        launch {
            blazeRepository.fetchPaymentMethods().fold(
                onSuccess = { paymentMethodsData ->
                    if (selectedPaymentMethodId.value == null) {
                        selectedPaymentMethodId.value = paymentMethodsData.savedPaymentMethods.firstOrNull()?.id
                    }

                    paymentMethodsState.value = PaymentMethodsState.Success(
                        paymentMethodsData = paymentMethodsData,
                        onClick = {
                            triggerEvent(
                                NavigateToPaymentsListScreen(
                                    paymentMethodsData = paymentMethodsData,
                                    selectedPaymentMethodId = selectedPaymentMethodId.value
                                )
                            )
                        }
                    )
                },
                onFailure = {
                    paymentMethodsState.value = PaymentMethodsState.Error { fetchPaymentMethodData() }
                }
            )
        }
    }

    fun onSubmitCampaign() {
        if (campaignCreationState.value == CampaignCreationState.Loading) {
            return
        }

        launch {
            campaignCreationState.value = CampaignCreationState.Loading
            blazeRepository.createCampaign(
                campaignDetails = navArgs.campaignDetails,
                paymentMethodId = requireNotNull(selectedPaymentMethodId.value)
            ).fold(
                onSuccess = {
                    campaignCreationState.value = null
                    analyticsTrackerWrapper.track(stat = BLAZE_CREATION_PAYMENT_SUBMIT_CAMPAIGN_TAPPED)
        triggerEvent(NavigateToStartingScreenWithSuccessBottomSheet)},
                onFailure = {
                    val errorMessage = when (it) {
                        is BlazeRepository.CampaignCreationError.MediaUploadError ->
                            R.string.blaze_campaign_creation_error_media_upload
                        is BlazeRepository.CampaignCreationError.MediaFetchError ->
                            R.string.blaze_campaign_creation_error_media_fetch
                        else -> R.string.blaze_campaign_creation_error
                    }
                    campaignCreationState.value = CampaignCreationState.Failed(errorMessage)
                }
            )
        }
    }

    data class ViewState(
        val budget: BlazeRepository.Budget,
        val paymentMethodsState: PaymentMethodsState,
        private val selectedPaymentMethodId: String?,
        val campaignCreationState: CampaignCreationState? = null
    ) {
        private val paymentMethodsData
            get() = (paymentMethodsState as? PaymentMethodsState.Success)?.paymentMethodsData
        val selectedPaymentMethod
            get() = selectedPaymentMethodId?.let { id ->
                paymentMethodsData?.savedPaymentMethods?.find { it.id == id }
            } ?: paymentMethodsData?.savedPaymentMethods?.firstOrNull()
        val isPaymentMethodSelected
            get() = selectedPaymentMethod != null
    }

    sealed interface PaymentMethodsState {
        data object Loading : PaymentMethodsState
        data class Success(
            val paymentMethodsData: PaymentMethodsData,
            val onClick: () -> Unit
        ) : PaymentMethodsState

        data class Error(val onRetry: () -> Unit) : PaymentMethodsState
    }

    sealed interface CampaignCreationState {
        data object Loading : CampaignCreationState
        data class Failed(@StringRes val errorMessage: Int) : CampaignCreationState
    }

    data class NavigateToPaymentsListScreen(
        val paymentMethodsData: PaymentMethodsData,
        val selectedPaymentMethodId: String?
    ) : MultiLiveEvent.Event()

    object NavigateToStartingScreenWithSuccessBottomSheet : MultiLiveEvent.Event()
}
