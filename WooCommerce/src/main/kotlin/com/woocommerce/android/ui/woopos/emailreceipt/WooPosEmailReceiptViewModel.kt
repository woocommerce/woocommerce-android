package com.woocommerce.android.ui.woopos.emailreceipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptSendFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptSendSuccess
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptSendTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("UnusedPrivateProperty")
class WooPosEmailReceiptViewModel @Inject constructor(
    private val repository: WooPosEmailReceiptRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val orderId = savedState.get<Long>(EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY)!!

    private val _state = savedState.getStateFlow<WooPosEmailReceiptState>(
        scope = viewModelScope,
        initialValue = WooPosEmailReceiptState.Email(
            email = "",
            errorMessage = null,
            button = WooPosEmailReceiptState.Email.Button(
                text = resourceProvider.getString(R.string.woopos_email_receipt_send_button),
                status = WooPosEmailReceiptState.Email.Button.Status.DISABLED
            )
        ),
        key = "woo_pos_email_receipt_state",
    )

    val state: StateFlow<WooPosEmailReceiptState> = _state

    fun onUIEvent(event: WooPosEmailReceiptUIEvent) {
        when (event) {
            WooPosEmailReceiptUIEvent.SendEmailClicked -> handleSendEmailClicked()
            is WooPosEmailReceiptUIEvent.EmailChanged -> handleEmailChanged(event.email)
        }
    }

    private fun handleSendEmailClicked() {
        viewModelScope.launch {
            analyticsTracker.track(EmailReceiptSendTapped)
            val currentState = _state.value as WooPosEmailReceiptState.Email
            _state.value = currentState.copy(
                errorMessage = null,
                button = currentState.button.copy(
                    status = WooPosEmailReceiptState.Email.Button.Status.LOADING
                )
            )

            val result = repository.sendReceiptByEmail(orderId, currentState.email)

            _state.value = if (result.isSuccess) {
                analyticsTracker.track(EmailReceiptSendSuccess)
                WooPosEmailReceiptState.Sent
            } else {
                analyticsTracker.track(EmailReceiptSendFailed)
                val currentState = _state.value as? WooPosEmailReceiptState.Email ?: return@launch
                currentState.copy(
                    errorMessage = resourceProvider.getString(R.string.woopos_email_receipt_send_error),
                    button = currentState.button.copy(
                        status = WooPosEmailReceiptState.Email.Button.Status.ENABLED
                    )
                )
            }
        }
    }

    private fun handleEmailChanged(email: String) {
        val currentState = _state.value as WooPosEmailReceiptState.Email
        _state.value = if (repository.isEmailValid(email)) {
            currentState.copy(
                email = email,
                errorMessage = null,
                button = currentState.button.copy(
                    status = WooPosEmailReceiptState.Email.Button.Status.ENABLED
                )
            )
        } else {
            currentState.copy(
                email = email,
                button = currentState.button.copy(
                    status = WooPosEmailReceiptState.Email.Button.Status.DISABLED
                )
            )
        }
    }
}
