package com.woocommerce.android.ui.blaze.creation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignPaymentMethodsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val navArgs by savedStateHandle.navArgs<BlazeCampaignPaymentMethodsListFragmentArgs>()

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        if (navArgs.paymentMethodsData.savedPaymentMethods.isEmpty()) {
            addPaymentMethodWebView()
        } else {
            paymentMethodsListState()
        }
    )
    val viewState = _viewState.asLiveData()

    private fun paymentMethodsListState() = ViewState.PaymentMethodsList(
        paymentMethods = navArgs.paymentMethodsData.savedPaymentMethods,
        onPaymentMethodClicked = { /* TODO */ },
        onAddPaymentMethodClicked = {
            _viewState.value = addPaymentMethodWebView()
        }
    )

    private fun addPaymentMethodWebView() = ViewState.AddPaymentMethodWebView(
        urls = navArgs.paymentMethodsData.paymentMethodUrls,
        onUrlLoaded = { /* TODO */ }
    )

    sealed interface ViewState {
        data class PaymentMethodsList(
            val paymentMethods: List<BlazeRepository.PaymentMethod>,
            val onPaymentMethodClicked: (BlazeRepository.PaymentMethod) -> Unit,
            val onAddPaymentMethodClicked: () -> Unit
        ) : ViewState

        data class AddPaymentMethodWebView(
            val urls: BlazeRepository.PaymentMethodUrls,
            val onUrlLoaded: (String) -> Unit
        ) : ViewState
    }
}
