package com.woocommerce.android.ui.woopos.settings.details.security

data class WooPosPinSetupState(
    val stage: Stage = Stage.CHOOSE,
    val enteredDigits: Int = 0,
) {
    enum class Stage { CHOOSE, CONFIRM, MISMATCH, CHECKING, SUCCESS }
}

sealed interface WooPosPinSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosPinSetupUIEvent
    data object DeleteTapped : WooPosPinSetupUIEvent
    data object BackTapped : WooPosPinSetupUIEvent
}
