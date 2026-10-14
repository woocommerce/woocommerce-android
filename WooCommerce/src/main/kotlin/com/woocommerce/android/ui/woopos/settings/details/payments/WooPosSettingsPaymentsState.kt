package com.woocommerce.android.ui.woopos.settings.details.payments

sealed class WooPosSettingsPaymentsState {
    data object Loading : WooPosSettingsPaymentsState()
    data class Content(val isPaymentSuccessSoundEnabled: Boolean) : WooPosSettingsPaymentsState()
}

sealed class WooPosSettingsPaymentsUIEvent {
    data class PaymentSuccessSoundToggled(val isEnabled: Boolean) : WooPosSettingsPaymentsUIEvent()
}
