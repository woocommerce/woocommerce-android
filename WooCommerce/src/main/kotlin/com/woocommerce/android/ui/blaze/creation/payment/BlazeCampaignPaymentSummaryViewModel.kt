package com.woocommerce.android.ui.blaze.creation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
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
    private val paymentMethodState = MutableStateFlow<PaymentMethodState>(PaymentMethodState.Loading)

    val viewState = combine(
        selectedPaymentMethodId,
        paymentMethodState
    ) { selectedPaymentMethodId, paymentMethodState ->
        ViewState(
            budget = navArgs.budget,
            paymentMethodState = paymentMethodState.let {
                when (it) {
                    is PaymentMethodState.Success -> it.copy(selectedPaymentMethodId = selectedPaymentMethodId)
                    else -> it
                }
            }
        )
    }.asLiveData()

    init {
        fetchPaymentMethodData()
    }

    fun onBackClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onPaymentMethodSelected(paymentMethodId: String) {
        selectedPaymentMethodId.value = paymentMethodId

        val paymentMethodState = paymentMethodState.value
        if (paymentMethodState is PaymentMethodState.Success &&
            !paymentMethodState.paymentMethodsData.savedPaymentMethods.any { it.id == paymentMethodId }
        ) {
            fetchPaymentMethodData()
        }
    }

    private fun fetchPaymentMethodData() {
        paymentMethodState.value = PaymentMethodState.Loading
        launch {
            blazeRepository.fetchPaymentMethods().fold(
                onSuccess = { paymentMethodsData ->
                    if (selectedPaymentMethodId.value == null) {
                        selectedPaymentMethodId.value = paymentMethodsData.savedPaymentMethods.firstOrNull()?.id
                    }

                    paymentMethodState.value = PaymentMethodState.Success(
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
                    paymentMethodState.value = PaymentMethodState.Error { fetchPaymentMethodData() }
                }
            )
        }
    }

    data class ViewState(
        val budget: BlazeRepository.Budget,
        val paymentMethodState: PaymentMethodState
    )

    sealed interface PaymentMethodState {
        data object Loading : PaymentMethodState
        data class Success(
            val paymentMethodsData: PaymentMethodsData,
            private val selectedPaymentMethodId: String? = null,
            val onClick: () -> Unit
        ) : PaymentMethodState {
            val selectedPaymentMethod = selectedPaymentMethodId?.let { id ->
                paymentMethodsData.savedPaymentMethods.find { it.id == id }
            } ?: paymentMethodsData.savedPaymentMethods.firstOrNull()
            val isPaymentMethodSelected = selectedPaymentMethod != null
        }

        data class Error(val onRetry: () -> Unit) : PaymentMethodState
    }

    data class NavigateToPaymentsListScreen(
        val paymentMethodsData: PaymentMethodsData,
        val selectedPaymentMethodId: String?
    ) : MultiLiveEvent.Event()
}
