package com.woocommerce.android.ui.blaze.creation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
    private val blazeRepository: BlazeRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs = BlazeCampaignPaymentSummaryFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val selectedPaymentMethodId = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "selectedPaymentMethodId"
    )
    private val paymentMethodsState = MutableStateFlow<PaymentMethodsState>(PaymentMethodsState.Loading)

    val viewState = combine(
        selectedPaymentMethodId,
        paymentMethodsState
    ) { selectedPaymentMethodId, paymentMethodState ->
        ViewState(
            budget = navArgs.budget,
            paymentMethodsState = paymentMethodState,
            selectedPaymentMethodId = selectedPaymentMethodId
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

    data class ViewState(
        val budget: BlazeRepository.Budget,
        val paymentMethodsState: PaymentMethodsState,
        private val selectedPaymentMethodId: String?
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

    data class NavigateToPaymentsListScreen(
        val paymentMethodsData: PaymentMethodsData,
        val selectedPaymentMethodId: String?
    ) : MultiLiveEvent.Event()
}
