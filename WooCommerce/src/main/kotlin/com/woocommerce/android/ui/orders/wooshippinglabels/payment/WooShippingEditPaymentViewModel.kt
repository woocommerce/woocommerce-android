package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.ui.orders.wooshippinglabels.FetchAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.ObserveAccountSettings
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodOptions
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
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

    private val loadingState = MutableStateFlow(LoadingState.Idle)
    private val dialogState = MutableStateFlow<DialogState?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = combine(
        isAddPaymentMethodWebViewVisible,
        accountSettings
    ) { isAddPaymentMethodWebViewVisible, accountSettings ->
        Pair(isAddPaymentMethodWebViewVisible, accountSettings)
    }
        .transformLatest { (isAddPaymentMethodWebViewVisible, accountSettings) ->
            if (accountSettings == null) {
                emit(ViewState.Loading)
                return@transformLatest
            }

            if (isAddPaymentMethodWebViewVisible) {
                initAddPaymentMethodWebView(accountSettings)
            } else {
                initContentViewState(accountSettings)
            }
        }
        .onStart { emit(ViewState.Loading) }
        .asLiveData()

    private suspend fun FlowCollector<ViewState>.initAddPaymentMethodWebView(accountSettings: AccountSettingsModel) {
        emit(
            ViewState.AddPaymentMethodWebView(
                url = accountSettings.paymentMethodOptions.addPaymentMethodUrl,
                userAgent = userAgent,
                authenticator = webViewAuthenticator,
                onUrlLoaded = { url ->
                    if (url.contains(PAYMENT_METHOD_SUCCESS_URL)) {
                        onPaymentMethodAdded()
                    }
                },
                onDismiss = { isAddPaymentMethodWebViewVisible.value = false }
            )
        )
    }

    private suspend fun FlowCollector<ViewState>.initContentViewState(accountSettings: AccountSettingsModel) {
        combine(selectedPaymentMethod, loadingState, dialogState) { selectedPaymentMethod, loadingState, dialogState ->
            ViewState.Content(
                loadingState = loadingState,
                dialogState = dialogState,
                canManagePaymentMethods = accountSettings.canManagePayments,
                canEditSettings = accountSettings.canEditSettings,
                emailTheReceipt = accountSettings.paymentMethodOptions.emailReceipts,
                storeOwnerName = accountSettings.storeOwnerName,
                storeOwnerUsername = accountSettings.storeOwnerUsername,
                selectedPaymentMethodId = selectedPaymentMethod
                    ?: accountSettings.paymentMethodOptions.selectedPaymentId,
                currentPaymentOptions = accountSettings.paymentMethodOptions
            )
        }.let { emitAll(it) }
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
        launch {
            isAddPaymentMethodWebViewVisible.value = false
            loadingState.value = LoadingState.LoadingAddedPaymentMethod

            val existingPaymentMethods = accountSettings.value?.paymentMethodOptions
                ?.paymentMethods ?: return@launch

            fetchAccountSettings().fold(
                onSuccess = { accountSettings ->
                    if (accountSettings.paymentMethodOptions.paymentMethods.size == existingPaymentMethods.size + 1) {
                        val newPaymentMethod = accountSettings.paymentMethodOptions.paymentMethods.firstOrNull {
                            it !in existingPaymentMethods
                        }
                        selectedPaymentMethod.value = newPaymentMethod?.paymentMethodId
                        triggerEvent(ShowSnackbar(R.string.woo_shipping_payment_method_added))
                    }
                },
                onFailure = {
                    dialogState.value = DialogState(
                        message = R.string.woo_shipping_payment_fetching_cards_failed,
                        positiveButton = DialogState.DialogButton(
                            text = R.string.retry,
                            onClick = {
                                dialogState.value = null
                                onPaymentMethodAdded()
                            }
                        ),
                        negativeButton = DialogState.DialogButton(
                            text = R.string.cancel,
                            onClick = { dialogState.value = null }
                        )
                    )
                }
            )
            loadingState.value = LoadingState.Idle
        }
    }

    sealed interface ViewState {
        data object Loading : ViewState
        data class Content(
            val loadingState: LoadingState = LoadingState.Idle,
            val dialogState: DialogState? = null,
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

    enum class LoadingState {
        Idle, LoadingAddedPaymentMethod, Saving
    }
}
