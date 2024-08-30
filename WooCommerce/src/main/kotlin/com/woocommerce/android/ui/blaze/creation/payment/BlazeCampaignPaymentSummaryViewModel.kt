package com.woocommerce.android.ui.blaze.creation.payment

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.PaymentMethodsData
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
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
    currencyFormatter: CurrencyFormatter,
    private val blazeRepository: BlazeRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val dashboardRepository: DashboardRepository,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    private val navArgs = BlazeCampaignPaymentSummaryFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val budgetFormatted = getBudgetDisplayValue(currencyFormatter)

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
            displayBudget = budgetFormatted,
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
        analyticsTrackerWrapper.track(stat = AnalyticsEvent.BLAZE_CREATION_PAYMENT_SUBMIT_CAMPAIGN_TAPPED)

        launch {
            campaignCreationState.value = CampaignCreationState.Loading
            blazeRepository.createCampaign(
                campaignDetails = navArgs.campaignDetails,
                paymentMethodId = requireNotNull(selectedPaymentMethodId.value)
            ).fold(
                onSuccess = {
                    campaignCreationState.value = null
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.BLAZE_CAMPAIGN_CREATION_SUCCESS,
                        properties = mapOf(
                            AnalyticsTracker.KEY_BLAZE_CAMPAIGN_TYPE to when {
                                navArgs.campaignDetails.budget.isEndlessCampaign ->
                                    AnalyticsTracker.VALUE_EVERGREEN_CAMPAIGN
                                else -> AnalyticsTracker.VALUE_START_END_CAMPAIGN
                            }
                        )
                    )
                    dashboardRepository.updateWidgetVisibility(type = DashboardWidget.Type.ONBOARDING, isVisible = true)
                    triggerEvent(NavigateToStartingScreenWithSuccessBottomSheet)
                },
                onFailure = {
                    val errorMessage = when (it) {
                        is BlazeRepository.CampaignCreationError.MediaUploadError ->
                            R.string.blaze_campaign_creation_error_media_upload

                        is BlazeRepository.CampaignCreationError.MediaFetchError ->
                            R.string.blaze_campaign_creation_error_media_fetch

                        else -> R.string.blaze_campaign_creation_error
                    }
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.BLAZE_CAMPAIGN_CREATION_FAILED,
                        properties = mapOf(
                            AnalyticsTracker.KEY_BLAZE_ERROR to it.message
                        )
                    )
                    campaignCreationState.value = CampaignCreationState.Failed(errorMessage)
                }
            )
        }
    }

    private fun getBudgetDisplayValue(currencyFormatter: CurrencyFormatter): String {
        val formattedBudget = currencyFormatter.formatCurrency(
            amount = navArgs.campaignDetails.budget.totalBudget.toBigDecimal(),
            currencyCode = navArgs.campaignDetails.budget.currencyCode
        )
        return when {
            navArgs.campaignDetails.budget.isEndlessCampaign -> resourceProvider.getString(
                R.string.blaze_campaign_budget_weekly_spending,
                formattedBudget
            )

            else -> formattedBudget
        }
    }

    data class ViewState(
        val displayBudget: String,
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
