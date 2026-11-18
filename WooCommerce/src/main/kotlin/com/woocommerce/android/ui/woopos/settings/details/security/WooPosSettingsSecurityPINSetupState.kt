package com.woocommerce.android.ui.woopos.settings.details.security

data class WooPosSettingsSecurityPINSetupState(
    val stage: Stage = Stage.CHOOSE,
    val enteredDigits: Int = 0,
) {
    enum class Stage { CHOOSE, CONFIRM, MISMATCH, CHECKING, SUCCESS }
}

sealed interface WooPosSettingsSecurityPINSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosSettingsSecurityPINSetupUIEvent
    data object DeleteTapped : WooPosSettingsSecurityPINSetupUIEvent
    data object BackTapped : WooPosSettingsSecurityPINSetupUIEvent
}
