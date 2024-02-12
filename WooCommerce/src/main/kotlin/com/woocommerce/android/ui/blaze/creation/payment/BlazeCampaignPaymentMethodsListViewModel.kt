package com.woocommerce.android.ui.blaze.creation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.network.UserAgent
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignPaymentMethodsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val userAgent: UserAgent,
    private val wpComWebViewAuthenticator: WPComWebViewAuthenticator
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
        accountEmail = accountRepository.getUserAccount()?.email ?: "",
        accountUsername = accountRepository.getUserAccount()?.userName ?: "",
        onPaymentMethodClicked = {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(it.id))
        },
        onAddPaymentMethodClicked = {
            _viewState.value = addPaymentMethodWebView()
        },
        onDismiss = { triggerEvent(MultiLiveEvent.Event.Exit) }
    )

    private fun addPaymentMethodWebView(): ViewState = ViewState.AddPaymentMethodWebView(
        formUrl = navArgs.paymentMethodsData.addPaymentMethodUrls.formUrl,
        userAgent = userAgent,
        wpComWebViewAuthenticator = wpComWebViewAuthenticator,
        onUrlLoaded = { url ->
            if (url.startsWith(navArgs.paymentMethodsData.addPaymentMethodUrls.successUrl)) {
                runCatching {
                    URL(url).query?.split("&")
                        ?.firstOrNull { it.startsWith("payment_method_id=") }
                        ?.substringAfter("=")
                        .let { requireNotNull(it) }
                }.fold(
                    onSuccess = { paymentMethodId ->
                        triggerEvent(
                            MultiLiveEvent.Event.ShowSnackbar(
                                R.string.blaze_campaign_payment_added_successfully
                            )
                        )
                        MultiLiveEvent.Event.ExitWithResult(paymentMethodId)
                    },
                    onFailure = {
                        WooLog.e(WooLog.T.BLAZE, "Failed to extract payment method id from URL: $url", it)
                        _viewState.value = paymentMethodsListState()
                    }
                )
            }
        },
        onDismiss = { _viewState.value = paymentMethodsListState() }
    )

    sealed interface ViewState {
        val onDismiss: () -> Unit

        data class PaymentMethodsList(
            val paymentMethods: List<BlazeRepository.PaymentMethod>,
            val selectedPaymentMethod: BlazeRepository.PaymentMethod?,
            val accountEmail: String,
            val accountUsername: String,
            val onPaymentMethodClicked: (BlazeRepository.PaymentMethod) -> Unit,
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
