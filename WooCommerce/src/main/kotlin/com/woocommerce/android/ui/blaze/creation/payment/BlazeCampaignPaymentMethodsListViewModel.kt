package com.woocommerce.android.ui.blaze.creation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_ADD_PAYMENT_METHOD_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CREATION_ADD_PAYMENT_METHOD_WEB_VIEW_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.BlazeRepository.PaymentMethod
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignPaymentMethodsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val userAgent: UserAgent,
    private val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val blazeRepository: BlazeRepository
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

    private fun paymentMethodsListState(
        paymentMethods: List<PaymentMethod> = navArgs.paymentMethodsData.savedPaymentMethods,
        selectedMethod: PaymentMethod? = navArgs.paymentMethodsData.savedPaymentMethods.firstOrNull {
            it.id == navArgs.selectedPaymentMethodId
        }
    ): ViewState = ViewState.PaymentMethodsList(
        paymentMethods = paymentMethods,
        selectedPaymentMethod = selectedMethod,
        accountEmail = accountRepository.getUserAccount()?.email ?: "",
        accountUsername = accountRepository.getUserAccount()?.userName ?: "",
        onPaymentMethodClicked = {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(it.id))
        },
        onAddPaymentMethodClicked = {
            _viewState.value = addPaymentMethodWebView()
            analyticsTrackerWrapper.track(stat = BLAZE_CREATION_ADD_PAYMENT_METHOD_WEB_VIEW_DISPLAYED)
        },
        onDismiss = { triggerEvent(MultiLiveEvent.Event.Exit) }
    )

    private fun addPaymentMethodWebView(): ViewState = ViewState.AddPaymentMethodWebView(
        formUrl = navArgs.paymentMethodsData.addPaymentMethodUrls.formUrl,
        userAgent = userAgent,
        wpComWebViewAuthenticator = wpComWebViewAuthenticator,
        onUrlLoaded = { url ->
            viewModelScope.launch {
                val urls = navArgs.paymentMethodsData.addPaymentMethodUrls
                if (url.contains(urls.successUrl)) {
                    blazeRepository.fetchPaymentMethods().fold(
                        onSuccess = { data ->
                            val newPayment = data.savedPaymentMethods.singleOrNull {
                                !navArgs.paymentMethodsData.savedPaymentMethods.contains(it)
                            }
                            if (newPayment != null) {
                                analyticsTrackerWrapper.track(stat = BLAZE_CREATION_ADD_PAYMENT_METHOD_SUCCESS)
                                triggerEvent(
                                    MultiLiveEvent.Event.ShowSnackbar(
                                        R.string.blaze_campaign_payment_added_successfully
                                    )
                                )
                                triggerEvent(MultiLiveEvent.Event.ExitWithResult(newPayment.id))
                            } else {
                                WooLog.e(WooLog.T.BLAZE, "Failed to find a new payment methods")
                                _viewState.value = paymentMethodsListState(data.savedPaymentMethods, null)
                            }
                        },
                        onFailure = {
                            WooLog.e(WooLog.T.BLAZE, "Failed to fetch payment methods after adding a new one")
                            _viewState.value = paymentMethodsListState()
                        }
                    )
                }
            }
        },
        onDismiss = { _viewState.value = paymentMethodsListState() }
    )

    sealed interface ViewState {
        val onDismiss: () -> Unit

        data class PaymentMethodsList(
            val paymentMethods: List<PaymentMethod>,
            val selectedPaymentMethod: PaymentMethod?,
            val accountEmail: String,
            val accountUsername: String,
            val onPaymentMethodClicked: (PaymentMethod) -> Unit,
            val onAddPaymentMethodClicked: () -> Unit,
            override val onDismiss: () -> Unit
        ) : ViewState

        data class AddPaymentMethodWebView(
            val formUrl: String,
            val userAgent: UserAgent,
            val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
            val onUrlLoaded: (String) -> Unit,
            override val onDismiss: () -> Unit
        ) : ViewState
    }
}
