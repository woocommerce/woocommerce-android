package com.woocommerce.android.ui.blaze.creation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
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

    private val _viewState = MutableStateFlow(
        if (navArgs.paymentMethodsData.savedPaymentMethods.isEmpty()) {
            addPaymentMethodWebView()
        } else {
            paymentMethodsListState()
        }
    )
    val viewState = _viewState.asLiveData()

    private fun paymentMethodsListState(): ViewState = ViewState.PaymentMethodsList(
        paymentMethods = navArgs.paymentMethodsData.savedPaymentMethods,
        selectedPaymentMethod = navArgs.paymentMethodsData.savedPaymentMethods.firstOrNull {
            it.id == navArgs.selectedPaymentMethodId
        },
        onPaymentMethodClicked = { /* TODO */ },
        onAddPaymentMethodClicked = {
            _viewState.value = addPaymentMethodWebView()
        },
        onDismiss = { triggerEvent(MultiLiveEvent.Event.Exit) }
    )

    private fun addPaymentMethodWebView(): ViewState = ViewState.AddPaymentMethodWebView(
        urls = navArgs.paymentMethodsData.addPaymentMethodUrls,
        onUrlLoaded = { /* TODO */ },
        onDismiss = { _viewState.value = paymentMethodsListState() }
    )

    sealed interface ViewState {
        val onDismiss: () -> Unit

        data class PaymentMethodsList(
            val paymentMethods: List<BlazeRepository.PaymentMethod>,
            val selectedPaymentMethod: BlazeRepository.PaymentMethod?,
            val onPaymentMethodClicked: (BlazeRepository.PaymentMethod) -> Unit,
            val onAddPaymentMethodClicked: () -> Unit,
            override val onDismiss: () -> Unit
        ) : ViewState

        data class AddPaymentMethodWebView(
            val urls: BlazeRepository.PaymentMethodUrls,
            val onUrlLoaded: (String) -> Unit,
            override val onDismiss: () -> Unit
        ) : ViewState
    }
}
