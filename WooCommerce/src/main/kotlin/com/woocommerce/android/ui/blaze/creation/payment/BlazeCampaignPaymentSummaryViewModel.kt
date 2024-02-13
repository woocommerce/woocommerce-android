package com.woocommerce.android.ui.blaze.creation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.PaymentMethodsData
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignPaymentSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs = BlazeCampaignPaymentSummaryFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val paymentMethodState = MutableStateFlow<PaymentMethodState>(PaymentMethodState.Loading)

    val viewState = paymentMethodState.map {
        ViewState(
            budget = navArgs.budget,
            paymentMethodState = it
        )
    }.asLiveData()

    init {
        fetchPaymentMethodData()
    }

    fun onBackClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun fetchPaymentMethodData() {
        paymentMethodState.value = PaymentMethodState.Loading
        launch {
            paymentMethodState.value = blazeRepository.fetchPaymentMethods().fold(
                onSuccess = {
                    PaymentMethodState.Success(
                        it,
                        onClick = { /* TODO */ }
                    )
                },
                onFailure = { PaymentMethodState.Error { fetchPaymentMethodData() } }
            )
        }
    }

    fun onSubmitCampaign() {
        // TODO show loading and trigger campaign creation
        triggerEvent(NavigateToStartingScreenWithSuccessBottomSheet)
    }

    data class ViewState(
        val budget: BlazeRepository.Budget,
        val paymentMethodState: PaymentMethodState
    )

    sealed interface PaymentMethodState {
        data object Loading : PaymentMethodState
        data class Success(
            private val paymentMethodsData: PaymentMethodsData,
            val onClick: () -> Unit
        ) : PaymentMethodState {
            val selectedPaymentMethod = paymentMethodsData.savedPaymentMethods.firstOrNull()
        }

        data class Error(val onRetry: () -> Unit) : PaymentMethodState
    }

    object NavigateToStartingScreenWithSuccessBottomSheet : MultiLiveEvent.Event()
}
