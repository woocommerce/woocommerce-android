package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.orders.wooshippinglabels.FetchAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.ObserveAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class WooShippingEditPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAccountSettings: ObserveAccountSettings,
    private val fetchAccountSettings: FetchAccountSettings,
    private val webViewAuthenticator: WebViewAuthenticator,
    private val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val PAYMENT_METHOD_SUCCESS_URL = "me/payment-methods"
    }

    private val selectedPaymentMethod = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = Int::class.java,
        key = "selectedPaymentMethod",
    )

    private val isAddPaymentMethodWebViewVisible = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "isAddPaymentMethodWebViewVisible"
    )

    private val accountSettings = observeAccountSettings()
        .stateIn(viewModelScope, initialValue = null, started = SharingStarted.Lazily)

    val viewState = combine(
        isAddPaymentMethodWebViewVisible,
        accountSettings,
        selectedPaymentMethod
    ) { isAddPaymentMethodWebViewVisible, accountSettings, selectedPaymentMethod ->
        if (accountSettings == null) return@combine ViewState.Loading

        if (isAddPaymentMethodWebViewVisible) {
            initAddPaymentMethodWebView(accountSettings)
        } else {
            initContentViewState(
                accountSettings = accountSettings,
                selectedPaymentMethod = selectedPaymentMethod
            )
        }
    }
        .onStart { emit(ViewState.Loading) }
        .asLiveData()

    private fun initAddPaymentMethodWebView(accountSettings: AccountSettingsModel): ViewState.AddPaymentMethodWebView {
        return ViewState.AddPaymentMethodWebView(
            url = accountSettings.paymentMethodOptions.addPaymentMethodUrl,
            userAgent = userAgent,
            authenticator = webViewAuthenticator,
            onUrlLoaded = { url ->
                if (url.contains(PAYMENT_METHOD_SUCCESS_URL)) {
                    onPaymentMethodAdded()
                }
            },
            onDismiss = { this.isAddPaymentMethodWebViewVisible.value = false }
        )
    }

    private fun initContentViewState(
        accountSettings: AccountSettingsModel,
        selectedPaymentMethod: Int?
    ): ViewState.Content {
        return ViewState.Content(
            canManagePaymentMethods = true, // TODO
            canEditSettings = true, // TODO
            emailTheReceipt = true, // TODO
            storeOwnerName = "John Doe", // TODO
            storeOwnerUsername = "johndoe", // TODO
            selectedPaymentMethodId = selectedPaymentMethod
                ?: accountSettings.paymentMethodOptions.selectedPaymentId,
            currentPaymentOptions = accountSettings.paymentMethodOptions
        )
    }

    fun onAddNewPaymentMethod() {
        isAddPaymentMethodWebViewVisible.value = true
    }

    fun onPaymentMethodSelected(paymentMethodId: Int?) {
        selectedPaymentMethod.value = paymentMethodId
    }

    fun onSaveClicked() {
        TODO()
    }

    private fun onPaymentMethodAdded() {
        isAddPaymentMethodWebViewVisible.value = false
        launch {
            val countOfCurrentPaymentMethods = accountSettings.value?.paymentMethodOptions
                ?.paymentMethods?.size ?: return@launch

            fetchAccountSettings().fold(
                onSuccess = {
                    if (it.paymentMethodOptions.paymentMethods.size == countOfCurrentPaymentMethods + 1) {
                        triggerEvent(ShowSnackbar(R.string.woo_shipping_payment_method_added))
                    }
                },
                onFailure = {
                    TODO()
                }
            )
        }
    }

    sealed interface ViewState {
        data object Loading : ViewState
        data class Content(
            val canManagePaymentMethods: Boolean,
            val canEditSettings: Boolean,
            val emailTheReceipt: Boolean,
            val selectedPaymentMethodId: Int?,
            val storeOwnerName: String,
            val storeOwnerUsername: String,
            val currentPaymentOptions: PaymentMethodOptions
        ) : ViewState {
            val paymentMethods get() = currentPaymentOptions.paymentMethods
        }

        data class AddPaymentMethodWebView(
            val url: String,
            val userAgent: UserAgent,
            val authenticator: WebViewAuthenticator,
            val onUrlLoaded: (String) -> Unit,
            val onDismiss: () -> Unit
        ) : ViewState
    }
}
