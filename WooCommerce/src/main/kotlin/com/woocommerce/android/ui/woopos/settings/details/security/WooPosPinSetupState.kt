package com.woocommerce.android.ui.woopos.settings.details.security

data class WooPosPinSetupState(
    val stage: Stage = Stage.CHOOSE,
    val enteredDigits: Int = 0,
    val mismatch: Boolean = false,
) {
    enum class Stage { CHOOSE, CONFIRM, CHECKING, SUCCESS }
    val canEnterPIN: Boolean
        get() = stage == Stage.CHOOSE || stage == Stage.CONFIRM
}

sealed interface WooPosPinSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosPinSetupUIEvent
    data object DeleteTapped : WooPosPinSetupUIEvent
    data object BackTapped : WooPosPinSetupUIEvent
}
