package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash

sealed interface WooPosSettingsSecurityPINSetupState {
    val canEnterDigits: Boolean get() = false
    val canGoBack: Boolean get() = false
    val canClose: Boolean get() = true

    data class Choose(
        val enteredDigits: Int = 0,
        val error: Error? = null,
    ) : WooPosSettingsSecurityPINSetupState {
        override val canEnterDigits: Boolean get() = enteredDigits < WooPosPINHash.PIN_LENGTH
    }

    /** A full confirmation holds its dots while the PIN is compared and saved. */
    data class Confirm(val enteredDigits: Int = 0) : WooPosSettingsSecurityPINSetupState {
        override val canEnterDigits: Boolean get() = enteredDigits < WooPosPINHash.PIN_LENGTH
        override val canGoBack: Boolean get() = canEnterDigits
        override val canClose: Boolean get() = canEnterDigits
    }

    data object Saving : WooPosSettingsSecurityPINSetupState {
        override val canClose: Boolean get() = false
    }

    data object Saved : WooPosSettingsSecurityPINSetupState

    enum class Error { MISMATCH, SAVE_FAILED }
}

sealed interface WooPosSettingsSecurityPINSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosSettingsSecurityPINSetupUIEvent
    data object DeleteTapped : WooPosSettingsSecurityPINSetupUIEvent
    data object BackTapped : WooPosSettingsSecurityPINSetupUIEvent
}
