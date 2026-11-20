package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash

sealed interface WooPosSettingsSecurityPINSetupState {
    val canGoBack: Boolean get() = false
    val canClose: Boolean get() = true

    sealed interface Entry : WooPosSettingsSecurityPINSetupState {
        val enteredDigits: Int
        val canEnterDigits: Boolean get() = enteredDigits < WooPosPINHash.PIN_LENGTH
    }

    data class Choose(
        override val enteredDigits: Int = 0,
        val error: Error? = null,
    ) : Entry

    data class Confirm(override val enteredDigits: Int = 0) : Entry {
        override val canGoBack: Boolean get() = canEnterDigits
        override val canClose: Boolean get() = canEnterDigits
    }

    data object Saving : WooPosSettingsSecurityPINSetupState {
        override val canClose: Boolean get() = false
    }

    data object Saved : WooPosSettingsSecurityPINSetupState

    data object AccessUnavailable : WooPosSettingsSecurityPINSetupState {
        override val canClose: Boolean get() = false
    }

    enum class Error { MISMATCH, SAVE_FAILED }
}

sealed interface WooPosSettingsSecurityPINSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosSettingsSecurityPINSetupUIEvent
    data object DeleteTapped : WooPosSettingsSecurityPINSetupUIEvent
    data object BackTapped : WooPosSettingsSecurityPINSetupUIEvent
}
